package com.destywen.mydroid.ui.screen.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.destywen.mydroid.AppContainer
import com.destywen.mydroid.data.local.AgentEntity
import com.destywen.mydroid.data.local.AppSettings
import com.destywen.mydroid.data.local.ChatDao
import com.destywen.mydroid.data.local.ChatMessageEntity
import com.destywen.mydroid.data.local.Role
import com.destywen.mydroid.data.remote.AiChatService
import com.destywen.mydroid.data.remote.Message
import com.destywen.mydroid.domain.AppLogger
import com.destywen.mydroid.util.toDateTimeString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: Long,
    val name: String,
    val role: String,
    val content: String,
    val time: Long,
    val agentId: Long?,
) {
    val isAi: Boolean
        get() = role == Role.ASSISTANT
}

data class ChatScreenState(
    val messages: List<ChatMessage> = emptyList(),
    val allAgents: List<AgentEntity> = emptyList(),
    val userInput: String? = null,
    val defaultEndpoint: String? = null,
    val defaultApiKey: String? = null,
    val error: String? = null,
)

class ChatViewModel(
    private val service: AiChatService,
    private val chatDao: ChatDao,
    private val settings: AppSettings
) : ViewModel() {

    private val _messages = chatDao.getAllMessages()
    private val _agents = chatDao.getAgents()
    private val _settings =
        combine(settings.defaultEndpoint, settings.defaultApiKey, settings.username) { a, b, c -> Triple(a, b, c) }
    private val _generating = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val _userInput = MutableStateFlow<String?>(null)
    private val _error = MutableStateFlow<String?>(null)

    val state: StateFlow<ChatScreenState> = combine(
        _messages,
        _agents,
        _settings,
        combine(_generating, _userInput, _error) { a, b, c -> Triple(a, b, c) }
    ) { messages, agents, (endpoint, apiKey, username), (generating, input, error) ->
        ChatScreenState(
            messages = (messages.map { m ->
                val name = when (m.role) {
                    Role.ASSISTANT -> agents.find { it.id == m.agentId }?.name ?: "LLM-${m.agentId}"
                    Role.USER -> username
                    else -> m.role
                }
                ChatMessage(m.id, name, m.role, m.content, m.timestamp, m.agentId)
            } + generating).sortedBy { it.time },
            allAgents = agents,
            userInput = input,
            defaultEndpoint = endpoint,
            defaultApiKey = apiKey,
            error = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatScreenState())

    fun sendMessage(content: String) = viewModelScope.launch {
        val id = chatDao.insertMessage(ChatMessageEntity(role = Role.USER, content = content))
        val messages = chatDao.getAllMessages().first()
        val agents = _agents.first()

        // time-trigger
        messages.getOrNull(messages.lastIndex - 1)?.let { m ->
            if (m.role == Role.ASSISTANT && ((messages.last().timestamp - m.timestamp) < 5 * 60 * 1000)) {
                agents.find { it.id == m.agentId }?.let {
                    reply(it, id, messages)
                    return@launch
                }
            }
        }

        // keyword-trigger
        if (content.contains("全体成员")) {
            agents.forEach { reply(it, id, messages) }
        } else {
            agents.forEach { agent ->
                if (content.contains(agent.name, ignoreCase = true)) {
                    reply(agent, id, messages)
                }
            }
        }
    }

    fun reply(agent: AgentEntity, targetId: Long, messages: List<ChatMessageEntity>) = viewModelScope.launch {
        val history = messages.takeWhile { it.id != targetId }.takeLast(20)
        val target = messages.first { it.id == targetId }
        val context = buildContext(history + listOf(target), agent.id)

        val tempId = -agent.id
        val createTime = System.currentTimeMillis()
        val generating = ChatMessage(
            id = tempId,
            name = agent.name,
            agentId = agent.id,
            role = Role.ASSISTANT,
            content = "...",
            time = createTime,
        )
        _generating.update { currentList ->
            if (currentList.any { it.id == tempId }) currentList else currentList + listOf(generating)
        }
        var fullContent = ""

        service.chatStreaming(context, agent).catch { e ->
            _error.update { e.message }
            _generating.update { list -> list.filterNot { it.id == tempId } }
            AppLogger.e("chatStreaming", e.message ?: "unknown error")
        }.collect { token ->
            fullContent += token
            _generating.update { list ->
                list.map { msg ->
                    if (msg.id == tempId) msg.copy(content = fullContent) else msg
                }
            }
        }

        if (fullContent.isNotEmpty()) {
            chatDao.insertMessage(
                ChatMessageEntity(
                    agentId = agent.id,
                    role = Role.ASSISTANT,
                    content = fullContent,
                    timestamp = createTime
                )
            )
        }
        _generating.update { list -> list.filterNot { it.id == tempId } }
    }

    suspend fun nameOf(message: ChatMessageEntity): String {
        return when (message.role) {
            Role.ASSISTANT -> {
                _agents.first().find { it.id == message.agentId }?.name ?: "LLM-${message.agentId}"
            }

            Role.USER -> {
                settings.username.first()
            }

            else -> {
                message.role
            }
        }
    }

    // return context with user, assistant, user, assistant, ..., user
    // all messages not sent by agentId are considered user message
    suspend fun buildContext(all: List<ChatMessageEntity>, agentId: Long): List<Message> {
        val messages = all.dropWhile { it.agentId == agentId }.dropLastWhile { it.agentId == agentId }.toMutableList()
        if (messages.isEmpty()) return emptyList()

        suspend fun timedContent(m: ChatMessageEntity): String {
            return "[${m.timestamp.toDateTimeString()}]\n${nameOf(m)}: ${m.content}\n"
        }

        val result = mutableListOf<Message>()

        var users = messages.takeWhile { it.agentId != agentId }
        while (users.isNotEmpty()) {
            result.add(Message(Role.USER, users.map { timedContent(it) }.joinToString("\n")))
            messages.subList(0, users.size).clear()
            val ais = messages.takeWhile { it.agentId == agentId }
            if (ais.isEmpty()) {
                break
            } else {
                result.add(Message(Role.ASSISTANT, ais.joinToString("\n") { it.content }))
                messages.subList(0, ais.size).clear()
                users = messages.takeWhile { it.agentId != agentId }
            }
        }

        return result
    }

    fun deleteMessage(id: Long) = viewModelScope.launch {
        chatDao.deleteMessageById(id)
    }

    fun upsertAgent(agent: AgentEntity) = viewModelScope.launch {
        chatDao.upsertAgent(agent)
    }

    fun deleteAgent(agentId: Long) = viewModelScope.launch {
        chatDao.deleteAgent(agentId)
    }

    fun updateDefault(endpoint: String?, apiKey: String?) = viewModelScope.launch {
        endpoint?.takeIf { it.isNotBlank() }?.let { settings.updateDefaultEndpoint(it) }
        apiKey?.takeIf { it.isNotBlank() }?.let { settings.updateDefaultApiKey(it) }
    }

    fun clearMessages() = viewModelScope.launch {
        chatDao.clearMessage()
    }

    companion object {
        fun Factory(container: AppContainer) = viewModelFactory {
            initializer {
                ChatViewModel(container.chatService, container.chatDao, container.settings)
            }
        }
    }
}