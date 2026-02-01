package com.destywen.mydroid.ui.screen.chat

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.destywen.mydroid.ui.components.Dropdown
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel, onNavigate: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showEditor by rememberSaveable { mutableStateOf(false) }
    var showList by rememberSaveable { mutableStateOf(false) }
    var editingAgentId by rememberSaveable { mutableStateOf<String?>(null) }
    val editingAgent = state.allAgents.find { it.id == editingAgentId }

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
    LaunchedEffect(state.messages) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex) // TODO: scroll to bottom
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.chat)) },
                navigationIcon = {
                    IconButton(onClick = { onNavigate() }) {
                        Icon(Icons.Default.Menu, null)
                    }
                },
                actions = {
                    var menuExpanded by remember { mutableStateOf(false) }
                    Dropdown(
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
                                text = { Text("设置") },
                                onClick = {
                                    menuExpanded = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar("click settings")
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("新建") },
                                onClick = {
                                    menuExpanded = false
                                    editingAgentId = null
                                    showEditor = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("编辑") },
                                onClick = {
                                    menuExpanded = false
                                    showList = true
                                }
                            )
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
                modifier = Modifier
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { focusManager.clearFocus() })
                    }
            ) {
                items(state.messages) { msg ->
                    ChatBubble(msg)
                }
            }

            UserInput(modifier = Modifier.fillMaxWidth(), !state.isResponding) {
                viewModel.sendMessage(it)
            }
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
//        EditableDropdown(options = listOf("http1", "http2"), label = { Text("接口地址") }) { endpoint = it }
//        EditableDropdown(options = emptyList(), label = { Text("API KEY") }) { apiKey = it }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
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
fun UserInput(modifier: Modifier, enabled: Boolean = true, onSend: (content: String) -> Unit) {
    var inputText by rememberSaveable { mutableStateOf("") }

    Surface(modifier = modifier, tonalElevation = 2.dp) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                maxLines = 4
            )
            IconButton(
                enabled = enabled && inputText.isNotBlank(),
                onClick = {
                    onSend(inputText)
                    inputText = ""
                }) {
                Icon(Icons.AutoMirrored.Filled.Send, null)
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {

    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val containerColor = if (message.isUser)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.secondaryContainer

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = containerColor,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 300.dp)
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
                    })
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
                    )
                }
            }
        }
    }
}