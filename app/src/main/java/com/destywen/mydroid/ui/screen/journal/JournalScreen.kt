package com.destywen.mydroid.ui.screen.journal

import android.content.ClipData
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.destywen.mydroid.R
import com.destywen.mydroid.util.timestampToLocalDateTimeString
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(viewModel: JournalViewModel, onNavigate: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showEditor by rememberSaveable { mutableStateOf(false) }
    var showCommentSheet by rememberSaveable { mutableStateOf(false) }
    var activeJournalId by rememberSaveable { mutableStateOf<Int?>(null) }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    var menuExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() } // snackbar so ugly

    val activeJournal = remember(activeJournalId, state.journals) {
        state.journals.find { it.id == activeJournalId }
    }
    val filteredJournals = remember(state.journals, searchQuery) {
        if (searchQuery.isBlank()) state.journals
        else state.journals.filter { j ->
            j.content.contains(searchQuery, ignoreCase = true) ||
                    j.comments.any { it.content.contains(searchQuery, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        TextField(value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("搜索内容或评论...") })
                    } else {
                        Text(stringResource(R.string.journal))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onNavigate() }) {
                        Icon(Icons.Default.Menu, null)
                    }
                },
                actions = {
                    IconButton(onClick = { isSearching = !isSearching }) {
                        Icon(Icons.Default.Search, null)
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, null)
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("设置") },
                            onClick = { scope.launch { snackbarHostState.showSnackbar("clicked 设置") } })
                        DropdownMenuItem(
                            text = { Text("跳转") },
                            onClick = { showDatePicker = true }
                        )
                    }
                })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    activeJournalId = null
                    showEditor = true
                }, modifier = Modifier.padding(24.dp)
            ) {
                Icon(Icons.Default.Add, null)
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { contentPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize()
        ) {
            items(filteredJournals, key = { it.id }) { journal ->
                JournalItemCard(journal, onClick = {
                    activeJournalId = journal.id
                    showCommentSheet = true
                }, onEdit = {
                    activeJournalId = journal.id
                    showEditor = true
                }, onDelete = {
                    viewModel.deleteJournal(journal.id)
                })
            }
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        showDatePicker = false
                        menuExpanded = false
                        datePickerState.selectedDateMillis?.let { targetTime ->
                            val index = state.journals.indexOfFirst { it.timestamp <= targetTime }
                            scope.launch {
                                listState.animateScrollToItem(if (index >= 0) index else filteredJournals.size - 1)
                            }
                        }
                    }) {
                        Text("确认")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (showEditor) {
            ModalBottomSheet(onDismissRequest = { showEditor = false }) {
                JournalEditorView(initialContent = activeJournal?.content ?: "",
                    initialTags = activeJournal?.tags ?: emptyList(),
                    allTags = state.tags,
                    onCancel = { showEditor = false },
                    onSave = { content, tags ->
                        showEditor = false
                        if (activeJournalId == null) {
                            viewModel.addJournal(content, tags)
                        } else {
                            viewModel.updateJournal(activeJournalId!!, content, tags)
                        }
                        scope.launch {
                            listState.animateScrollToItem(0)
                        }
                    })
            }
        }

        if (showCommentSheet) {
            ModalBottomSheet(onDismissRequest = { showCommentSheet = false }) {
                CommentInputView { text ->
                    activeJournalId?.let { viewModel.addComment(it, text) }
                    showCommentSheet = false
                }
            }
        }
    }
}

@Composable
fun JournalItemCard(item: Journal, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val clipboard = LocalClipboard.current.nativeClipboard
    var showMenu by remember { mutableStateOf(false) }

    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .combinedClickable(interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = { showMenu = true })
    ) {
        Column(Modifier.padding(8.dp)) {
            if (item.tags.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    item.tags.filter { it.isNotBlank() }.forEach {
                        SuggestionChip(onClick = {}, label = { Text(it) }, modifier = Modifier.height(32.dp))
                    }
                }
            }
            Text(item.content)
            item.comments.forEach {
                Text(buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp)) {
                        append(it.name + ": ")
                    }
                    withStyle(style = SpanStyle(fontStyle = FontStyle.Italic, fontSize = 14.sp)) {
                        append(it.content)
                    }
                }, lineHeight = 1.sp)
            }

            Text(timestampToLocalDateTimeString(item.timestamp), fontSize = 12.sp, lineHeight = 1.sp)

            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text("复制") }, onClick = {
                    showMenu = false
                    clipboard.setPrimaryClip(ClipData.newPlainText("content", item.content))
                })
                DropdownMenuItem(text = { Text("编辑") }, onClick = { showMenu = false; onEdit() })
                DropdownMenuItem(text = { Text("删除") }, onClick = { showMenu = false; onDelete() })
            }
        }
    }
}

@Composable
fun JournalEditorView(
    initialContent: String,
    initialTags: List<String>,
    allTags: List<String>,
    onCancel: () -> Unit,
    onSave: (content: String, tags: List<String>) -> Unit
) {
    var content by rememberSaveable { mutableStateOf(initialContent) }
    var selectableTags by rememberSaveable { mutableStateOf(allTags) }
    var selectedTags by rememberSaveable { mutableStateOf(initialTags) }
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var newTag by rememberSaveable { mutableStateOf("") }

    val isValid = content.isNotBlank()

    Column(
        Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Text(if (initialContent == "") "新建" else "编辑", fontSize = 22.sp)
        OutlinedTextField(
            content,
            onValueChange = { content = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            label = { Text("内容") })
        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(selectableTags) { tag ->
                val isSelected = tag in selectedTags
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedTags = if (isSelected) {
                            selectedTags.filter { it != tag }
                        } else {
                            selectedTags + tag
                        }
                    },
                    label = { Text(tag) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(Icons.Filled.Done, null)
                        }
                    } else {
                        null
                    })
            }
            item {
                IconButton(onClick = { showCreate = true }) {
                    Icon(Icons.Default.Add, null)
                }
            }
        }
        if (showCreate) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    newTag, onValueChange = { newTag = it }, modifier = Modifier
                        .width(100.dp)
                        .height(50.dp)
                )
                Button(onClick = {
                    showCreate = false
                    selectableTags = selectableTags + newTag
                }, enabled = newTag.isNotBlank()) {
                    Text("添加")
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { onCancel() }) { Text("取消") }
            Button(onClick = {
                if (isValid) {
                    onSave(content, selectedTags)
                }
            }, enabled = isValid) { Text("保存") }
        }
    }
}

@Composable
fun CommentInputView(onSend: (String) -> Unit) {
    var content by rememberSaveable { mutableStateOf("") }
    Row(
        Modifier
            .padding(16.dp)
            .fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = content,
            onValueChange = { content = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("添加评论...") })
        IconButton(onClick = { onSend(content) }) {
            Icon(Icons.AutoMirrored.Default.Send, null)
        }
    }
}
