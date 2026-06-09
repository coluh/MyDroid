package com.destywen.mydroid.ui.screen.task

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Button
import androidx.compose.material.Card
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
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.destywen.mydroid.MyApplication
import com.destywen.mydroid.data.local.TaskDao
import com.destywen.mydroid.data.local.TaskEntity
import com.destywen.mydroid.data.local.TaskStatus
import com.destywen.mydroid.ui.components.ClickTextField
import com.destywen.mydroid.ui.components.DateTimePickerButton
import com.destywen.mydroid.util.toDateTimeString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class UiState(
    val processingTask: List<TaskEntity> = emptyList(),
    val pendingTasks: List<TaskEntity> = emptyList(),
    val completedTasks: List<TaskEntity> = emptyList(),
    val cancelledTasks: List<TaskEntity> = emptyList(),
    val error: String? = null,
)

class TaskViewModel(private val dao: TaskDao) : ViewModel() {

    private val _tasks = dao.getAllTasks()
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<UiState> = combine(_tasks, _error) { tasks, error ->
        val taskGroup = tasks.groupBy { it.status }
        UiState(
            processingTask = taskGroup[TaskStatus.PROCESSING] ?: emptyList(),
            pendingTasks = taskGroup[TaskStatus.PENDING] ?: emptyList(),
            completedTasks = taskGroup[TaskStatus.COMPLETED] ?: emptyList(),
            cancelledTasks = taskGroup[TaskStatus.CANCELLED] ?: emptyList(),
            error = error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    fun clearError() = _error.update { null }

    fun addTask(task: TaskEntity) = viewModelScope.launch {
        dao.insert(task)
    }

    companion object {
        fun Factory(application: Application) = viewModelFactory {
            initializer {
                val app = application as MyApplication
                TaskViewModel(app.database.taskDao())
            }
        }
    }
}

@Composable
fun TaskScreen(onNavigate: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as MyApplication
    val viewModel: TaskViewModel = viewModel(factory = TaskViewModel.Factory(app))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showAddTaskDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar("Error: $it")
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = AppBarDefaults.topAppBarWindowInsets,
                title = { Text("任务") },
                navigationIcon = { IconButton({ onNavigate() }) { Icon(Icons.AutoMirrored.Filled.List, null) } },
                actions = {
                    IconButton(onClick = { showAddTaskDialog = true }) { Icon(Icons.Default.Add, null) }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { contentPadding ->
        Column(
            Modifier
                .padding(contentPadding)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.pendingTasks, key = { it.id }) { task ->
                    TaskItem(task)
                }
            }
        }

        if (showAddTaskDialog) {
            AddTaskDialog(
                onDismiss = { showAddTaskDialog = false },
                onConfirm = { viewModel.addTask(it); showAddTaskDialog = false },
            )
        }
    }
}

@Composable
fun AddTaskDialog(onDismiss: () -> Unit, onConfirm: (TaskEntity) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var priority by remember { mutableIntStateOf(0) }
    var estimatedMinutes by remember { mutableStateOf<Int?>(null) }
    var estimatedMinutesText by remember { mutableStateOf("") }
    var energyLevel by remember { mutableIntStateOf(2) }
    val energyLevels = mapOf(1 to "1 low", 2 to "2 medium", 3 to "3 high")
    var expandLevels by remember { mutableStateOf(false) }
    var dueDate by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建任务", style = MaterialTheme.typography.h5) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("", style = MaterialTheme.typography.overline, fontSize = 1.sp, lineHeight = 1.sp)
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("标题") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("描述") })
                ClickTextField(
                    category,
                    { category = it },
                    modifier = Modifier.size(120.dp, 40.dp),
                    label = { Text("设置分类") })
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("优先级")
                    Slider(priority.toFloat(), { priority = it.roundToInt() }, valueRange = 0f..5f)
                    Text("$priority")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        estimatedMinutesText,
                        { estimatedMinutesText = it; estimatedMinutes = it.toIntOrNull() },
                        modifier = Modifier.weight(1f),
                        label = { Text("预估耗时/min") }
                    )
                    Spacer(Modifier.width(4.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            "${energyLevels[energyLevel]}",
                            {},
                            readOnly = true,
                            label = { Text("所需精力水平") },
                            trailingIcon = {
                                IconButton(onClick = { expandLevels = true }) {
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        null
                                    )
                                }
                            },
                        )
                        DropdownMenu(expanded = expandLevels, onDismissRequest = { expandLevels = false }) {
                            energyLevels.forEach { (level, comment) ->
                                DropdownMenuItem(onClick = {
                                    energyLevel = level
                                    expandLevels = false
                                }) { Text(comment) }
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("截止时间：")
                    DateTimePickerButton(dueDate) { dueDate = it }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank()) {
                    onConfirm(
                        TaskEntity(
                            title = title,
                            description = description.takeIf { it.isNotBlank() },
                            priority = priority,
                            category = category.takeIf { it.isNotBlank() },
                            estimatedMinutes = estimatedMinutes,
                            energyLevel = energyLevel,
                            dueDate = dueDate,
                        )
                    )
                }
            }, enabled = title.isNotBlank()) { Text("确认") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun TaskItem(task: TaskEntity) {
    val borderColor = if (task.priority < 5) {
        Color.hsl(30f * task.priority, 1f, 0.5f)
    } else {
        Color.hsl(120f, 4.0f / task.priority, 0.5f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        border = BorderStroke(2.dp, borderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(task.title, style = MaterialTheme.typography.h6)
                Spacer(Modifier.width(4.dp))
                Text("⭐".repeat(task.energyLevel), fontStyle = FontStyle.Italic)
                Spacer(Modifier.weight(1f))
                task.estimatedMinutes?.let {
                    Text("预计需要${it}分钟")
                }
            }
            task.description?.let {
                Text(it)
            }
            Row {
                Text(task.updatedAt.toDateTimeString(), style = MaterialTheme.typography.body2)
                Spacer(Modifier.weight(1f))
                task.category?.let {
                    Text(it, fontStyle = FontStyle.Italic)
                }
            }
        }
    }
}