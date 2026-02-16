package com.destywen.mydroid.ui.screen.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.destywen.mydroid.data.local.AppSettings
import com.destywen.mydroid.data.local.ChatAgent
import com.destywen.mydroid.data.local.ChatDao
import com.destywen.mydroid.data.local.ChatMessageEntity
import com.destywen.mydroid.data.remote.AiChatService
import com.destywen.mydroid.data.remote.Message
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val id: Long = 0
)

data class ChatScreenState(
    val messages: List<ChatMessage> = emptyList(),
    val allAgents: List<ChatAgent> = emptyList(),
    val selectedAgent: ChatAgent? = null,
    val isResponding: Boolean = false,
    val error: String? = null,
    val userInput: String? = null
)

class ChatViewModel(
    private val service: AiChatService,
    private val chatDao: ChatDao,
    private val settings: AppSettings
) : ViewModel() {
    private val _agentsFlow = settings.agentsFlow
    private val _selectedId = settings.selectedAgentIdFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _dbMessages = _selectedId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else chatDao.getMessagesByAgent(id)
    }

    private val _generatingMessage = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val _isResponding = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _userInput = MutableStateFlow<String?>(null)

    val state: StateFlow<ChatScreenState> = combine(
        combine(_dbMessages, _generatingMessage, _agentsFlow) { db, gen, agents -> Triple(db, gen, agents) },
        combine(_selectedId, _isResponding, _error) { id, resp, err -> Triple(id, resp, err) },
        _userInput
    ) { (db, gen, agents), (id, resp, err), input ->
        val activeAgent = agents.find { it.id == id } ?: agents.firstOrNull()
        ChatScreenState(
            messages = db.map { ChatMessage( it.content, it.role == "user", it.id) } + gen,
            allAgents = agents,
            selectedAgent = activeAgent,
            isResponding = resp,
            error = err,
            userInput = input
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ChatScreenState())

    fun sendMessage(content: String) {
        if (content.isBlank()) {
            _error.update { "用户消息为空" }
            return
        }
        val agent = if (state.value.selectedAgent != null) {
            state.value.selectedAgent!!
        } else {
            _error.update { "没有模型配置" }
            return
        }

        viewModelScope.launch {

            _generatingMessage.update { listOf(ChatMessage(content, true)) }
            _isResponding.update { true }
            val history = buildList {
                if (agent.systemPrompt.isNotBlank()) {
                    add(Message("system", agent.systemPrompt))
                }
                addAll(_dbMessages.first().takeLast(20).map {
                    Message(it.role, it.content)
                })
                add(Message("user", content))
            }

            var fullResponse = ""

            service.streamChat(history, agent)
                .catch { e ->
                    _userInput.update { content }
                    _generatingMessage.update { emptyList() }
                    _isResponding.update { false }
                    _error.update { e.message }
                }
                .collect { token ->
                    fullResponse += token
                    _generatingMessage.update { listOf(ChatMessage(content, true), ChatMessage(fullResponse, false)) }
                }

            if (!_isResponding.value) {
                return@launch
            }

            chatDao.insertMessage(ChatMessageEntity(agentId = agent.id, role = "user", content = content))
            chatDao.insertMessage(ChatMessageEntity(agentId = agent.id, role = "assistant", content = fullResponse))
            _generatingMessage.update { emptyList() }
            _isResponding.update { false }
        }
    }

    fun deleteHistory(agentId: String) {
        viewModelScope.launch {
            chatDao.clearHistory(agentId)
        }
    }

    fun deleteMessageById(messageId: Long) {
        viewModelScope.launch {
            chatDao.deleteMessageById(messageId)
        }
    }

    fun selectAgent(id: String) {
        viewModelScope.launch {
            settings.selectAgent(id)
        }
    }

    fun deleteAgent(id: String) {
        viewModelScope.launch {
            settings.updateAgents(state.value.allAgents.filter { it.id != id })
        }
    }

    fun saveAgent(agent: ChatAgent) {
        viewModelScope.launch {
            val newList = state.value.allAgents.filter { it.id != agent.id } + agent
            settings.updateAgents(newList)
        }
    }

    fun clearSnackbar() {
        _error.update { null }
    }

    companion object {
        fun Factory(service: AiChatService, dao: ChatDao, settings: AppSettings) = viewModelFactory {
            initializer {
                ChatViewModel(service, dao, settings)
            }
        }
    }
}