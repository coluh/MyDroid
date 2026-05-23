package com.destywen.mydroid.ui.screen.schedule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.destywen.mydroid.MyApplication
import com.destywen.mydroid.R
import com.destywen.mydroid.data.local.ScheduleEntity
import com.destywen.mydroid.ui.components.BottomModal
import com.destywen.mydroid.ui.components.DateTimePickerButton
import com.destywen.mydroid.util.toShortTime

@Composable
fun ScheduleScreen(onNavigate: () -> Unit) {
    val app = LocalContext.current.applicationContext as MyApplication
    val viewModel: ScheduleViewModel = viewModel(factory = ScheduleViewModel.Factory(app))
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf<ScheduleEntity?>(null) }
    var expandAll by rememberSaveable { mutableStateOf(false) }

    val onCheck: (ScheduleEntity, Boolean) -> Unit = { schedule, checked ->
        viewModel.checkSchedule(schedule.id, checked)
    }
    val onEdit: (ScheduleEntity) -> Unit = { schedule ->
        editing = schedule
        showEditor = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = AppBarDefaults.topAppBarWindowInsets,
                title = { Text(stringResource(R.string.schedule)) },
                navigationIcon = {
                    IconButton(onClick = { onNavigate() }) {
                        Icon(Icons.Default.Menu, null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // expand all schedules
                        expandAll = !expandAll
                    }) {
                        if (!expandAll) {
                            Icon(Icons.Default.KeyboardArrowDown, null)
                        } else {
                            Icon(Icons.Default.KeyboardArrowUp, null)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                // create new schedule
                editing = null
                showEditor = true
            }, modifier = Modifier.padding(24.dp)) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
        ) {
            val groupBackground = MaterialTheme.colors.onSurface.copy(alpha = 0.05f)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                var lastGroup: String? = null
                state.schedules.forEach { schedule ->
                    if (schedule.groupName != null && schedule.groupName != lastGroup) {
                        item(key = "header_${schedule.groupName}") {
                            Spacer(Modifier.height(8.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .background(groupBackground)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    schedule.groupName,
                                    style = MaterialTheme.typography.body1,
                                )
                            }
                        }
                    }
                    item(key = schedule.id) {
                        if (schedule.groupName != null) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .background(groupBackground)
                                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                                    .animateItem()
                            ) {
                                ScheduleItem(schedule, Modifier, expandAll, onCheck, onEdit)
                            }
                        } else {
                            Spacer(Modifier.height(8.dp))
                            ScheduleItem(schedule, Modifier.animateItem(), expandAll, onCheck, onEdit)
                        }
                    }
                    lastGroup = schedule.groupName
                }
            }
        }

        BottomModal(showEditor, { showEditor = false }) {
            ScheduleEditor(
                initial = editing,
                onDismiss = { showEditor = false },
                onComplete = { title, desc, due, groupId ->
                    if (editing != null) {
                        viewModel.updateSchedule(editing!!.id, title, desc, due, groupId)
                    } else {
                        viewModel.addSchedule(title, desc, due, groupId)
                    }
                },
                onDelete = { editing?.let { viewModel.deleteSchedule(it.id) } }
            )
        }
    }
}

@Composable
fun ScheduleItem(
    schedule: ScheduleEntity,
    modifier: Modifier = Modifier,
    expandAll: Boolean,
    onCheck: (ScheduleEntity, Boolean) -> Unit,
    onEdit: (ScheduleEntity) -> Unit
) {
    var showDetail by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(null, LocalIndication.current, onLongClick = {
                onEdit(schedule)
            }) {
                showDetail = !showDetail
            }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(schedule.isCompleted, { onCheck(schedule, it) }, modifier = Modifier.size(64.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!schedule.isCompleted) {
                            Text(schedule.title)
                        } else {
                            Text(schedule.title, textDecoration = TextDecoration.LineThrough, color = Color.Gray)
                        }
                    }
                }

                schedule.due?.let {
                    Text(
                        schedule.due.toShortTime(),
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier
                            .clickable(null, LocalIndication.current) {
                                onEdit(schedule)
                            }
                            .padding(8.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = (showDetail || expandAll) && schedule.description != null,
                enter = expandVertically(animationSpec = tween(150))
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(64.dp))
                    Text(schedule.description ?: "")
                }
            }
        }
    }
}

@Composable
fun ScheduleEditor(
    initial: ScheduleEntity?,
    onDismiss: () -> Unit,
    onComplete: (String, String?, Long?, String?) -> Unit,
    onDelete: () -> Unit
) {
    var title by rememberSaveable { mutableStateOf(initial?.title ?: "") }
    var desc by rememberSaveable { mutableStateOf(initial?.description) }
    var due by rememberSaveable { mutableStateOf(initial?.due) }
    var groupName by rememberSaveable { mutableStateOf(initial?.groupName) }

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(title, { title = it }, label = { Text("标题") })
        OutlinedTextField(desc ?: "", { desc = it }, label = { Text("描述") })

        Row {
            DateTimePickerButton(due) { due = it }
            Spacer(Modifier.width(4.dp))
            TextButton({ due = null }) { Text("清除") }
        }

        OutlinedTextField(groupName ?: "", { groupName = it }, label = { Text("分组") })

        Row(Modifier.fillMaxWidth()) {
            TextButton({
                onDelete()
                onDismiss()
            }) { Text("删除", color = MaterialTheme.colors.error) }

            Spacer(Modifier.weight(1f))

            TextButton({ onDismiss() }) { Text("取消") }
            Button({
                onComplete(title, desc, due, groupName)
                onDismiss()
            }, enabled = title.isNotBlank()) { Text("完成") }
        }
    }
}
