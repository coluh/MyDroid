package com.destywen.mydroid.ui.screen.schedule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.destywen.mydroid.R
import com.destywen.mydroid.data.local.ScheduleEntity
import com.destywen.mydroid.ui.components.BottomModal
import com.destywen.mydroid.ui.components.DateTimePickerButton
import com.destywen.mydroid.util.toShortTime

@Composable
fun ScheduleScreen(viewModel: ScheduleViewModel, onNavigate: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf<ScheduleEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = AppBarDefaults.topAppBarWindowInsets,
                title = { Text(stringResource(R.string.schedule)) },
                navigationIcon = {
                    IconButton(onClick = { onNavigate() }) {
                        Icon(Icons.Default.Menu, null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                // add schedule
                editing = null
                showEditor = true
            }, modifier = Modifier.padding(24.dp)) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.schedules, key = { it.id }) { item ->
                ScheduleItem(
                    item,
                    modifier = Modifier.animateItem(),
                    onCheck = { viewModel.checkSchedule(item.id, it) },
                    onEdit = {
                        editing = item
                        showEditor = true
                    })
            }
        }

        BottomModal(showEditor, { showEditor = false }) {
            ScheduleEditor(editing, { showEditor = false }, { title, desc, due ->
                if (editing != null) {
                    viewModel.updateSchedule(editing!!.id, title, desc, due)
                } else {
                    viewModel.addSchedule(title, desc, due)
                }
            }, { editing?.let { viewModel.deleteSchedule(it.id) } })
        }
    }
}

@Composable
fun ScheduleItem(
    schedule: ScheduleEntity,
    modifier: Modifier = Modifier,
    onCheck: (Boolean) -> Unit,
    onEdit: () -> Unit
) {
    var showDetail by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(null, LocalIndication.current, onLongClick = {
                onEdit()
            }) {
                showDetail = !showDetail
            }
    ) {
        Column(Modifier
            .fillMaxWidth()
            .padding(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(schedule.isCompleted, { onCheck(it) }, modifier = Modifier.size(64.dp))
                if (!schedule.isCompleted) {
                    Text(schedule.title)
                } else {
                    Text(schedule.title, textDecoration = TextDecoration.LineThrough, color = Color.Gray)
                }

                Spacer(Modifier.weight(1f))

                schedule.due?.let {
                    Text(
                        schedule.due.toShortTime(),
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier
                            .clickable(null, LocalIndication.current) {
                                onEdit()
                            }
                            .padding(8.dp)
                    )
                }
            }

            AnimatedVisibility(visible = showDetail && schedule.description != null) {
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
    onComplete: (String, String?, Long?) -> Unit,
    onDelete: () -> Unit
) {
    var title by rememberSaveable { mutableStateOf(initial?.title ?: "") }
    var desc by rememberSaveable { mutableStateOf(initial?.description) }
    var due by rememberSaveable { mutableStateOf(initial?.due) }

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(title, { title = it }, label = { Text("标题") })
        OutlinedTextField(desc ?: "", { desc = it }, label = { Text("描述") })

        // date picker
        Row {
            DateTimePickerButton(due, { due = it })
            Spacer(Modifier.width(4.dp))
            TextButton({ due = null }) { Text("清除") }
        }

        Row(Modifier.fillMaxWidth()) {
            TextButton({
                onDelete()
                onDismiss()
            }) { Text("删除", color = MaterialTheme.colors.error) }

            Spacer(Modifier.weight(1f))

            TextButton({ onDismiss() }) { Text("取消") }
            Button({
                onComplete(title, desc, due)
                onDismiss()
            }, enabled = title.isNotBlank()) { Text("完成") }
        }
    }
}
