package com.destywen.mydroid.ui.screen.chat

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.destywen.mydroid.R
import com.destywen.mydroid.data.local.AgentEntity
import com.destywen.mydroid.ui.components.AgentCard


@Composable
fun ChatScreen(viewModel: ChatViewModel, onNavigateConv: (Long) -> Unit, onDrawer: () -> Unit) {


    val snackbarHostState = remember { SnackbarHostState() }


    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = AppBarDefaults.topAppBarWindowInsets,
                title = { Text(stringResource(R.string.chat)) },
                navigationIcon = {
                    IconButton({ onDrawer() }) { Icon(Icons.Default.Menu, null) }
                },
                actions = {
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { contentPadding ->
        Column(Modifier.padding(contentPadding)) { }
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