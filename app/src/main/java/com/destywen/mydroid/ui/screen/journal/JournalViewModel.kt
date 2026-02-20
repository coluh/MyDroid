package com.destywen.mydroid.ui.screen.journal

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.destywen.mydroid.AppContainer
import com.destywen.mydroid.domain.AppLogger
import com.destywen.mydroid.data.local.AppSettings
import com.destywen.mydroid.data.local.ChatAgent
import com.destywen.mydroid.data.local.CommentEntity
import com.destywen.mydroid.data.local.JournalDao
import com.destywen.mydroid.data.local.JournalEntity
import com.destywen.mydroid.data.remote.AiChatService
import com.destywen.mydroid.data.remote.Message
import com.destywen.mydroid.domain.FileManager
import com.destywen.mydroid.util.timestampToLocalDateTime
import com.destywen.mydroid.util.timestampToLocalDateTimeString
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class Comment(
    val id: Int,
    val name: String,
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
    val allAgents: List<ChatAgent> = emptyList(),
    val replyAgent: ChatAgent? = null,
)

class JournalViewModel(
    private val dao: JournalDao,
    private val manager: FileManager,
    private val service: AiChatService,
    private val settings: AppSettings
) : ViewModel() {
    private val journalsFlow = dao.getAllJournals()
    private val commentsFlow = dao.getAllComments()
    private val hideTagsFlow = settings.hideTagsFlow
    private val agentsFlow = settings.agentsFlow
    private val replyAgentId = settings.journalAgentIdFlow

    private val commentsByJournalId = commentsFlow.map { comments -> comments.groupBy { it.journalId } }

    val state = combine(
        journalsFlow,
        commentsByJournalId,
        hideTagsFlow,
        agentsFlow,
        replyAgentId
    ) { journals, commentMap, hideTags, allAgents, replyAgentId ->
        JournalScreenState(
            journals = journals.map { j ->
                Journal(
                    id = j.id,
                    content = j.content,
                    tags = if (j.tag.isNotBlank()) {
                        j.tag.split(",")
                    } else {
                        emptyList()
                    },
                    image = j.image,
                    comments = commentMap[j.id].orEmpty().map { c ->
                        Comment(
                            id = c.id,
                            name = c.name,
                            content = c.content,
                            timestamp = c.time
                        )
                    },
                    timestamp = j.time
                )
            },
            tags = buildSet {
                journals.forEach { j ->
                    j.tag.split(",").forEach { tag ->
                        if (tag.isNotBlank()) {
                            add(tag)
                        }
                    }
                }
            }.toList(),
            hideTags = hideTags?.split(",") ?: emptyList(),
            allAgents = allAgents,
            replyAgent = allAgents.find { it.id == replyAgentId }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), JournalScreenState())

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
        dao.upsertJournal(JournalEntity(content = content, tag = tagString, image = imageName))
    }

    fun updateJournal(id: Int, content: String, tags: List<String>) = viewModelScope.launch {
        val origin = journalsFlow.first().first { it.id == id }

        val tagString = tags.asSequence()
            .flatMap { it.split(",") }
            .map { it.trim() }.filter { it.isNotEmpty() }
            .distinct()
            .joinToString(",")
        dao.upsertJournal(
            origin.copy(
                content = content,
                tag = tagString,
            )
        )
    }

    fun addComment(journalId: Int, name: String, content: String) = viewModelScope.launch {
        dao.insertComment(CommentEntity(journalId = journalId, name = name, content = content))
    }

    fun deleteJournal(id: Int) = viewModelScope.launch {
        val origin = journalsFlow.first().first { it.id == id }
        origin.image?.let { manager.deleteImage(origin.image) }
        dao.deleteJournal(id)
    }

    fun hideTag(tag: String) = viewModelScope.launch {
        val hidedTags = settings.hideTagsFlow.first()?.split(",") ?: emptyList()
        val newList = (hidedTags + listOf(tag)).distinct().joinToString(",")
        settings.updateHideTags(newList)
    }

    fun showTag(tag: String) = viewModelScope.launch {
        val hidedTags = settings.hideTagsFlow.first()?.split(",") ?: emptyList()
        val newList = hidedTags.filter { it != tag }.joinToString(",")
        settings.updateHideTags(newList)
    }

    fun selectReplyAgent(id: String) = viewModelScope.launch {
        settings.updateJournalAgentId(id)
    }

    fun generateReply(id: Int) = viewModelScope.launch {
        if (state.value.replyAgent == null) {
            AppLogger.i("JournalViewModel.generateReply", "agent not set")
            return@launch
        }

        val all = state.value.journals.filter { j ->
            !j.tags.any { it in state.value.hideTags }
        }
        val target = all.first { it.id == id }
        val messagesReversed = mutableListOf<String>()

        fun timedContent(j: Journal): String = buildString {
            appendLine("[" + timestampToLocalDateTimeString(j.timestamp) + "]")
            appendLine(j.content)
            j.comments.filter { it.name == "Destywen" }.forEach { appendLine(it.content) }
        }

        val month = timestampToLocalDateTime(target.timestamp).monthValue
        for (j in all.subList(all.indexOf(target) + 1, all.size)) {
            if (timestampToLocalDateTime(j.timestamp).monthValue != month) {
                break
            }

            messagesReversed.add(timedContent(j))
        }

        val messages = messagesReversed.reversed().joinToString("\n")
        val user = buildString {
            appendLine("## 历史随笔：")
            appendLine("\n" + messages)
            appendLine("## 最新随笔")
            appendLine("\n" + timedContent(target))
        }

        val history = listOf(Message("user", user))

        try {
            val result = service.chat(history, state.value.replyAgent!!)
            addComment(id, state.value.replyAgent!!.name, result)
        } catch (_: Exception) {
        }
    }

    companion object {
        fun Factory(container: AppContainer) = viewModelFactory {
            initializer {
                JournalViewModel(container.journalDao, container.fileManager, container.chatService, container.settings)
            }
        }
    }
}