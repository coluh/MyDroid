package com.destywen.mydroid.ui.screen.schedule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.FilterChip
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.destywen.mydroid.R
import com.destywen.mydroid.data.local.ScheduleEntity
import com.destywen.mydroid.data.local.ScheduleGroupEntity
import com.destywen.mydroid.ui.components.BottomModal
import com.destywen.mydroid.ui.components.DateTimePickerButton
import com.destywen.mydroid.util.toShortTime

@Composable
fun ScheduleScreen(viewModel: ScheduleViewModel, onNavigate: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf<ScheduleEntity?>(null) }
    var showAddGroupDialog by rememberSaveable { mutableStateOf(false) }

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
                .padding(8.dp),
        ) {
            GroupFilterRow(
                groups = state.groups,
                selectedGroupId = state.selectedGroupId,
                onSelectGroup = { viewModel.selectGroup(it) },
                onAddGroup = { showAddGroupDialog = true }
            )

            val displayItems = buildDisplayItems(state.schedules, state.groups, state.selectedGroupId)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(displayItems, key = { item ->
                    when (item) {
                        is DisplayItem.GroupHeader -> "g_${item.group.id}"
                        is DisplayItem.UngroupedHeader -> "g_null"
                        is DisplayItem.ScheduleItem -> item.schedule.id
                    }
                }) { item ->
                    when (item) {
                        is DisplayItem.GroupHeader -> GroupHeaderItem(item.group.name)
                        is DisplayItem.UngroupedHeader -> GroupHeaderItem("未分组")
                        is DisplayItem.ScheduleItem -> ScheduleItem(
                            item.schedule,
                            modifier = Modifier.animateItem(),
                            groupName = state.groups.find { it.id == item.schedule.groupId }?.name,
                            onCheck = { viewModel.checkSchedule(item.schedule.id, it) },
                            onEdit = {
                                editing = item.schedule
                                showEditor = true
                            }
                        )
                    }
                }
            }
        }

        if (showAddGroupDialog) {
            AddGroupDialog(
                onDismiss = { showAddGroupDialog = false },
                onConfirm = { name ->
                    viewModel.addGroup(name)
                    showAddGroupDialog = false
                }
            )
        }

        BottomModal(showEditor, { showEditor = false }) {
            ScheduleEditor(
                initial = editing,
                groups = state.groups,
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

private sealed class DisplayItem {
    data class GroupHeader(val group: ScheduleGroupEntity) : DisplayItem()
    data object UngroupedHeader : DisplayItem()
    data class ScheduleItem(val schedule: ScheduleEntity) : DisplayItem()
}

private fun buildDisplayItems(
    schedules: List<ScheduleEntity>,
    groups: List<ScheduleGroupEntity>,
    selectedGroupId: Long?,
): List<DisplayItem> {
    if (selectedGroupId != null) {
        return schedules.map { DisplayItem.ScheduleItem(it) }
    }

    val grouped = schedules.groupBy { it.groupId }
    val ungrouped = grouped[null].orEmpty()

    return buildList {
        for (group in groups) {
            val items = grouped[group.id]
            if (!items.isNullOrEmpty()) {
                add(DisplayItem.GroupHeader(group))
                items.forEach { add(DisplayItem.ScheduleItem(it)) }
            }
        }
        if (ungrouped.isNotEmpty()) {
            add(DisplayItem.UngroupedHeader)
            ungrouped.forEach { add(DisplayItem.ScheduleItem(it)) }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun GroupFilterRow(
    groups: List<ScheduleGroupEntity>,
    selectedGroupId: Long?,
    onSelectGroup: (Long?) -> Unit,
    onAddGroup: () -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedGroupId == null,
                onClick = { onSelectGroup(null) },
                content = { Text("全部") }
            )
        }
        items(groups, key = { it.id }) { group ->
            FilterChip(
                selected = selectedGroupId == group.id,
                onClick = { onSelectGroup(group.id) },
                content = { Text(group.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
        }
        item {
            IconButton(onClick = onAddGroup, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, "新建分组")
            }
        }
    }
}

@Composable
private fun GroupHeaderItem(name: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colors.primary.copy(alpha = 0.08f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Create,
                null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colors.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                name,
                style = MaterialTheme.typography.subtitle2,
                color = MaterialTheme.colors.primary
            )
        }
    }
}

@Composable
fun AddGroupDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("新建分组", style = MaterialTheme.typography.h6)
                OutlinedTextField(name, { name = it }, label = { Text("分组名称") }, singleLine = true)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("完成") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun GroupSelect(
    groups: List<ScheduleGroupEntity>,
    selectedGroupId: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = groups.find { it.id == selectedGroupId }?.name ?: "无"

    ExposedDropdownMenuBox(modifier = modifier, expanded = expanded, onExpandedChange = { expanded = it }) {
        TextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("分组") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(backgroundColor = Color.Transparent),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                onClick = {
                    expanded = false
                    onSelect(null)
                }
            ) { Text("无") }
            groups.forEach { group ->
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        onSelect(group.id)
                    }
                ) { Text(group.name) }
            }
        }
    }
}

@Composable
fun ScheduleItem(
    schedule: ScheduleEntity,
    modifier: Modifier = Modifier,
    groupName: String? = null,
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
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!schedule.isCompleted) {
                            Text(schedule.title)
                        } else {
                            Text(schedule.title, textDecoration = TextDecoration.LineThrough, color = Color.Gray)
                        }
                    }
                    if (groupName != null) {
                        Text(
                            groupName,
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.primary.copy(alpha = 0.7f)
                        )
                    }
                }

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
    groups: List<ScheduleGroupEntity> = emptyList(),
    onDismiss: () -> Unit,
    onComplete: (String, String?, Long?, Long?) -> Unit,
    onDelete: () -> Unit
) {
    var title by rememberSaveable { mutableStateOf(initial?.title ?: "") }
    var desc by rememberSaveable { mutableStateOf(initial?.description) }
    var due by rememberSaveable { mutableStateOf(initial?.due) }
    var groupId by rememberSaveable { mutableStateOf(initial?.groupId) }

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (groups.isNotEmpty()) {
            GroupSelect(groups, groupId, { groupId = it }, Modifier.fillMaxWidth())
        }
        OutlinedTextField(title, { title = it }, label = { Text("标题") })
        OutlinedTextField(desc ?: "", { desc = it }, label = { Text("描述") })

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
                onComplete(title, desc, due, groupId)
                onDismiss()
            }, enabled = title.isNotBlank()) { Text("完成") }
        }
    }
}
