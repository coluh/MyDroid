package com.destywen.mydroid.ui.screen.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.destywen.mydroid.AppContainer
import com.destywen.mydroid.data.local.AppSettings
import com.destywen.mydroid.data.local.MessageEntity
import com.destywen.mydroid.data.local.UserEntity
import com.destywen.mydroid.data.remote.AiChatService
import com.destywen.mydroid.domain.ChatRepository
import com.destywen.mydroid.domain.FileManager
import com.destywen.mydroid.domain.model.Conversation
import com.destywen.mydroid.domain.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class ChatViewModel(
    private val service: AiChatService,
    private val repository: ChatRepository,
    private val manager: FileManager,
    private val settings: AppSettings
) : ViewModel() {

    val conversations: StateFlow<List<Conversation>> =
        repository.getAllConversations().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val users: StateFlow<List<UserEntity>> =
        repository.getUsers().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val selfId: StateFlow<Long?> = settings.userId.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val error = MutableStateFlow<String?>(null)

    fun getMessages(convId: Long): StateFlow<List<Message>> =
        repository.getMessages(convId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun createUser(name: String, avatar: Uri?) = viewModelScope.launch(Dispatchers.IO) {
        val avatarPath = if (avatar != null) {
            manager.saveImage(avatar, "img/avatars", "${name}_${System.currentTimeMillis()}.png")
        } else null
        repository.createUser(name, avatarPath)
    }

    fun setSelf(userId: Long) = viewModelScope.launch {
        settings.updateUserId(userId)
    }

    fun sendMessage(convId: Long, content: String, senderId: Long? = null) = viewModelScope.launch {
        val userId = senderId ?: settings.userId.first()
        if (userId == null) {
            error.update { "self user id not set" }
            return@launch
        }
        repository.sendTextMessage(convId, userId, content)
    }

    fun clear() = viewModelScope.launch {
        repository.clear()
    }

    companion object {
        fun Factory(container: AppContainer) = viewModelFactory {
            initializer {
                ChatViewModel(
                    container.chatService,
                    container.chatRepository,
                    container.fileManager,
                    container.settings
                )
            }
        }
    }
}