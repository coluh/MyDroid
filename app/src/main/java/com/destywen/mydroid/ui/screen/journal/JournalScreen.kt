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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.destywen.mydroid.util.toLocalDateTimeString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(viewModel: JournalViewModel, onNavigate: () -> Unit) {
    val journals by viewModel.items.collectAsStateWithLifecycle()

    var showCommentSheet by rememberSaveable { mutableStateOf(false) }
    var activeJournalId by rememberSaveable { mutableStateOf<Int?>(null) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingJournalId by rememberSaveable { mutableStateOf<Int?>(null) }
    val editingJournal = remember(editingJournalId, journals) {
        journals.find { it.id == editingJournalId }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.journal)) }, navigationIcon = {
            IconButton(onClick = { onNavigate() }) {
                Icon(Icons.Default.Menu, "Menu")
            }
        })
    }, floatingActionButton = {
        FloatingActionButton(
            onClick = {
                editingJournalId = null
                showEditor = true
            }, modifier = Modifier.padding(24.dp)
        ) {
            Icon(Icons.Default.Add, "Add")
        }
    }) { innerPadding ->
        LazyColumn(
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            items(journals, key = { it.id }) { item ->
                JournalItemCard(item, onClick = {
                    activeJournalId = item.id
                    showCommentSheet = true
                }, onEdit = {
                    editingJournalId = item.id
                    showEditor = true
                }, onDelete = {
                    viewModel.deleteJournal(item.id)
                })
            }
        }

        if (showEditor) {
            ModalBottomSheet(onDismissRequest = { showEditor = false }) {
                JournalEditorView(initialContent = editingJournal?.content ?: "",
                    initialTag = editingJournal?.tag ?: "",
                    onCancel = { showEditor = false },
                    onSave = { content, tag ->
                        if (editingJournalId == null) {
                            viewModel.addJournal(content, tag)
                        } else {
                            viewModel.updateJournal(editingJournalId!!, content, tag)
                        }
                        showEditor = false
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
        Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                item.tag.split(",").forEach {
                    Text(it, fontSize = 14.sp)
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
                })
            }
            Text(item.timestamp.toLocalDateTimeString(), fontSize = 14.sp)

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
    initialContent: String, initialTag: String, onCancel: () -> Unit, onSave: (content: String, tag: String) -> Unit
) {
    var content by rememberSaveable { mutableStateOf(initialContent) }
    var tag by rememberSaveable { mutableStateOf(initialTag) }
    val isValid = content.isNotBlank() && tag.isNotBlank()

    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(if (initialContent == "") "新建" else "编辑", fontSize = 22.sp)
        OutlinedTextField(
            content,
            onValueChange = { content = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            label = { Text("内容") })
        // TODO: select
        OutlinedTextField(tag, onValueChange = { tag = it }, label = { Text("标签") })
        Row(
            Modifier
                .fillMaxWidth(), horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { onCancel() }) { Text("取消") }
            Button(onClick = {
                if (isValid) {
                    onSave(content, tag)
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
            .fillMaxWidth()
            .padding(16.dp), verticalAlignment = Alignment.CenterVertically
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