package com.destywen.mydroid.ui.screen.chat

import android.os.Parcelable
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
import androidx.compose.material.DropdownMenu
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.destywen.mydroid.R
import com.destywen.mydroid.domain.model.Message
import com.destywen.mydroid.domain.model.MessageType
import com.destywen.mydroid.util.toSmartTime
import java.nio.file.WatchEvent
import java.time.LocalDateTime

@Composable
fun ConversationScreen(viewModel: ChatViewModel, convId: Long, onBack: () -> Unit) {
    val messages by viewModel.getMessages(convId).collectAsStateWithLifecycle()
    val messageItems = rememberMessageItems(messages)
    val users by viewModel.users.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = AppBarDefaults.topAppBarWindowInsets,
                title = { Text(error ?: stringResource(R.string.chat)) },
                navigationIcon = {
                    IconButton({ onBack() }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null) }
                },
                actions = {
                    IconButton(onClick = { }) {
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
                                    val user = users.find { it.id == message.senderId }
                                    ChatBubble(message.content, message.isSelf, user?.avatar, user?.name ?: "?")
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
