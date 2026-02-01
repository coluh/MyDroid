package com.destywen.mydroid.ui.screen.chat

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.destywen.mydroid.data.local.AgentSettings
import com.destywen.mydroid.data.local.ChatAgent
import com.destywen.mydroid.data.remote.AiChatService
import com.destywen.mydroid.data.remote.Message
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

data class ChatScreenState(
    val messages: List<ChatMessage> = emptyList(),
    val allAgents: List<ChatAgent> = emptyList(),
    val selectedAgent: ChatAgent? = null,
    val isResponding: Boolean = false,
    val error: String? = null
)

class ChatViewModel(private val service: AiChatService, private val agentSettings: AgentSettings) : ViewModel() {
    private val TAG: String = "colo"
    private val _agentsFlow = agentSettings.agentsFlow
    private val _selectedIdFlow = agentSettings.selectedAgentIdFlow

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val _isResponding = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val state: StateFlow<ChatScreenState> = combine(
        _messages, _agentsFlow, _selectedIdFlow, _isResponding, _error
    ) { messages, agents, selectedId, isResponding, error ->
        val activeAgent = agents.find { it.id == selectedId } ?: agents.firstOrNull()
        ChatScreenState(
            messages = messages,
            allAgents = agents,
            selectedAgent = activeAgent,
            isResponding = isResponding,
            error = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = ChatScreenState())

    private val chatHistory = mutableListOf<Message>()

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

        // add user message
        _messages.update { it + ChatMessage(content, true) }
        _isResponding.update { true }
        chatHistory.add(Message("user", content))

        // build context
        val apiMessage = mutableListOf<Message>()
        if (agent.systemPrompt.isNotBlank()) {
            apiMessage.add(Message(role = "system", content = agent.systemPrompt))
        }
        apiMessage.addAll(chatHistory)

        viewModelScope.launch {
            var fullResponse = ""
            _messages.update { it + ChatMessage("", false) }

            service.streamChat(apiMessage, agent)
                .catch { e ->
                    _error.update { e.message }
                    Log.d(TAG, "sendMessage: ${e.message}")
                    _isResponding.update { false }
                }
                .collect { token ->
                    Log.d("colo", "sendMessage: receive token length=${token.length}")
                    fullResponse += token
                    _messages.update { it.dropLast(1) + ChatMessage(fullResponse, false) }
                }

            chatHistory.add(Message("assistant", fullResponse))
            _isResponding.update { false }
        }
    }

    fun selectAgent(id: String) {
        viewModelScope.launch {
            agentSettings.selectAgent(id)
        }
    }

    fun saveAgent(agent: ChatAgent) {
        viewModelScope.launch {
            val newList = state.value.allAgents.filter { it.id != agent.id } + agent
            agentSettings.saveAgents(newList)
        }
    }

    fun clearSnackbar() {
        _error.update { null }
    }

    companion object {
        fun Factory(service: AiChatService, settings: AgentSettings) = viewModelFactory {
            initializer {
                ChatViewModel(service, settings)
            }
        }
    }
}