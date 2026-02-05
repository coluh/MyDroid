package com.destywen.mydroid.ui.screen.chat

import android.content.ClipData
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.destywen.mydroid.R
import com.destywen.mydroid.data.local.ChatAgent
import com.destywen.mydroid.ui.components.BottomModal
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(viewModel: ChatViewModel, onNavigate: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showEditor by rememberSaveable { mutableStateOf(false) }
    var showList by rememberSaveable { mutableStateOf(false) }
    var editingAgentId by rememberSaveable { mutableStateOf<String?>(null) }
    val editingAgent = state.allAgents.find { it.id == editingAgentId }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var deletingMessage by remember { mutableStateOf<ChatMessage?>(null) }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            delay(2000)
            viewModel.clearSnackbar()
        }
    }
    LaunchedEffect(state.messages.size) {
        listState.animateScrollToItem(0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = AppBarDefaults.topAppBarWindowInsets,
                title = { Text(stringResource(R.string.chat)) },
                navigationIcon = {
                    IconButton(onClick = { onNavigate() }) {
                        Icon(Icons.Default.Menu, null)
                    }
                },
                actions = {
                    var menuExpanded by remember { mutableStateOf(false) }
                    ModelSelector(
                        modifier = Modifier.width(200.dp),
                        options = state.allAgents,
                        toText = { "${it.name} - ${it.modelName}" },
                        selected = state.selectedAgent,
                        onSelect = {
                            viewModel.selectAgent(it.id)
                        })
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, null)
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                onClick = {
                                    menuExpanded = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar("click settings")
                                    }
                                }
                            ) { Text("设置") }
                            DropdownMenuItem(
                                onClick = {
                                    menuExpanded = false
                                    editingAgentId = null
                                    showEditor = true
                                }
                            ) { Text("新建") }
                            DropdownMenuItem(
                                onClick = {
                                    menuExpanded = false
                                    showList = true
                                }
                            ) { Text("编辑") }
                            DropdownMenuItem(
                                onClick = {
                                    menuExpanded = false
                                    state.selectedAgent?.let { viewModel.deleteHistory(it.id) }
                                }
                            ) { Text("清空") }
                        }
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { contentPadding ->
        Column(
            Modifier
                .padding(contentPadding)
                .imePadding()
                .fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { focusManager.clearFocus() })
                    }
            ) {
                items(state.messages.asReversed(), key = { it.hashCode() }) { message ->
                    ChatBubble(message, onDelete = {
                        deletingMessage = it
                        showDeleteConfirm = true
                    })
                }
                item {
                    Spacer(modifier = Modifier.height(0.dp))
                }
            }

            UserInput(modifier = Modifier.fillMaxWidth(), state.userInput, state.isResponding) {
                viewModel.sendMessage(it)
            }
        }

        BottomModal(
            visible = showDeleteConfirm,
            modifier = Modifier.padding(contentPadding),
            onDismissRequest = {
                deletingMessage = null
                showDeleteConfirm = false
            }
        ) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                confirmButton = {
                    Button(onClick = {
                        showDeleteConfirm = false
                        val idx = state.messages.indexOfFirst { it.id == deletingMessage!!.id }
                        val idx2 = if (deletingMessage!!.isUser) idx + 1 else idx - 1
                        viewModel.deleteMessageById(state.messages[idx].id)
                        viewModel.deleteMessageById(state.messages[idx2].id)
                    }) { Text("确认") }
                },
                text = { Text("确认删除这轮对话吗？") }
            )
        }

        BottomModal(
            visible = showList,
            modifier = Modifier.padding(contentPadding),
            onDismissRequest = { showList = false }
        ) {
            AgentList(state.allAgents) { agent ->
                showList = false
                editingAgentId = agent.id
                showEditor = true
            }
        }

        BottomModal(
            visible = showEditor,
            modifier = Modifier.padding(contentPadding),
            onDismissRequest = { showEditor = false }
        ) {
            AgentEditor(
                initial = editingAgent,
                allAgents = state.allAgents,
                onDismiss = { showEditor = false },
                onDelete = { viewModel.deleteAgent(it.id) },
                onSave = { viewModel.saveAgent(it) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgentEditor(
    initial: ChatAgent?,
    allAgents: List<ChatAgent> = emptyList(),
    onDismiss: () -> Unit,
    onDelete: (agent: ChatAgent) -> Unit = {},
    onSave: (agent: ChatAgent) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var model by rememberSaveable { mutableStateOf(initial?.modelName ?: "") }
    var prompt by rememberSaveable { mutableStateOf(initial?.systemPrompt ?: "") }
    var endpoint by rememberSaveable { mutableStateOf(initial?.endpoint ?: "") }
    var apiKey by rememberSaveable { mutableStateOf(initial?.apiKey ?: "") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // name, endpoint, apikey, modelName, systemPrompt
        OutlinedTextField(name, { name = it }, label = { Text("名称") })
        OutlinedTextField(model, { model = it }, label = { Text("模型") })
        OutlinedTextField(prompt, { prompt = it }, label = { Text("系统提示词") })
        OutlinedTextField(endpoint, { endpoint = it }, label = { Text("接口地址") })
        OutlinedTextField(apiKey, { apiKey = it }, label = { Text("API KEY") })
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End)) {
            if (initial != null) {
                TextButton(onClick = { onDelete(initial); onDismiss() }) {
                    Text("删除", color = Color.Red)
                }
            }
            TextButton(onClick = { onDismiss() }) {
                Text("取消")
            }
            Button(
                enabled = name.isNotBlank() && model.isNotBlank() && endpoint.isNotBlank() && apiKey.isNotBlank(),
                onClick = {
                    onSave(
                        if (initial != null) {
                            ChatAgent(
                                id = initial.id,
                                name = name,
                                endpoint = endpoint,
                                apiKey = apiKey,
                                modelName = model,
                                systemPrompt = prompt
                            )
                        } else {
                            ChatAgent(
                                name = name,
                                endpoint = endpoint,
                                apiKey = apiKey,
                                modelName = model,
                                systemPrompt = prompt
                            )
                        }
                    )
                    onDismiss()
                }) {
                Text("保存")
            }
        }
    }
}

@Composable
fun UserInput(modifier: Modifier, text: String? = null, responding: Boolean = true, onSend: (content: String) -> Unit) {
    var inputText by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(text) {
        text?.let { inputText = text }
    }

    Surface(modifier = modifier) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                maxLines = 4
            )
            IconButton(
                enabled = !responding && inputText.isNotBlank(),
                onClick = {
                    onSend(inputText)
                    inputText = ""
                }) {
                if (!responding) {
                    Icon(Icons.AutoMirrored.Filled.Send, null)
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colors.primary
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, onDelete: (msg: ChatMessage) -> Unit) {

    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val containerColor = when {
        message.isUser -> MaterialTheme.colors.primary
        else -> MaterialTheme.colors.surface
    }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .combinedClickable(null, LocalIndication.current, onLongClick = {
                onDelete(message)
            }, onClick = {}),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = containerColor,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .widthIn(max = 300.dp)
                .border(BorderStroke(1.dp, MaterialTheme.colors.onSurface), RoundedCornerShape(16.dp))
                .clickable(null, LocalIndication.current) {
                    scope.launch {
                        clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("content", message.text)))
                    }
                }
        ) {
            Text(message.text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
        }
    }
}

@Composable
fun AgentList(agents: List<ChatAgent>, onSelect: (ChatAgent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        agents.forEach { agent ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(null, LocalIndication.current, onClick = {
                        onSelect(agent)
                    }),
                elevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        Text(agent.name, fontWeight = FontWeight.Bold, lineHeight = 1.sp)
                        Text(
                            agent.modelName,
                            fontSize = 14.sp,
                            lineHeight = 1.sp,
                            fontWeight = FontWeight.Bold
                        )

                    }
                    Text(
                        agent.systemPrompt,
                        fontSize = 14.sp,
                        maxLines = 1,
                        softWrap = false,
                        lineHeight = 1.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        agent.endpoint,
                        fontSize = 12.sp,
                        softWrap = false,
                        maxLines = 1,
                        lineHeight = 1.sp
                    ) // TODO: simplify these
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun <T> ModelSelector(
    modifier: Modifier = Modifier,
    options: List<T>,
    toText: (T) -> String,
    selected: T? = null,
    label: @Composable (() -> Unit)? = null,
    onSelect: (T) -> Unit
) {
    var shouldExpand by remember { mutableStateOf(false) }

    val expanded = shouldExpand && options.isNotEmpty()
    val showText = if (selected == null) "-- - --" else toText(selected)

    ExposedDropdownMenuBox(modifier = modifier, expanded = expanded, onExpandedChange = { shouldExpand = it }) {
        TextField(
            value = showText,
            onValueChange = {},
            readOnly = true,
            textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold),
            label = label,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(
                backgroundColor = Color.Transparent
            ),
            singleLine = true
        )

        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { shouldExpand = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    onClick = {
                        shouldExpand = false
                        onSelect(option)
                    }
                ) { Text(toText(option)) }
            }
        }
    }
}
