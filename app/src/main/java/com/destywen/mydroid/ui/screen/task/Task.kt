package com.destywen.mydroid.ui.screen.task

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.AlertDialog
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

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
        dao.insert(task.copy(createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
    }

    fun updateTask(task: TaskEntity) = viewModelScope.launch {
        dao.update(task.copy(updatedAt = System.currentTimeMillis()))
    }

    fun startTask(taskId: Long) = viewModelScope.launch {
        val task = dao.getTaskById(taskId) ?: return@launch
        dao.update(task.copy(status = TaskStatus.PROCESSING, updatedAt = System.currentTimeMillis()))
    }

    fun finishTask(taskId: Long) = viewModelScope.launch {
        val task = dao.getTaskById(taskId) ?: return@launch
        val actualMinutes = (System.currentTimeMillis() - task.updatedAt) / 1000 / 60
        dao.update(
            task.copy(
                status = TaskStatus.COMPLETED,
                actualMinutes = actualMinutes.toInt(),
                completedAt = System.currentTimeMillis()
            )
        )
    }

    fun revertTask(taskId: Long) = viewModelScope.launch {
        val task = dao.getTaskById(taskId) ?: return@launch
        dao.update(task.copy(status = TaskStatus.PENDING, updatedAt = System.currentTimeMillis()))
    }

    fun cancelTask(taskId: Long) = viewModelScope.launch {
        val task = dao.getTaskById(taskId) ?: return@launch
        dao.update(task.copy(status = TaskStatus.CANCELLED, updatedAt = System.currentTimeMillis()))
    }

    fun deleteTask(taskId: Long) = viewModelScope.launch {
        dao.deleteTaskById(taskId)
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

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TaskEntity?>(null) }
    var reusing by remember { mutableStateOf(false) }

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
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "label_0") {
                    if (uiState.processingTask.isEmpty()) {
                        Text("没有进行中的任务")
                    } else {
                        Text("进行中：")
                    }
                }
                items(uiState.processingTask, key = { it.id }) { task ->
                    TaskItem(
                        task,
                        modifier = Modifier.animateItem(),
                        onEdit = { editing = task; showAddTaskDialog = true },
                        onComplete = { viewModel.finishTask(it) },
                        onRevert = { viewModel.revertTask(it) },
                        onCancel = { viewModel.cancelTask(it) },
                    )
                }
                item(key = "divider_1") {
                    Divider(thickness = 2.dp)
                }
                items(uiState.pendingTasks, key = { it.id }) { task ->
                    TaskItem(
                        task,
                        modifier = Modifier.animateItem(),
                        onStart = { viewModel.startTask(it); scope.launch { listState.animateScrollToItem(0) } },
                        onEdit = { editing = task; showAddTaskDialog = true },
                        onCancel = { viewModel.cancelTask(it) },
                    )
                }
                item(key = "divider_2") {
                    Divider(thickness = 2.dp)
                    Spacer(Modifier.height(4.dp))
                    Text("已完成/已取消：")
                }
                items(uiState.completedTasks + uiState.cancelledTasks, key = { it.id }) { task ->
                    TaskItem(
                        task,
                        modifier = Modifier.animateItem(),
                        onEdit = { editing = task; showAddTaskDialog = true },
                        onReuse = { editing = task; reusing = true; showAddTaskDialog = true },
                        onDelete = { viewModel.deleteTask(it) },
                    )
                }
            }
        }

        if (showAddTaskDialog) {
            AddTaskDialog(
                onDismiss = { showAddTaskDialog = false; editing = null; reusing = false },
                onConfirm = {
                    if (editing == null) {
                        viewModel.addTask(it)
                    } else {
                        if (!reusing) {
                            viewModel.updateTask(it)
                        } else {
                            viewModel.addTask(it.copy(id = 0, status = TaskStatus.PENDING))
                            reusing = false
                        }
                        editing = null
                    }
                    showAddTaskDialog = false
                },
                editing = editing,
            )
        }
    }
}

@Composable
fun AddTaskDialog(onDismiss: () -> Unit, onConfirm: (TaskEntity) -> Unit, editing: TaskEntity? = null) {
    var title by remember { mutableStateOf(editing?.title ?: "") }
    var description by remember { mutableStateOf(editing?.description ?: "") }
    var category by remember { mutableStateOf(editing?.category ?: "") }
    var priority by remember { mutableFloatStateOf(editing?.priority?.toFloat() ?: 0f) }
    var estimatedMinutes by remember { mutableStateOf(editing?.estimatedMinutes) }
    var estimatedMinutesText by remember { mutableStateOf(editing?.estimatedMinutes?.toString() ?: "") }
    var energyLevel by remember { mutableIntStateOf(editing?.energyLevel ?: 2) }
    val energyLevels = mapOf(1 to "1 low", 2 to "2 medium", 3 to "3 high")
    var expandLevels by remember { mutableStateOf(false) }
    var dueDate by remember { mutableStateOf(editing?.dueDate) }
    // actualMinutes
    // completedAt

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing == null) "新建任务" else "编辑任务", style = MaterialTheme.typography.h5) },
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
//                    Slider(priority.toFloat(), { priority = it.roundToInt() }, valueRange = 0f..5f)
                    GradientSlider(
                        priority,
                        { priority = it },
                        modifier = Modifier.weight(1f),
                        valueRange = 0f..4f,
                        steps = 3,
                        colors = listOf(0, 30, 60, 90, 120).map { Color.hsl(it.toFloat(), 0.7f, 0.5f) }
                    )
                    Text("${priority.roundToInt()}", modifier = Modifier.width(20.dp))
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
                            id = editing?.id ?: 0,
                            title = title,
                            description = description.takeIf { it.isNotBlank() },
                            status = editing?.status ?: TaskStatus.PENDING,
                            priority = priority.roundToInt(),
                            category = category.takeIf { it.isNotBlank() },
                            estimatedMinutes = estimatedMinutes,
                            energyLevel = energyLevel,
                            dueDate = dueDate,
                            actualMinutes = editing?.actualMinutes,
                            completedAt = editing?.completedAt,
                            createdAt = editing?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = editing?.updatedAt ?: System.currentTimeMillis(),
                        )
                    )
                }
            }, enabled = title.isNotBlank()) { Text("确认") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun TaskItem(
    task: TaskEntity,
    modifier: Modifier = Modifier,
    onStart: ((Long) -> Unit)? = null,
    onEdit: (Long) -> Unit,
    onComplete: ((Long) -> Unit)? = null,
    onRevert: ((Long) -> Unit)? = null,
    onCancel: ((Long) -> Unit)? = null,
    onReuse: ((Long) -> Unit)? = null,
    onDelete: ((Long) -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        TaskCard(task, onClick = { expanded = !expanded })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (onStart != null) {
                DropdownMenuItem(onClick = {
                    onStart(task.id)
                    expanded = false
                }) { Text("开始") }
            }
            DropdownMenuItem(onClick = {
                onEdit(task.id)
                expanded = false
            }) { Text("编辑") }
            if (onComplete != null) {
                DropdownMenuItem(onClick = {
                    onComplete(task.id)
                    expanded = false
                }) { Text("完成") }
            }
            if (onRevert != null) {
                DropdownMenuItem(onClick = {
                    onRevert(task.id)
                    expanded = false
                }) { Text("撤回") }
            }
            if (onCancel != null) {
                DropdownMenuItem(onClick = {
                    onCancel(task.id)
                    expanded = false
                }) { Text("取消") }
            }
            if (onReuse != null) {
                DropdownMenuItem(onClick = {
                    onReuse(task.id)
                    expanded = false
                }) { Text("复用") }
            }
            if (onDelete != null) {
                DropdownMenuItem(onClick = {
                    onDelete(task.id)
                    expanded = false
                }) { Text("删除") }
            }
        }
    }
}

@Composable
fun TaskCard(task: TaskEntity, onClick: () -> Unit = {}) {
    val nonActiveStatuses = setOf(TaskStatus.COMPLETED, TaskStatus.CANCELLED)
    val borderColor = if (task.priority < 5) {
        Color.hsl(30f * task.priority, 0.7f, 0.5f)
    } else {
        Color.hsl(120f, 2.8f / task.priority, 0.5f)
    }.copy(alpha = if (task.status in nonActiveStatuses) 0.5f else 1f)
    val backgroundColor = MaterialTheme.colors.surface.copy(alpha = if (task.status in nonActiveStatuses) 0.5f else 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(null, LocalIndication.current) { onClick() },
        border = BorderStroke(2.dp, borderColor),
        backgroundColor = backgroundColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    task.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.h6,
                    textDecoration = TextDecoration.LineThrough.takeIf { task.status == TaskStatus.CANCELLED })
                Spacer(Modifier.width(4.dp))
                Text(
                    "⭐".repeat(task.energyLevel),
                    fontStyle = FontStyle.Italic,
                    textDecoration = TextDecoration.LineThrough.takeIf { task.status == TaskStatus.CANCELLED })
            }
            if (task.status == TaskStatus.PROCESSING && task.estimatedMinutes != null) {
                TimeProgressRow(task.updatedAt, task.estimatedMinutes)
            }
            task.description?.let {
                Text(it)
            }
            Divider()
            Row {
                task.estimatedMinutes?.let {
                    Text("预计需要${it}分钟")
                }
                if (task.status == TaskStatus.COMPLETED && task.actualMinutes != null) {
                    Spacer(Modifier.weight(1f))
                    Text((task.estimatedMinutes?.let { "实际" } ?: "") + "用时${task.actualMinutes}分钟")
                }
            }
            task.completedAt?.let {
                Row { Text("完成于" + it.toDateTimeString(), style = MaterialTheme.typography.body2) }
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

@Composable
fun GradientSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    colors: List<Color> = listOf(Color.Red, Color.Green)
) {
    val trackHeight = 24.dp
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        modifier = modifier.drawBehind {
            drawRoundRect(
                brush = Brush.horizontalGradient(colors),
                topLeft = Offset(0f, (size.height - trackHeight.toPx()) / 2),
                size = Size(size.width, trackHeight.toPx()),
                cornerRadius = CornerRadius(trackHeight.toPx() / 2),
            )
        },
        colors = SliderDefaults.colors(
            activeTrackColor = Color.Transparent,
            inactiveTrackColor = Color.Transparent,
            thumbColor = MaterialTheme.colors.primary
        ),
    )
}

@Composable
fun TimeProgressRow(startTime: Long, totalMinutes: Int) {
    var passedMinutes by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(startTime) {
        while (true) {
            passedMinutes = (System.currentTimeMillis() - startTime) / 1000f / 60f
            delay(1000.milliseconds)
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        LinearProgressIndicator(
            progress = (passedMinutes / totalMinutes).coerceIn(0f, 1f),
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(4.dp))
        Text("已开始${passedMinutes.toInt()}分钟")
    }
}