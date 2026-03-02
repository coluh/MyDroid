package com.destywen.mydroid

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.destywen.mydroid.data.local.AgentEntity
import com.destywen.mydroid.data.local.AppSettings
import com.destywen.mydroid.data.local.Role
import com.destywen.mydroid.data.remote.AiChatService
import com.destywen.mydroid.data.remote.Message
import com.destywen.mydroid.data.remote.NetworkModule
import com.destywen.mydroid.ui.theme.MyDroidTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val selectedText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString() ?: ""
        val settings = AppSettings(this)
        val chatService = AiChatService(NetworkModule.client, settings)

        setContent {
            MyDroidTheme {
                ChatFloating(selectedText, chatService, { finish() })
            }
        }
    }
}

@Composable
fun ChatFloating(initialText: String, service: AiChatService, onDismiss: () -> Unit) {

    var context by rememberSaveable { mutableStateOf(listOf(Message(Role.USER, "解释这个：$initialText"))) }
    var input by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val config = AgentEntity(
        name = "qwen3.5-plus",
        modelName = "qwen3.5-plus",
        systemPrompt = "你是一个有帮助的助手，回复简洁有用。禁止使用任何Markdown格式。"
    )
    var currentJob: Job? = null

    // call with current context, add assistant message or fail
    fun call() {
        currentJob = scope.launch {
            var response = ""
            context = context + Message(Role.ASSISTANT, response)
            runCatching {
                service.chatStreaming(context, config).collect { token ->
                    response += token
                    context = context.dropLast(1) + Message(Role.ASSISTANT, response)
                }
            }.onFailure {
                context = context.dropLast(1)
            }
        }
    }

    LaunchedEffect(Unit) {
        call()
    }

    LaunchedEffect(context.size) {
        listState.animateScrollToItem(0)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(null, LocalIndication.current) { onDismiss() }
            .statusBarsPadding()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .clickable(null, LocalIndication.current, enabled = false) {},
            shape = RoundedCornerShape(16.dp),
            backgroundColor = MaterialTheme.colors.background,
        ) {
            Column(Modifier.padding(16.dp)) {
                Surface {
                    Text(
                        "> $initialText",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        style = MaterialTheme.typography.body2
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    state = listState,
                    reverseLayout = true,
                ) {
                    items(context.drop(1).reversed(), { it.hashCode() }) {
                        if (it.role == Role.ASSISTANT) {
                            Text("${config.modelName}: ${it.content}", style = MaterialTheme.typography.body2)
                        } else {
                            Text(it.content, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.body2)
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        input,
                        { input = it },
                        textStyle = MaterialTheme.typography.body2,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = {
                            currentJob?.cancel()
                            context = if (context.last().role == Role.USER) {
                                context.dropLast(1) + Message(Role.USER, context.last().content + input)
                            } else {
                                context + Message(Role.USER, input)
                            }
                            input = ""
                            call()
                        },
                        modifier = Modifier
                            .width(80.dp)
                            .height(48.dp),
                        enabled = input.isNotEmpty(),
                    ) { Text("发送") }
                }
            }
        }
    }
}

