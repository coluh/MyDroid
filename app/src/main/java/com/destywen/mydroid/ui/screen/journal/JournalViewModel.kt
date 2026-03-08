package com.destywen.mydroid.ui.screen.journal

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.destywen.mydroid.AppContainer
import com.destywen.mydroid.data.local.AgentEntity
import com.destywen.mydroid.data.local.AppSettings
import com.destywen.mydroid.data.local.ChatDao
import com.destywen.mydroid.data.local.CommentEntity
import com.destywen.mydroid.data.local.JournalDao
import com.destywen.mydroid.data.local.JournalEntity
import com.destywen.mydroid.data.local.Role
import com.destywen.mydroid.data.remote.AiChatService
import com.destywen.mydroid.data.remote.Message
import com.destywen.mydroid.domain.AppLogger
import com.destywen.mydroid.domain.FileManager
import com.destywen.mydroid.util.toDateTime
import com.destywen.mydroid.util.toDateTimeString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val allAgents: List<AgentEntity> = emptyList(),
    val replyAgent: AgentEntity? = null,
    val visionModalName: String? = null,
    val status: String? = null,
)

class JournalViewModel(
    private val journalDao: JournalDao,
    chatDao: ChatDao,
    private val manager: FileManager,
    private val service: AiChatService,
    private val settings: AppSettings
) : ViewModel() {
    private val _journalsFlow = journalDao.getAllJournals()
    private val _commentsFlow = journalDao.getAllComments()
    private val _agentsFlow = chatDao.getAgents()
    private val _status = MutableStateFlow<String?>(null)

    private val hideTags = settings.hideTags.map { it?.split(",") ?: emptyList() }
    private val settingsFlow =
        combine(hideTags, settings.journalAgentId, settings.vlModel) { a, b, c -> Triple(a, b, c) }
    private val commentsByJournalId = _commentsFlow.map { comments -> comments.groupBy { it.journalId } }
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
                        agents.find { it.id.toString() == c.name }?.name ?: "LLM-${c.name.take(6)}"
                    }
                    Comment(c.id, name, c.role, c.content, c.time)
                },
                journal.time
            )
        }
    }

    val state = combine(
        journals,
        _agentsFlow,
        settingsFlow,
        _status
    ) { journals, agents, (hideTags, replyAgentId, vlModel), status ->
        JournalScreenState(
            journals = journals.filter { j -> !j.tags.any { it in hideTags } },
            tags = journals.flatMap { j ->
                j.tags.filter { it.isNotBlank() }
            }.distinct(),
            hideTags = hideTags,
            allAgents = agents,
            replyAgent = agents.find { it.id.toString() == replyAgentId },
            visionModalName = vlModel,
            status = status,
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
        // save image if need
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
            // TODO: ui not synced yet, use comments from database
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
        val hidedTags = settings.hideTags.first()?.split(",") ?: emptyList()
        val newList = (hidedTags + listOf(tag)).distinct().joinToString(",")
        settings.updateHideTags(newList)
    }

    fun showTag(tag: String) = viewModelScope.launch {
        val hidedTags = settings.hideTags.first()?.split(",") ?: emptyList()
        val newList = hidedTags.filter { it != tag }.joinToString(",")
        settings.updateHideTags(newList)
    }

    fun selectReplyAgent(id: Long) = viewModelScope.launch {
        settings.updateJournalAgentId(id)
    }

    fun updateVisionModel(name: String) = viewModelScope.launch {
        name.takeIf { it.isNotBlank() }?.let {
            settings.updateVlModel(it)
        }
    }

    fun generateReply(id: Int, enableVision: Boolean = false) = viewModelScope.launch {
        if (state.value.replyAgent == null) {
            AppLogger.i("JournalViewModel.generateReply", "agent not set")
            _status.update { "回复模型为空" }
            return@launch
        }
        _status.update { "正在构建上下文..." }

        val all = journals.first().filter { j ->
            !j.tags.any { it in hideTags.first() }
        }
        val target = all.first { it.id == id }
        val messagesReversed = mutableListOf<String>()

        val agentNames = state.value.allAgents.map { it.name }

        val month = target.timestamp.toDateTime().monthValue
        for (j in all.subList(all.indexOf(target) + 1, all.size)) {
            if (j.timestamp.toDateTime().monthValue != month) {
                break
            }

            messagesReversed.add(buildString {
                appendLine("[" + j.timestamp.toDateTimeString() + "]")
                appendLine(j.content)
                // filter by display name
                j.comments.filter { it.name !in agentNames }.forEach { appendLine(it.content) }
            })
        }

        val messages = messagesReversed.take(40).reversed().joinToString("\n") // 要那么多上下文干嘛
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
        service.chat(history, state.value.replyAgent!!, image)
            .onSuccess {
                addAiComment(id, state.value.replyAgent!!.id.toString(), it)
                _status.update { null }
            }.onFailure { e ->
                _status.update { "响应失败：${e.message ?: "unknown"}" }
            }
    }

    private fun mergeComments(comments: List<Comment>, userPrefix: String): List<Message> {
        if (comments.isEmpty()) return listOf(Message(Role.USER, userPrefix))

        val result = mutableListOf<Message>()
        var currentRole = Role.USER
        var currentContent = StringBuilder(userPrefix)

        for (comment in comments) {
            if (comment.role == currentRole) {
                currentContent.append("\n").append(comment.content)
            } else {
                result.add(Message(currentRole, currentContent.toString()))
                currentRole = comment.role
                currentContent = StringBuilder(comment.content)
            }
        }

        result.add(Message(currentRole, currentContent.toString()))
        if (result.last().role == Role.ASSISTANT) {
            result.removeAt(result.lastIndex)
        }
        return result
    }

    companion object {
        fun Factory(container: AppContainer) = viewModelFactory {
            initializer {
                JournalViewModel(
                    container.journalDao,
                    container.chatDao,
                    container.fileManager,
                    container.chatService,
                    container.settings
                )
            }
        }
    }
}