package com.destywen.mydroid.ui.screen.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.destywen.mydroid.data.local.CommentEntity
import com.destywen.mydroid.data.local.JournalDao
import com.destywen.mydroid.data.local.JournalEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
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
    val comments: List<Comment>,
    val timestamp: Long
)

data class JournalScreenState(
    val journals: List<Journal>,
    val tags: List<String>
)

class JournalViewModel(private val dao: JournalDao) : ViewModel() {
    private val journalsFlow = dao.getAllJournals()
    private val commentsFlow = dao.getAllComments()

    private val commentsByJournalId = commentsFlow.map { comments -> comments.groupBy { it.journalId } }

    val state = combine(journalsFlow, commentsByJournalId) { journals, commentMap ->
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
            tags = buildSet<String> {
                journals.forEach { j ->
                    j.tag.split(",").forEach { tag ->
                        if (tag.isNotBlank()) {
                            add(tag)
                        }
                    }
                }
            }.toList()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), JournalScreenState(emptyList(), emptyList()))

    fun addJournal(content: String, tags: List<String>) = viewModelScope.launch {
        val tagString = tags.asSequence()
            .flatMap { it.split(",") }
            .map { it.trim() }.filter { it.isNotEmpty() }
            .distinct()
            .joinToString(",")
        dao.upsertJournal(JournalEntity(content = content, tag = tagString))
    }

    fun updateJournal(id: Int, content: String, tags: List<String>) = viewModelScope.launch {
        // keep time
        val tagString = tags.asSequence()
            .flatMap { it.split(",") }
            .map { it.trim() }.filter { it.isNotEmpty() }
            .distinct()
            .joinToString(",")
        val createdTime = state.value.journals.first { it.id == id }.timestamp
        dao.upsertJournal(JournalEntity(id = id, content = content, tag = tagString, time = createdTime))
    }

    fun addComment(journalId: Int, content: String) = viewModelScope.launch {
        dao.insertComment(CommentEntity(journalId = journalId, content = content))
    }

    fun deleteJournal(id: Int) = viewModelScope.launch { dao.deleteJournal(id) }

    companion object {
        fun Factory(dao: JournalDao) = viewModelFactory {
            initializer {
                JournalViewModel(dao)
            }
        }
    }
}