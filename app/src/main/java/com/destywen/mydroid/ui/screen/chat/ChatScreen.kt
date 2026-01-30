package com.destywen.mydroid.ui.screen.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.destywen.mydroid.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel, onNavigate: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var menuExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

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
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, null)
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("设置") },
                            onClick = { scope.launch { snackbarHostState.showSnackbar("click settings") } }
                        )
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
                modifier = Modifier.weight(1f)
            ) {
                items(state.messages) { msg ->
                    ChatBubble(msg)
                }
            }

            UserInput(modifier = Modifier.fillMaxWidth()) { content ->
                viewModel.sendMessageDebug(content)
            }
        }
    }
}

@Composable
fun UserInput(modifier: Modifier, onSend: (content: String) -> Unit) {
    var inputText by rememberSaveable { mutableStateOf("") }

    Surface(modifier = modifier, tonalElevation = 2.dp) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                maxLines = 4
            )
            IconButton(onClick = {
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
            .padding(8.dp), horizontalAlignment = alignment
    ) {
        Surface(color = containerColor, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(0.8f)) {
            Text(message.text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
        }
    }
}