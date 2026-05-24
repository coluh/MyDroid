package com.destywen.mydroid.ui.screen.settings

import android.app.Application
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.AlertDialog
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.destywen.mydroid.data.local.AppConfig
import com.destywen.mydroid.data.local.AppSettings
import com.destywen.mydroid.data.local.Keys
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onNavigate: () -> Unit) {
    val app = LocalContext.current.applicationContext as Application
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(app))
    val config by viewModel.config.collectAsStateWithLifecycle()

    var editingField by rememberSaveable { mutableStateOf<EditField?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = AppBarDefaults.topAppBarWindowInsets,
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = { onNavigate() }) {
                        Icon(Icons.Default.Menu, null)
                    }
                }
            )
        }
    ) { contentPadding ->
        LazyColumn(Modifier.padding(contentPadding)) {
            item {
                SettingsSection("用户信息") {
                    SettingsItem("用户名", config.username ?: "- -") {
                        editingField = EditField("用户名", config.username ?: "") {
                            viewModel.updateUsername(it)
                        }
                    }
                }
            }
            item {
                SettingsSection("随笔") {
                    SettingsItem("回复提示词", config.journalPrompt ?: "- -") {
                        editingField = EditField("回复提示词", config.journalPrompt ?: "", true) {
                            viewModel.updateJournalPrompt(it)
                        }
                    }
                }
            }
            item {
                SettingsSection("LLM API") {
                    SettingsItem("Endpoint", config.defaultEndpoint ?: "- -") {
                        editingField = EditField("Endpoint", config.defaultEndpoint ?: "", true) {
                            viewModel.updateDefaultEndpoint(it)
                        }
                    }
                    SettingsItem("API_KEY", config.defaultApiKey ?: "- -") {
                        editingField = EditField("API_KEY", config.defaultApiKey ?: "") {
                            viewModel.updateDefaultApiKey(it)
                        }
                    }
                    SettingsItem("Model", config.defaultModel ?: "- -") {
                        editingField = EditField("Model", config.defaultModel ?: "") {
                            viewModel.updateDefaultModel(it)
                        }
                    }
                    SettingsItem("Vision Model", config.defaultVisionModel ?: "- -") {
                        editingField = EditField("Vision Model", config.defaultVisionModel ?: "") {
                            viewModel.updateDefaultVisionModel(it)
                        }
                    }
                }
            }
        }

        editingField?.let { field ->
            EditDialog(
                title = field.title,
                value = field.value,
                multiLine = field.multiLine,
                onDismiss = { editingField = null },
                onConfirm = {
                    field.onSave(it)
                    editingField = null
                }
            )
        }
    }
}

private data class EditField(
    val title: String,
    val value: String,
    val multiLine: Boolean = false,
    val onSave: (String) -> Unit
)

@Composable
private fun EditDialog(
    title: String,
    value: String,
    multiLine: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by rememberSaveable { mutableStateOf(value) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑$title") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = !multiLine,
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(text) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Text(title, style = MaterialTheme.typography.h6, modifier = Modifier.padding(16.dp, 12.dp))
    Card(Modifier.fillMaxWidth()) {
        Column { content() }
    }
}

@Composable
fun SettingsItem(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(null, LocalIndication.current) { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Spacer(Modifier.width(8.dp))
        Text(value, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
    }
}

class SettingsViewModel(private val settings: AppSettings) : ViewModel() {

    val config: StateFlow<AppConfig> =
        settings.config.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppConfig())

    fun updateUsername(name: String) = viewModelScope.launch {
        settings.update { it[Keys.USERNAME] = name }
    }

    fun updateJournalPrompt(prompt: String) = viewModelScope.launch {
        settings.update { it[Keys.JOURNAL_PROMPT] = prompt }
    }

    fun updateDefaultEndpoint(endpoint: String) = viewModelScope.launch {
        settings.update { it[Keys.DEFAULT_ENDPOINT] = endpoint }
    }

    fun updateDefaultApiKey(key: String) = viewModelScope.launch {
        settings.update { it[Keys.DEFAULT_API_KEY] = key }
    }

    fun updateDefaultModel(model: String) = viewModelScope.launch {
        settings.update { it[Keys.DEFAULT_MODEL] = model }
    }

    fun updateDefaultVisionModel(model: String) = viewModelScope.launch {
        settings.update { it[Keys.DEFAULT_VISION_MODEL] = model }
    }

    companion object {
        fun Factory(application: Application) = viewModelFactory {
            initializer {
                val app = application as MyApplication
                SettingsViewModel(settings = app.settings)
            }
        }
    }
}