package com.destywen.mydroid.ui.screen.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.destywen.mydroid.data.local.CommentEntity
import com.destywen.mydroid.data.local.JournalDao
import com.destywen.mydroid.data.local.JournalEntity
import com.destywen.mydroid.data.local.JournalSettings
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
    val comments: List<Comment>,
    val timestamp: Long
)

data class JournalScreenState(
    val journals: List<Journal> = emptyList(),
    val tags: List<String> = emptyList(),
    val hideTags: List<String> = emptyList()
)

class JournalViewModel(private val dao: JournalDao, private val journalSettings: JournalSettings) : ViewModel() {
    private val journalsFlow = dao.getAllJournals()
    private val commentsFlow = dao.getAllComments()
    private val hideTagsFlow = journalSettings.hideTags

    private val commentsByJournalId = commentsFlow.map { comments -> comments.groupBy { it.journalId } }

    val state = combine(journalsFlow, commentsByJournalId, hideTagsFlow) { journals, commentMap, hideTags ->
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
            tags = buildSet {
                journals.forEach { j ->
                    j.tag.split(",").forEach { tag ->
                        if (tag.isNotBlank()) {
                            add(tag)
                        }
                    }
                }
            }.toList(),
            hideTags = hideTags?.split(",") ?: emptyList()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), JournalScreenState())

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

    fun hideTag(tag: String) = viewModelScope.launch {
        val hidedTags = journalSettings.hideTags.first()?.split(",") ?: emptyList()
        val newList = (hidedTags + listOf(tag)).distinct().joinToString(",")
        journalSettings.updateHideTags(newList)
    }

    fun showTag(tag: String) = viewModelScope.launch {
        val hidedTags = journalSettings.hideTags.first()?.split(",") ?: emptyList()
        val newList = hidedTags.filter { it != tag }.joinToString(",")
        journalSettings.updateHideTags(newList)
    }

    companion object {
        fun Factory(dao: JournalDao, settings: JournalSettings) = viewModelFactory {
            initializer {
                JournalViewModel(dao, settings)
            }
        }
    }
}