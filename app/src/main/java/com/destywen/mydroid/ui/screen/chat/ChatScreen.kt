package com.destywen.mydroid.ui.screen.chat

import android.content.ClipData
import android.os.Parcelable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Slider
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.destywen.mydroid.R
import com.destywen.mydroid.data.local.AgentEntity
import com.destywen.mydroid.data.local.Role
import com.destywen.mydroid.ui.components.AgentCard
import com.destywen.mydroid.ui.components.BottomModal
import com.destywen.mydroid.util.toSmartTime
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime

@Parcelize
sealed class ChatScreenModal : Parcelable {
    object None : ChatScreenModal()
    object Setting : ChatScreenModal()
    data class Editor(val agent: AgentEntity?) : ChatScreenModal()
    data class Agents(val agents: List<AgentEntity>) : ChatScreenModal()
}

sealed class ChatListItem {
    data class MessageItem(val message: ChatMessage) : ChatListItem()
    data class TimeDivider(val timeText: String) : ChatListItem()
}

@Composable
fun rememberChatItems(messages: List<ChatMessage>): List<ChatListItem> {
    return remember(messages) {
        val result = mutableListOf<ChatListItem>()
        val threshold = 5 * 60 * 1000
        val now = LocalDateTime.now()

        for (i in messages.indices) {
            val current = messages[i]
            if (i == 0 || (current.time - messages[i - 1].time) >= threshold) {
                result.add(ChatListItem.TimeDivider(current.time.toSmartTime(now)))
            }
            result.add(ChatListItem.MessageItem(current))
        }

        result
    }
}

@Composable
fun ChatScreen(viewModel: ChatViewModel, onNavigate: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val chatItems = rememberChatItems(state.messages)
    var activeModal by rememberSaveable { mutableStateOf<ChatScreenModal>(ChatScreenModal.None) }

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.messages.size) {
        listState.animateScrollToItem(0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = AppBarDefaults.topAppBarWindowInsets,
                title = { Text(state.error ?: stringResource(R.string.chat)) },
                navigationIcon = {
                    IconButton({ onNavigate() }) { Icon(Icons.Default.Menu, null) }
                },
                actions = {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton({ expanded = !expanded }) { Icon(Icons.Default.MoreVert, null) }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem({
                                expanded = false
                                activeModal = ChatScreenModal.Setting
                            }) { Text("设置") }
                            DropdownMenuItem({
                                expanded = false
                                activeModal = ChatScreenModal.Editor(null)
                            }) { Text("新建") }
                            DropdownMenuItem({
                                expanded = false
                                activeModal = ChatScreenModal.Agents(state.allAgents)
                            }) { Text("列表") }
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .imePadding()
                .fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { focusManager.clearFocus() })
                    }
            ) {
                item {
                    Spacer(Modifier.height(0.dp))
                }
                items(chatItems.asReversed(), key = {
                    when (it) {
                        is ChatListItem.MessageItem -> "msg_${it.message.id}"
                        is ChatListItem.TimeDivider -> "time_${it.timeText}"
                    }
                }) { item ->
                    when (item) {
                        is ChatListItem.MessageItem -> ChatBubble(
                            item.message,
                            onDelete = { viewModel.deleteMessage(it) })

                        is ChatListItem.TimeDivider -> TimeDividerLabel(item.timeText)
                    }

                }
            }
            UserInput(state.userInput) { viewModel.sendMessage(it) }
        }

        val dismiss = { activeModal = ChatScreenModal.None }
        BottomModal(
            visible = activeModal != ChatScreenModal.None,
            onDismissRequest = dismiss
        ) {
            when (val modal = activeModal) {
                is ChatScreenModal.Setting -> Setting(
                    state.defaultEndpoint,
                    state.defaultApiKey,
                    dismiss
                ) { endpoint, key ->
                    viewModel.updateDefault(endpoint, key)
                }

                is ChatScreenModal.Editor -> AgentEditor(modal.agent, dismiss) {
                    viewModel.upsertAgent(it)
                }

                is ChatScreenModal.Agents -> AgentList(
                    modal.agents, dismiss,
                    onEdit = { activeModal = ChatScreenModal.Editor(it) },
                    onDelete = { viewModel.deleteAgent(it) }
                )

                else -> {}
            }
        }
    }
}

@Composable
fun TimeDividerLabel(time: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = time,
            style = MaterialTheme.typography.caption,
            color = Color.Gray,
            modifier = Modifier
//                .background(Color.LightGray.copy(0.2f), CircleShape)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun UserInput(text: String? = null, onSend: (String) -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(text) {
        text?.let { input = text } // controlled by viewModel
    }

    Surface(color = MaterialTheme.colors.background) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                maxLines = 4
            )
            IconButton(
                {
                    onSend(input)
                    input = ""
                },
                modifier = Modifier
                    .padding(0.dp, 8.dp)
                    .width(64.dp)
                    .height(48.dp)
                    .background(
                        color = if (input.isNotBlank()) MaterialTheme.colors.primary else Color.Gray,
                        shape = RoundedCornerShape(8.dp)
                    ),
                enabled = input.isNotEmpty()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send, null,
                    tint = MaterialTheme.colors.onPrimary
                )
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, modifier: Modifier = Modifier, onDelete: (id: Long) -> Unit) {
    val alignment = if (message.isAi) Alignment.Start else Alignment.End
    val containerColor = if (message.isAi) MaterialTheme.colors.surface else MaterialTheme.colors.primary

    var expanded by remember { mutableStateOf(false) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(8.dp, 4.dp), horizontalAlignment = alignment
        ) {
            val name = if (message.role == Role.USER) stringResource(R.string.username) else message.name
            Text(name, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Box {
                Surface(
                    color = containerColor, shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.combinedClickable(
                        null, LocalIndication.current,
                        onClick = {}, onLongClick = { expanded = true })
                ) {
                    Text(message.content, modifier = Modifier.padding(12.dp, 8.dp))
                }
                DropdownMenu(expanded, { expanded = false }) {
                    DropdownMenuItem({
                        scope.launch {
                            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("content", message.content)))
                        }
                    }) { Text("复制") }
                    DropdownMenuItem({
                        onDelete(message.id)
                    }) { Text("删除") }
                }
            }
        }
    }
}

@Composable
private fun Setting(
    originEndpoint: String?,
    originApiKey: String?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var endpoint by rememberSaveable { mutableStateOf(originEndpoint ?: "") }
    var apiKey by rememberSaveable { mutableStateOf(originApiKey ?: "") }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(endpoint, { endpoint = it }, label = { Text("默认endpoint") })
        OutlinedTextField(apiKey, { apiKey = it }, label = { Text("默认api key") })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton({ onDismiss() }) { Text("取消") }
            Button({
                onSave(endpoint, apiKey)
                onDismiss()
            }) { Text("保存") }
        }
    }
}

@Composable
fun AgentEditor(origin: AgentEntity?, onDismiss: () -> Unit, onComplete: (AgentEntity) -> Unit) {
    var name by rememberSaveable { mutableStateOf(origin?.name ?: "") }
    var prompt by rememberSaveable { mutableStateOf(origin?.systemPrompt ?: "") }
    var model by rememberSaveable { mutableStateOf(origin?.modelName ?: "") }
    var temperature by rememberSaveable { mutableFloatStateOf(origin?.temperature ?: 0.7f) }
    var endpoint by rememberSaveable { mutableStateOf(origin?.apiEndpoint ?: "") }
    var key by rememberSaveable { mutableStateOf(origin?.apiKey ?: "") }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(if (origin == null) "新建" else "编辑")
        OutlinedTextField(name, { name = it }, label = { Text("名称") })
        OutlinedTextField(prompt, { prompt = it }, label = { Text("提示词") })
        OutlinedTextField(model, { model = it }, label = { Text("模型") })
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Slider(temperature, { temperature = it }, valueRange = 0f..2f, steps = 19, modifier = Modifier.weight(1f))
            Text("温度：${"%.1f".format(temperature)}")
        }
        OutlinedTextField(endpoint, { endpoint = it }, label = { Text("endpoint") })
        OutlinedTextField(key, { key = it }, label = { Text("API KEY") })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton({ onDismiss() }) { Text("取消") }
            Button({
                val apiEndpoint = endpoint.takeIf { it.isNotBlank() }
                val apiKey = key.takeIf { it.isNotBlank() }
                onComplete(
                    AgentEntity(
                        id = origin?.id ?: 0,
                        name = name,
                        systemPrompt = prompt,
                        modelName = model,
                        temperature = temperature,
                        apiEndpoint = apiEndpoint,
                        apiKey = apiKey
                    )
                )
                onDismiss()
            }, enabled = name.isNotBlank() && model.isNotBlank()) { Text("保存") }
        }
    }
}

@Composable
fun AgentList(
    agents: List<AgentEntity>,
    onDismiss: () -> Unit,
    onEdit: (AgentEntity) -> Unit,
    onDelete: (Long) -> Unit
) {
    LazyColumn(
        Modifier
            .fillMaxHeight(0.7f)
            .padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(agents, key = { it.id }) {
            var expanded by remember { mutableStateOf(false) }
            Box {
                AgentCard(
                    it, modifier = Modifier.combinedClickable(
                        null, LocalIndication.current,
                        onClick = {}, onLongClick = { expanded = true })
                )
                DropdownMenu(expanded, { expanded = false }) {
                    DropdownMenuItem({
                        onEdit(it)
                    }) { Text("编辑") }
                    DropdownMenuItem({
                        onDelete(it.id)
                    }) { Text("删除") }
                }
            }
        }
    }
}