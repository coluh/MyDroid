package com.destywen.mydroid.ui.screen.chat

import android.app.Application
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.destywen.mydroid.MyApplication
import com.destywen.mydroid.R
import com.destywen.mydroid.data.local.AppSettings
import com.destywen.mydroid.domain.ChatRepository
import com.destywen.mydroid.domain.model.Message
import com.destywen.mydroid.domain.model.MessageType
import com.destywen.mydroid.util.toSmartTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class ConversationUiState(
    val messages: List<Message> = emptyList(),
    val selfUserId: Long? = null,
    val error: String? = null,
)

class ConversationViewModel(
    convId: Long,
    private val repository: ChatRepository,
    settings: AppSettings,
) : ViewModel() {

    private val _messages = repository.getMessages(convId)
    private val _selfId = settings.userId
    private val _error = MutableStateFlow<String?>(null)

    val state = combine(_messages, _selfId, _error) { messages, selfId, error ->
        ConversationUiState(messages, selfId, error)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ConversationUiState())

    fun sendMessage(convId: Long, content: String, senderId: Long? = null) = viewModelScope.launch(Dispatchers.IO) {
        val userId = senderId ?: _selfId.first()
        if (userId == null) {
            _error.update { "self user id not set" }
            return@launch
        }
        repository.sendTextMessage(convId, userId, content)
    }

    companion object {
        fun Factory(convId: Long, application: Application) = viewModelFactory {
            initializer {
                val app = application as MyApplication
                ConversationViewModel(convId, app.chatRepository, app.settings)
            }
        }
    }
}

@Composable
fun ConversationScreen(convId: Long, onNavigateSettings: () -> Unit, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as Application
    val viewModel: ConversationViewModel = viewModel(factory = ConversationViewModel.Factory(convId, app))
    val state = viewModel.state.collectAsStateWithLifecycle()
    val messageItems = rememberMessageItems(state.value.messages)

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = AppBarDefaults.topAppBarWindowInsets,
                title = { Text(state.value.error ?: stringResource(R.string.chat)) },
                navigationIcon = {
                    IconButton({ onBack() }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null) }
                },
                actions = {
                    IconButton(onClick = {
                        onNavigateSettings()
                    }) {
                        Icon(Icons.Default.Menu, null)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { contentPadding ->
        Column(
            Modifier
                .padding(contentPadding)
                .imePadding()
                .fillMaxSize()
        ) {
            LazyColumn(
                state = listState, reverseLayout = true, modifier = Modifier
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { focusManager.clearFocus() })
                    }
            ) {
                items(messageItems.asReversed(), key = {
                    when (it) {
                        is MessageItem.MessageContent -> "msg_${it.message.id}"
                        is MessageItem.TimeDivider -> "time_${it.time}"
                    }
                }) { item ->
                    when (item) {
                        is MessageItem.MessageContent -> {
                            val message = item.message
                            when (message.type) {
                                MessageType.TEXT -> {
                                    ChatBubble(
                                        message.content,
                                        message.isSelf,
                                        message.senderAvatar,
                                        message.senderName
                                    )
                                }

                                else -> ChatBubble("not supported: ${message.content}")
                            }
                        }

                        is MessageItem.TimeDivider -> TimeDivider(item.time)

                    }
                }
            }
            InputLine { viewModel.sendMessage(convId, it) }
        }
    }
}

sealed class MessageItem {
    data class MessageContent(val message: Message) : MessageItem()
    data class TimeDivider(val time: String) : MessageItem()
}

// messages sort by time asc
@Composable
fun rememberMessageItems(messages: List<Message>): List<MessageItem> {
    return remember(messages) {
        val result = mutableListOf<MessageItem>()
        val threshold = 5 * 60 * 1000
        val now = LocalDateTime.now()

        for (i in messages.indices) {
            val current = messages[i]
            if (i == 0 || (current.timestamp - messages[i - 1].timestamp) >= threshold) {
                result.add(MessageItem.TimeDivider(current.timestamp.toSmartTime(now)))
            }
            result.add(MessageItem.MessageContent(current))
        }

        result
    }
}

@Composable
fun ChatBubble(content: String, isSelf: Boolean = false, avatar: String? = null, name: String = "?") {
    val alignment = if (isSelf) Alignment.End else Alignment.Start
    val bgColor = if (isSelf) MaterialTheme.colors.primary else MaterialTheme.colors.surface

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(10.dp, 5.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, alignment),
        ) {
            if (isSelf) {
                Surface(color = bgColor, shape = RoundedCornerShape(20.dp)) {
                    Text(content, modifier = Modifier.padding(12.dp, 8.dp))
                }
                Avatar(avatar, name, size = 50)
            } else {
                Avatar(avatar, name, size = 50)
                Surface(color = bgColor, shape = RoundedCornerShape(20.dp)) {
                    Text(content, modifier = Modifier.padding(12.dp, 8.dp))
                }
            }
        }
    }
}
