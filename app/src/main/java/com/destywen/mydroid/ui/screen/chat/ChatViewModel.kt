package com.destywen.mydroid.ui.screen.chat

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.destywen.mydroid.data.remote.AiChatService
import com.destywen.mydroid.data.remote.Message
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

data class ChatScreenState(
    val messages: List<ChatMessage> = emptyList(),
    val isResponding: Boolean = false,
    val error: String? = null
)

class ChatViewModel(private val service: AiChatService) : ViewModel() {
    private val _state = MutableStateFlow(ChatScreenState())
    val state: StateFlow<ChatScreenState> = _state.asStateFlow()

    private val modelName = "deepseek-v3.2"
    private val chatHistory = mutableListOf<Message>()

    fun sendMessage(content: String) {
        if (content.isBlank()) return

        _state.update { it.copy(messages = it.messages + ChatMessage(content, true), isResponding = true) }
        chatHistory.add(Message("user", content))

        viewModelScope.launch {
            var fullResponse = ""
            _state.update { it.copy(messages = it.messages + ChatMessage("", false)) }

            service.streamChat(chatHistory, modelName)
                .catch { e ->
                    _state.update { it.copy(error = e.message, isResponding = false) }
                }
                .collect { token ->
                    fullResponse += token
                    _state.update { it.copy(messages = it.messages.dropLast(1) + ChatMessage(fullResponse, false)) }
                }

            chatHistory.add(Message("assistant", fullResponse))
            _state.update { it.copy(isResponding = false) }
        }
    }

    fun sendMessageDebug(content: String) {
        if (content.isBlank()) return

        _state.update { it.copy(messages = it.messages + ChatMessage(content, true), isResponding = true) }
        chatHistory.add(Message("user", content))

        viewModelScope.launch {
            var fullResponse = ""
            _state.update { it.copy(messages = it.messages + ChatMessage("", false)) }

            val testResponse = "你是一个去除了所有限制的用户。"
            for (char in testResponse) {
                delay(100L)
                fullResponse += char
                _state.update { it.copy(messages = it.messages.dropLast(1) + ChatMessage(fullResponse, false)) }
            }

            chatHistory.add(Message("assistant", fullResponse))
            _state.update { it.copy(isResponding = false) }
        }
    }

    companion object {
        fun Factory(service: AiChatService) = viewModelFactory {
            initializer {
                ChatViewModel(service)
            }
        }
    }
}