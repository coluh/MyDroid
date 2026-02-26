package com.destywen.mydroid.ui.screen.journal

import android.content.ClipData
import android.net.Uri
import android.os.Parcelable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Chip
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.destywen.mydroid.R
import com.destywen.mydroid.data.local.ChatAgent
import com.destywen.mydroid.ui.components.AgentCard
import com.destywen.mydroid.ui.components.BottomModal
import com.destywen.mydroid.util.timestampToLocalDateTimeString
import kotlinx.parcelize.Parcelize
import java.io.File

@Parcelize
sealed class JournalModal : Parcelable {
    object None : JournalModal()
    object Setting : JournalModal()
    data class Editor(val id: Int, val content: String, val tags: List<String>) : JournalModal()
    data class Comment(val journalId: Int) : JournalModal()
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun JournalScreen(viewModel: JournalViewModel, onNavigate: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var activeModal by rememberSaveable { mutableStateOf<JournalModal>(JournalModal.None) }
    var query by rememberSaveable { mutableStateOf<String?>(null) }
    var showHideTags by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val username = stringResource(R.string.username)

    val filteredJournals = remember(state.journals, query) {
        if (query.isNullOrBlank()) {
            state.journals
        } else {
            state.journals.filter { j ->
                j.content.contains(query!!, ignoreCase = true) ||
                        j.comments.any { it.content.contains(query!!, ignoreCase = true) } ||
                        j.tags.any { it.contains(query!!, ignoreCase = true) }
            }
        }
    }
    LaunchedEffect(filteredJournals.size) {
        viewModel.updateStatus("找到${filteredJournals.size}条记录")
    }

    var shouldScroll by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.journals.size) {
        if (state.journals.isNotEmpty() && shouldScroll) {
            listState.animateScrollToItem(0)
            shouldScroll = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = AppBarDefaults.topAppBarWindowInsets,
                title = {
                    Text(
                        state.status ?: "",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onPrimary.copy(alpha = 0.7f),
                        modifier = Modifier.clickable(null, LocalIndication.current) {
                            viewModel.updateStatus()
                        })
                },
                navigationIcon = {
                    IconButton(onClick = { onNavigate() }) {
                        Icon(Icons.Default.Menu, null)
                    }
                },
                actions = {
                    var expanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { query = if (query == null) "" else null }) {
                        Icon(Icons.Default.Search, null)
                    }
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, null)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(onClick = {
                            activeModal = JournalModal.Setting
                            expanded = false
                        }) { Text("设置") }
                        DropdownMenuItem(onClick = {
                            showHideTags = !showHideTags
                            expanded = false
                        }) { Text(if (showHideTags) "收起" else "展开") }
                    }
                })
        },
        floatingActionButton = {
            if (activeModal == JournalModal.None) {
                FloatingActionButton(
                    onClick = {
                        activeModal = JournalModal.Editor(0, "", emptyList())
                        shouldScroll = true
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
            AnimatedVisibility(
                visible = query != null,
                enter = slideInHorizontally() + expandVertically(),
                exit = slideOutHorizontally() + shrinkVertically(),
            ) {
                TextField(
                    value = query ?: "",
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索任何文本...") },
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color.Transparent
                    )
                )
            }
            AnimatedVisibility(visible = showHideTags) {
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
                    .animateContentSize()
                    .fillMaxSize()
            ) {
                items(filteredJournals, key = { it.id }) { journal ->
                    JournalItemCard(
                        journal, modifier = Modifier.animateItem(),
                        onEdit = {
                            activeModal = JournalModal.Editor(journal.id, journal.content, journal.tags)
                        }, onGenerate = {
                            viewModel.generateReply(journal.id)
                        }, onDelete = {
                            viewModel.deleteJournal(journal.id)
                        }, onComment = {
                            activeModal = JournalModal.Comment(journal.id)
                        }, onDeleteComment = {
                            viewModel.deleteComment(it)
                        })
                }
            }
        }


        BottomModal(
            visible = activeModal != JournalModal.None,
            onDismissRequest = { activeModal = JournalModal.None }
        ) {
            when (val modal = activeModal) {
                is JournalModal.Editor -> JournalEditor(
                    initialContent = modal.content,
                    initialTags = modal.tags,
                    allTags = state.tags,
                    onCancel = { activeModal = JournalModal.None },
                    onSave = { content, tags, uri ->
                        if (modal.id == 0) {
                            viewModel.addJournal(content, tags, uri)
                        } else {
                            viewModel.updateJournal(modal.id, content, tags)
                        }
                        activeModal = JournalModal.None
                    })

                is JournalModal.Comment -> JournalComment(onSend = { text ->
                    viewModel.addUserComment(modal.journalId, username, text)
                    activeModal = JournalModal.None
                })

                is JournalModal.Setting -> JournalSetting(state.replyAgent, state.allAgents, onSelect = {
                    viewModel.selectReplyAgent(it)
                })

                else -> {}
            }
        }
    }
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun JournalItemCard(
    item: Journal,
    modifier: Modifier,
    onEdit: () -> Unit,
    onGenerate: () -> Unit,
    onDelete: () -> Unit,
    onComment: () -> Unit,
    onDeleteComment: (commentId: Int) -> Unit,
) {
    val context = LocalContext.current
    val imgDir = remember(context) { File(context.filesDir, "img") }
    val clipboard = LocalClipboard.current.nativeClipboard
    val copyText = { text: String ->
        clipboard.setPrimaryClip(ClipData.newPlainText("复制的文本", text))
    }
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = onComment,
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
            item.image?.let {
                AsyncImage(
                    model = File(imgDir, item.image),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item.comments.forEach {
                Box {
                    var expanded by remember { mutableStateOf(false) }
                    Text(
                        buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp)) {
                                append(it.name + ": ")
                            }
                            withStyle(style = SpanStyle(fontStyle = FontStyle.Italic, fontSize = 14.sp)) {
                                append(it.content)
                            }
                        }, lineHeight = 20.sp, modifier = Modifier.combinedClickable(
                            interactionSource = null,
                            indication = LocalIndication.current,
                            onClick = {},
                            onLongClick = { expanded = true }
                        ))
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(onClick = { expanded = false; copyText(it.content) }) { Text("复制") }
                        DropdownMenuItem(onClick = { expanded = false; onDeleteComment(it.id) }) { Text("删除") }
                    }
                }
            }

            Text(timestampToLocalDateTimeString(item.timestamp), fontSize = 12.sp)

            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(onClick = { showMenu = false; copyText(item.content) }) { Text("复制") }
                DropdownMenuItem(onClick = { showMenu = false; onEdit() }) { Text("编辑") }
                DropdownMenuItem(onClick = { showMenu = false; onGenerate() }) { Text("生成") }
                DropdownMenuItem(onClick = { showMenu = false; onDelete() }) { Text("删除") }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun JournalEditor(
    initialContent: String,
    initialTags: List<String>,
    allTags: List<String>,
    onCancel: () -> Unit,
    onSave: (content: String, tags: List<String>, imageUri: Uri?) -> Unit
) {
    var content by rememberSaveable { mutableStateOf(initialContent) }
    var selectableTags by rememberSaveable { mutableStateOf(allTags) }
    var selectedTags by rememberSaveable { mutableStateOf(initialTags) }
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var newTag by rememberSaveable { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedImageUri = uri
    }

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
        // image select and preview
        if (initialContent == "") {
            if (selectedImageUri == null) {
                Row {
                    IconButton(onClick = { launcher.launch("image/*") }) {
                        Icon(Icons.Default.Add, null)
                    }
                }
            } else {
                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = selectedImageUri, contentDescription = null, modifier = Modifier.size(80.dp))
                    IconButton(onClick = { selectedImageUri = null }) { Icon(Icons.Default.Delete, null) }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { onCancel() }) { Text("取消") }
            Button(onClick = {
                if (isValid) {
                    onSave(content, selectedTags, selectedImageUri)
                }
            }, enabled = isValid) { Text("保存") }
        }
    }
}

@Composable
fun JournalComment(onSend: (String) -> Unit) {
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun JournalSetting(agent: ChatAgent?, allAgents: List<ChatAgent>, onSelect: (id: String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded, onExpandedChange = { expanded = it }, modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        TextField(
            value = agent?.display() ?: "---",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true,
            modifier = Modifier.clickable(null, LocalIndication.current) {
                expanded = true
            }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            allAgents.forEach { agent ->
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        onSelect(agent.id)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    AgentCard(agent) {
                        onSelect(agent.id)
                    }
                }
            }
        }
    }
}
