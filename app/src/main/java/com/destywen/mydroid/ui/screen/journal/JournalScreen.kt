package com.destywen.mydroid.ui.screen.journal

import android.content.ClipData
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Chip
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldDefaults
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.destywen.mydroid.R
import com.destywen.mydroid.ui.components.BottomModal
import com.destywen.mydroid.util.timestampToLocalDateTimeString
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun JournalScreen(viewModel: JournalViewModel, onNavigate: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showEditor by rememberSaveable { mutableStateOf(false) }
    var showCommentSheet by rememberSaveable { mutableStateOf(false) }
    var activeJournalId by rememberSaveable { mutableStateOf<Int?>(null) }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showTagFilter by rememberSaveable { mutableStateOf(false) }

    var menuExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() } // snackbar so ugly
    val username = stringResource(R.string.username)

    val activeJournal = remember(activeJournalId, state.journals) {
        state.journals.find { it.id == activeJournalId }
    }
    val filteredJournals = remember(state.journals, searchQuery, state.hideTags) {
        (if (searchQuery.isBlank()) state.journals
        else state.journals.filter { j ->
            j.content.contains(searchQuery, ignoreCase = true) ||
                    j.comments.any { it.content.contains(searchQuery, ignoreCase = true) } ||
                    j.tags.any {it.contains(searchQuery, ignoreCase = true)}
        }).filter { j ->
            !j.tags.any { it in state.hideTags }
        }
    }

    LaunchedEffect(state.journals.size) {
        if (state.journals.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = AppBarDefaults.topAppBarWindowInsets,
                title = {
                    if (!isSearching) {
                        Text(stringResource(R.string.journal))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onNavigate() }) {
                        Icon(Icons.Default.Menu, null)
                    }
                },
                actions = {
                    if (isSearching) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("搜索内容或评论...") },
                            colors = TextFieldDefaults.textFieldColors(
                                backgroundColor = Color.Transparent
                            )
                        )
                    }
                    IconButton(onClick = { isSearching = !isSearching }) {
                        Icon(Icons.Default.Search, null)
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, null)
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(onClick = {
                            scope.launch { snackbarHostState.showSnackbar("clicked 设置") }
                        }) { Text("设置") }
                        DropdownMenuItem(onClick = {
                            showTagFilter = !showTagFilter; menuExpanded = false
                        }) { Text(if (showTagFilter) "收起" else "展开") }
                    }
                })
        },
        floatingActionButton = {
            if (!showEditor && !showCommentSheet) {
                FloatingActionButton(
                    onClick = {
                        activeJournalId = null
                        showEditor = true
                    }, modifier = Modifier.padding(24.dp)
                ) {
                    Icon(Icons.Default.Add, null)
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize()
        ) {
            AnimatedVisibility(visible = showTagFilter, enter = slideInVertically(), exit = slideOutVertically()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.tags.forEach { tag ->
                        val isSelected = tag in state.hideTags
                        Chip(
                            onClick = {
                                if (isSelected) {
                                    viewModel.showTag(tag)
                                } else {
                                    viewModel.hideTag(tag)
                                }
                            },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Filled.Lock, null) }
                            } else null
                        ) { Text(tag) }
                    }
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
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
        }

        // TODO: use more convenient way to locate journal by time
//        if (showDatePicker) {
//            DatePickerDialog(
//                onDismissRequest = { showDatePicker = false },
//                confirmButton = {
//                    TextButton(onClick = {
//                        showDatePicker = false
//                        menuExpanded = false
//                        datePickerState.selectedDateMillis?.let { targetTime ->
//                            val index = state.journals.indexOfFirst { it.timestamp <= targetTime }
//                            scope.launch {
//                                listState.animateScrollToItem(if (index >= 0) index else filteredJournals.size - 1)
//                            }
//                        }
//                    }) {
//                        Text("确认")
//                    }
//                }
//            ) {
//                DatePicker(state = datePickerState)
//            }
//        }

        BottomModal(showEditor, Modifier.padding(contentPadding), onDismissRequest = { showEditor = false }) {
            JournalEditorView(
                initialContent = activeJournal?.content ?: "",
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
                })
        }

        BottomModal(showCommentSheet, Modifier.padding(contentPadding), { showCommentSheet = false }) {
            CommentInputView { text ->
                activeJournalId?.let { viewModel.addComment(it, username, text) }
                showCommentSheet = false
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun JournalItemCard(item: Journal, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val clipboard = LocalClipboard.current.nativeClipboard
    var showMenu by remember { mutableStateOf(false) }

    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = { showMenu = true }),
        elevation = 2.dp
    ) {
        Column(Modifier.padding(8.dp)) {
            if (item.tags.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    item.tags.filter { it.isNotBlank() }.forEach {
                        Chip(onClick = {}, modifier = Modifier.height(32.dp)) { Text(it) }
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
                }, lineHeight = 20.sp)
            }

            Text(timestampToLocalDateTimeString(item.timestamp), fontSize = 12.sp)

            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(onClick = {
                    showMenu = false
                    clipboard.setPrimaryClip(ClipData.newPlainText("content", item.content))
                }) { Text("复制") }
                DropdownMenuItem(onClick = { showMenu = false; onEdit() }) { Text("编辑") }
                DropdownMenuItem(onClick = { showMenu = false; onDelete() }) { Text("删除") }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
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
                Chip(
                    onClick = {
                        selectedTags = if (isSelected) {
                            selectedTags.filter { it != tag }
                        } else {
                            selectedTags + tag
                        }
                    },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(Icons.Filled.Done, null)
                        }
                    } else {
                        null
                    }) { Text(tag) }
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
                        .width(160.dp)
                )
                Button(onClick = {
                    showCreate = false
                    selectableTags = selectableTags + newTag
                    selectedTags += newTag
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
        IconButton(
            enabled = content.isNotBlank(),
            onClick = { onSend(content) }) {
            Icon(Icons.AutoMirrored.Default.Send, null)
        }
    }
}
