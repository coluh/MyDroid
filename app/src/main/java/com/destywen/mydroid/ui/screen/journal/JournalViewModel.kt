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
    val tag: String,
    val comments: List<Comment>,
    val timestamp: Long
)

class JournalViewModel(private val dao: JournalDao) : ViewModel() {
    private val journals = dao.getAllJournals()
    private val comments = dao.getAllComments()

    val items = combine(journals, comments) { journals, comments ->
        journals.map { j ->
            Journal(j.id, j.content, j.tag, comments.filter { it.journalId == j.id }.map { c ->
                Comment(
                    id = c.id, name = c.name, content = c.content, timestamp = c.time
                )
            }, j.time
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addJournal(content: String, tag: String) = viewModelScope.launch {
        dao.upsertJournal(JournalEntity(content = content, tag = tag))
    }

    fun updateJournal(id: Int, content: String, tag: String) = viewModelScope.launch {
        // keep time
        val createdTime = items.value.first { it.id == id }.timestamp
        dao.upsertJournal(JournalEntity(id = id, content = content, tag = tag, time = createdTime))
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