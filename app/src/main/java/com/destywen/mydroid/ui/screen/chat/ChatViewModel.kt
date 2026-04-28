package com.destywen.mydroid.ui.screen.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.destywen.mydroid.AppContainer
import com.destywen.mydroid.data.local.AppSettings
import com.destywen.mydroid.data.remote.AiChatService
import com.destywen.mydroid.domain.ChatRepository


class ChatViewModel(
    private val service: AiChatService,
    private val chatRepository: ChatRepository,
    private val settings: AppSettings
) : ViewModel() {


    companion object {
        fun Factory(container: AppContainer) = viewModelFactory {
            initializer {
                ChatViewModel(container.chatService, container.chatRepository, container.settings)
            }
        }
    }
}