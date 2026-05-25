package com.destywen.mydroid.ui.screen.journal

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.destywen.mydroid.MyApplication
import com.destywen.mydroid.data.local.CommentEntity
import com.destywen.mydroid.data.local.JournalEntity
import com.destywen.mydroid.data.local.Keys
import com.destywen.mydroid.data.local.Role
import com.destywen.mydroid.data.remote.ApiConfig
import com.destywen.mydroid.data.remote.ApiMessage
import com.destywen.mydroid.domain.AppLogger
import com.destywen.mydroid.util.toDateTime
import com.destywen.mydroid.util.toDateTimeString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class Comment(
    val id: Int,
    val name: String,
    val role: String,
    val content: String,
    val timestamp: Long
)

data class Journal(
    val id: Int,
    val content: String,
    val tags: List<String>,
    val image: String?,
    val comments: List<Comment>,
    val timestamp: Long
)

data class JournalScreenState(
    val journals: List<Journal> = emptyList(),
    val tags: List<String> = emptyList(),
    val hideTags: List<String> = emptyList(),
    val replyPrompt: String? = null,
    val username: String? = null,
    val status: String? = null,
    val importing: Int? = null,
    val importingTotal: Int? = null,
)

class JournalViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val app = application as MyApplication
    private val journalDao = app.database.journalDao()
    private val chatDao = app.database.chatDao()
    private val manager = app.fileManager
    private val settings = app.settings
    private val service = app.apiService

    private val _journalsFlow = journalDao.getAllJournals()
    private val _commentsFlow = journalDao.getAllComments()
    private val _agentsFlow = chatDao.getAllLlmConfigs()
    private val _status = MutableStateFlow<String?>(null)
    private val _importing = MutableStateFlow<Int?>(null)
    private val _importingTotal = MutableStateFlow<Int?>(null)

    private val commentsByJournalId = _commentsFlow.map { comments -> comments.groupBy { it.journalId } }
    private val settingsFlow = settings.config.map {
        val hideTags = it.hideTags?.split(",") ?: emptyList()
        Triple(hideTags, it.journalPrompt, it.username)
    }
    private val importStatus = combine(_importing, _importingTotal) { a, b -> Pair(a, b) }

    private val journals = combine(_journalsFlow, commentsByJournalId, _agentsFlow) { journals, commentMap, agents ->
        journals.map { journal ->
            Journal(
                id = journal.id,
                content = journal.content,
                tags = journal.tag.takeIf { it.isNotBlank() }?.split(",") ?: emptyList(),
                image = journal.image,
                comments = commentMap[journal.id].orEmpty().map { c ->
                    val name = if (c.role == Role.USER) {
                        c.name
                    } else {
                        agents.find { it.id.toString() == c.name }?.name ?: c.name.take(6)
                    }
                    Comment(c.id, name, c.role, c.content, c.time)
                },
                journal.time
            )
        }
    }

    val state = combine(
        journals,
        settingsFlow,
        _status,
        importStatus,
    ) { journals, (hideTags, replyPrompt, username), status, (importing, total) ->
        JournalScreenState(
            journals = journals.filter { j -> !j.tags.any { it in hideTags } },
            tags = journals.flatMap { j ->
                j.tags.filter { it.isNotBlank() }
            }.distinct(),
            hideTags = hideTags,
            replyPrompt = replyPrompt,
            username = username,
            status = status,
            importing = importing,
            importingTotal = total,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), JournalScreenState())

    fun updateStatus(s: String? = null) = viewModelScope.launch {
        _status.update { s }
    }

    fun addJournal(content: String, tags: List<String>, uri: Uri?) = viewModelScope.launch {
        val tagString = tags.asSequence()
            .flatMap { it.split(",") }
            .map { it.trim() }.filter { it.isNotEmpty() }
            .distinct()
            .joinToString(",")
        // save image if needed
        val imageName = uri?.let {
            manager.saveImage(uri)
        }
        journalDao.upsertJournal(JournalEntity(content = content, tag = tagString, image = imageName))
        imageName?.let { generateReply(journalDao.getAllJournals().first()[0].id, true) }
    }

    fun updateJournal(id: Int, content: String, tags: List<String>) = viewModelScope.launch {
        val origin = _journalsFlow.first().first { it.id == id }

        val tagString = tags.asSequence()
            .flatMap { it.split(",") }
            .map { it.trim() }.filter { it.isNotEmpty() }
            .distinct()
            .joinToString(",")
        journalDao.upsertJournal(
            origin.copy(
                content = content,
                tag = tagString,
            )
        )
    }

    fun updateJournalPrompt(prompt: String) = viewModelScope.launch {
        settings.update { it[Keys.JOURNAL_PROMPT] = prompt }
    }

    fun addAiComment(journalId: Int, name: String, content: String) = viewModelScope.launch {
        journalDao.insertComment(
            CommentEntity(
                journalId = journalId,
                name = name,
                role = "assistant",
                content = content
            )
        )
    }

    fun addUserComment(journalId: Int, name: String, content: String) = viewModelScope.launch {
        journalDao.insertComment(CommentEntity(journalId = journalId, name = name, role = "user", content = content))
        if (content.contains("LLM", ignoreCase = true)) {
            generateReply(journalId)
        }
    }

    fun deleteComment(commentId: Int) = viewModelScope.launch {
        journalDao.deleteComment(commentId)
    }

    fun deleteJournal(id: Int) = viewModelScope.launch {
        val origin = _journalsFlow.first().first { it.id == id }
        origin.image?.let { manager.deleteImage(origin.image) }
        journalDao.deleteCommentsOfJournal(id)
        journalDao.deleteJournal(id)
    }

    fun hideTag(tag: String) = viewModelScope.launch {
        val hidedTags = settings.config.first().hideTags?.split(",") ?: emptyList()
        val newList = (hidedTags + listOf(tag)).distinct().joinToString(",")
        settings.update { it[Keys.HIDE_TAGS] = newList }
    }

    fun showTag(tag: String) = viewModelScope.launch {
        val hidedTags = settings.config.first().hideTags?.split(",") ?: emptyList()
        val newList = hidedTags.filter { it != tag }.joinToString(",")
        settings.update { it[Keys.HIDE_TAGS] = newList }
    }

    fun generateReply(id: Int, enableVision: Boolean = false) = viewModelScope.launch {
        if (state.value.replyPrompt == null) {
            _status.update { "回复提示词为空" }
            return@launch
        }
        _status.update { "正在构建上下文..." }

        val all = journals.first().filter { j ->
            val hideTags = settings.config.first().hideTags?.split(",") ?: emptyList()
            !j.tags.any { it in hideTags }
        }
        val target = all.first { it.id == id }
        val messagesReversed = mutableListOf<String>()

        val month = target.timestamp.toDateTime().monthValue
        for (j in all.subList(all.indexOf(target) + 1, all.size)) {
            if (j.timestamp.toDateTime().monthValue != month) {
                break
            }

            messagesReversed.add(buildString {
                appendLine("[" + j.timestamp.toDateTimeString() + "]")
                appendLine(j.content)
                // filter by display name
                j.comments.filter { it.role == "user" }.forEach { appendLine(it.content) }
            })
        }

        val messages = messagesReversed.take(50).reversed().joinToString("\n") // 要那么多上下文干嘛
        val user = buildString {
            appendLine("## 历史随笔：")
            appendLine("\n" + messages)
            appendLine("## 最新随笔")
            appendLine("\n")
            appendLine("[" + target.timestamp.toDateTimeString() + "]")
            appendLine(target.content)
        }

        val history = mergeComments(target.comments, user)

        _status.update { "调用请求已发送" }
        val image = target.image?.let { manager.getImage(it) }.takeIf { enableVision }
        val config = ApiConfig(systemPrompt = state.value.replyPrompt!!)
        service.callLlm(history, config, image)
            .onSuccess {
                addAiComment(id, "LLM", it) // TODO: agent name
                _status.update { null }
            }.onFailure { e ->
                _status.update { "响应失败：${e.message ?: "unknown"}" }
            }
    }

    private fun mergeComments(comments: List<Comment>, userPrefix: String): List<ApiMessage> {
        if (comments.isEmpty()) return listOf(ApiMessage(Role.USER, userPrefix))

        val result = mutableListOf<ApiMessage>()
        var currentRole = Role.USER
        var currentContent = StringBuilder(userPrefix)

        for (comment in comments) {
            if (comment.role == currentRole) {
                currentContent.append("\n").append(comment.content)
            } else {
                result.add(ApiMessage(currentRole, currentContent.toString()))
                currentRole = comment.role
                currentContent = StringBuilder(comment.content)
            }
        }

        result.add(ApiMessage(currentRole, currentContent.toString()))
        while (result.last().role == Role.ASSISTANT) {
            result.removeAt(result.lastIndex)
        }
        return result
    }

    fun loadOldJournals(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        _importing.update { 0 }

        val oldJournals = mutableListOf<Journal>()

        val tableJson = manager.readFile(uri)
        if (tableJson == null) {
            _status.update { "fail to open file" }
            _importing.update { null }
            return@launch
        }

        @Serializable
        data class OldJournal(
            @SerialName("_id")
            val id: Long,
            val image: String,
            val text: String,
            val timestamp: Long,
        )

        @Serializable
        data class OldMessage(val role: String, val content: String)

        val items = Json.decodeFromString<List<OldJournal>>(tableJson)
        _importingTotal.update { items.size }
        val username = settings.config.first().username ?: "Destywen"
        items.forEach { item ->
            var content = item.text
            var comments = emptyList<Comment>()
            if (item.text.startsWith("[")) {
                val messages = Json.decodeFromString<List<OldMessage>>(item.text)
                content = messages.first().content
                comments = messages.drop(1).map {
                    val role = if (it.role == "user" || it.role == "用户") Role.USER else Role.ASSISTANT
                    val name = if (role == Role.USER) username else "LLM"
                    Comment(0, name, role, it.content, 0)
                }
            }
            oldJournals.add(
                Journal(
                    image = item.image.takeIf { it.isNotBlank() },
                    timestamp = item.timestamp,
                    content = content,
                    comments = comments,
                    id = 0,
                    tags = emptyList(),
                )
            )
            _importing.update { oldJournals.size }
        }

        // add to database
        oldJournals.forEachIndexed { idx, journal ->
            val id = journalDao.upsertJournal(
                JournalEntity(
                    content = journal.content,
                    image = journal.image,
                    time = journal.timestamp
                )
            )
            journal.comments.forEach { comment ->
                journalDao.insertComment(
                    CommentEntity(
                        journalId = id.toInt(),
                        name = comment.name,
                        role = comment.role,
                        content = comment.content,
                        time = journal.timestamp
                    )
                )
            }
            _importing.update { idx + 1 }
        }

        _importing.update { null }
        AppLogger.i("loadOldJournals", "load ${oldJournals.size} journals")
    }

    companion object {
        fun Factory(app: Application) = viewModelFactory {
            initializer {
                JournalViewModel(app)
            }
        }
    }
}