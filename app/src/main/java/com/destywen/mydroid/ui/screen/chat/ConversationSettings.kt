package com.destywen.mydroid.ui.screen.chat

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Slider
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.destywen.mydroid.MyApplication
import com.destywen.mydroid.data.local.AppSettings
import com.destywen.mydroid.data.local.LlmConfigEntity
import com.destywen.mydroid.domain.ChatRepository
import com.destywen.mydroid.domain.model.ConversationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Composable
fun ConversationSettingsScreen(convId: Long, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as MyApplication
    val viewModel: ConversationSettingsViewModel =
        viewModel(factory = ConversationSettingsViewModel.Factory(convId, app))
    val state = viewModel.state.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = AppBarDefaults.topAppBarWindowInsets,
                title = { Text("聊天设置") },
                navigationIcon = {
                    IconButton({ onBack() }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null) }
                },
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
            key(state.value.llmConfig?.id) {
                LlmConfigEditor(state.value.llmConfig) { entity ->
                    viewModel.saveLlmConfig(entity)
                }
            }
        }
    }
}

@Composable
fun LlmConfigEditor(origin: LlmConfigEntity? = null, onSave: (LlmConfigEntity) -> Unit) {
    var name by rememberSaveable { mutableStateOf(origin?.name ?: "") }
    var provider by rememberSaveable { mutableStateOf(origin?.provider ?: "") }
    var model by rememberSaveable { mutableStateOf(origin?.model ?: "") }
    var apiKey by rememberSaveable { mutableStateOf(origin?.apiKey ?: "") }
    var systemPrompt by rememberSaveable { mutableStateOf(origin?.systemPrompt ?: "") }
    var temperature by rememberSaveable { mutableFloatStateOf(origin?.temperature ?: 0.7f) }
    var maxTokens by rememberSaveable { mutableIntStateOf(origin?.maxTokens ?: 2048) }
    var topP by rememberSaveable { mutableFloatStateOf(origin?.topP ?: 0.9f) }
    var maxTokensText by rememberSaveable { mutableStateOf((origin?.maxTokens ?: 2048).toString()) }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        OutlinedTextField(name, { name = it }, label = { Text("name") })
        OutlinedTextField(provider, { provider = it }, label = { Text("provider") })
        OutlinedTextField(model, { model = it }, label = { Text("model") })
        OutlinedTextField(apiKey, { apiKey = it }, label = { Text("apiKey") })
        OutlinedTextField(systemPrompt, { systemPrompt = it }, label = { Text("systemPrompt") })
        Slider(temperature, { temperature = it }, valueRange = 0f..2f, steps = 19)
        Slider(topP, { topP = it }, valueRange = 0f..1f, steps = 9)
        OutlinedTextField(
            value = maxTokensText,
            onValueChange = { value ->
                maxTokensText = value
                value.toIntOrNull()?.let { maxTokens = it }
            },
            label = { Text("Max Tokens") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Button(onClick = {
            onSave(
                LlmConfigEntity(
                    id = origin?.id ?: 0,
                    userId = origin?.userId,
                    name = name,
                    provider = provider,
                    model = model,
                    apiKey = apiKey,
                    systemPrompt = systemPrompt,
                    temperature = temperature,
                    maxTokens = maxTokens,
                    topP = topP,
                    createdAt = origin?.createdAt ?: System.currentTimeMillis()
                )
            )
        }) { Text("保存") }
    }
}

data class ConvSettingsUiState(
    val type: ConversationType = ConversationType.GROUP,
    val userId: Long? = null,
    val llmConfig: LlmConfigEntity? = null,
)

class ConversationSettingsViewModel(
    private val convId: Long,
    private val repository: ChatRepository,
    settings: AppSettings,
) : ViewModel() {

    private val convType: ConversationType = runBlocking(Dispatchers.IO) {
        repository.getConvType(convId)
    }

    val state = combine(
        repository.getMemberIds(convId),
        repository.getLlmConfigs(),
        settings.userId,
    ) { members, llmConfigs, selfId ->
        if (convType == ConversationType.PRIVATE) {
            val otherId = members.find { it != selfId }
            ConvSettingsUiState(
                type = ConversationType.PRIVATE,
                userId = otherId,
                llmConfig = llmConfigs.find { it.userId == otherId },
            )
        } else {
            ConvSettingsUiState(
                type = ConversationType.GROUP,
                userId = null,
                llmConfig = null,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ConvSettingsUiState())

    fun saveLlmConfig(llmConfig: LlmConfigEntity) = viewModelScope.launch(Dispatchers.IO) {
        repository.saveLlmConfig(llmConfig.copy(userId = state.value.userId))
    }

    companion object {
        fun Factory(convId: Long, application: Application) = viewModelFactory {
            initializer {
                val app = application as MyApplication
                ConversationSettingsViewModel(convId, app.chatRepository, app.settings)
            }
        }
    }
}