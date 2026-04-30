package com.destywen.mydroid.ui.screen.chat

import android.net.Uri
import android.os.Parcelable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.destywen.mydroid.R
import com.destywen.mydroid.data.local.UserEntity
import com.destywen.mydroid.util.toSmartTime
import kotlinx.parcelize.Parcelize
import java.io.File

@Parcelize
sealed class ChatModal : Parcelable {
    object None : ChatModal()
    object CreateUser : ChatModal()
    object UserList : ChatModal()
}

@Composable
fun ChatScreen(viewModel: ChatViewModel, onNavigateConv: (Long) -> Unit, onDrawer: () -> Unit) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val users by viewModel.users.collectAsStateWithLifecycle()
    val selfId by viewModel.selfId.collectAsStateWithLifecycle()
    val selfUser = remember(users, selfId) {
        users.find { it.id == selfId }
    }
    var activeModal by rememberSaveable { mutableStateOf<ChatModal>(ChatModal.None) }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = AppBarDefaults.topAppBarWindowInsets,
                title = { Text(stringResource(R.string.chat)) },
                navigationIcon = {
                    IconButton({ onDrawer() }) { Icon(Icons.Default.Menu, null) }
                },
                actions = {
                    var expanded by remember { mutableStateOf(false) }
                    selfUser?.let {
                        Avatar(it.avatar, it.name, 40)
                    }
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.Add, null)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(onClick = {
                            activeModal = ChatModal.UserList
                            expanded = false
                        }) { Text("登录") }
                        DropdownMenuItem(onClick = {
                            activeModal = ChatModal.CreateUser
                            expanded = false
                        }) { Text("创建好友") }
                        DropdownMenuItem(onClick = {
                            expanded = false
                        }) { Text("创建群聊") }
                        DropdownMenuItem(onClick = {
                            viewModel.clear()
                            expanded = false
                        }) { Text("clear") }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize()
                .animateContentSize()
        ) {
            items(conversations, key = { it.id }) { conv ->
                ConvListItem(conv.title, conv.avatar, conv.lastMessageTime, conv.lastMessagePreview, conv.unreadCount) {
                    onNavigateConv(conv.id)
                }
            }
        }

        when (val modal = activeModal) {
            is ChatModal.CreateUser -> CreateUserDialog(
                onDismiss = { activeModal = ChatModal.None },
                onConfirm = { name, avatarUri ->
                    viewModel.createUser(name, avatarUri)
                    activeModal = ChatModal.None
                })

            is ChatModal.UserList -> UserListDialog(
                users = users,
                onDismiss = { activeModal = ChatModal.None },
                onConfirm = { user ->
                    viewModel.setSelf(user.id)
                    activeModal = ChatModal.None
                },
            )

            else -> {}
        }
    }
}

@Composable
fun ConvListItem(
    title: String,
    avatar: String?,
    updateAt: Long,
    preview: String?,
    unreadCount: Int,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(null, LocalIndication.current) { onClick() }
            .background(MaterialTheme.colors.surface)
            .padding(10.dp)
    ) {
        Avatar(avatar, title)
        Spacer(Modifier.width(10.dp))
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title)
                Text(updateAt.toSmartTime(), style = MaterialTheme.typography.caption)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(preview ?: "")
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .background(Color.Red, RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$unreadCount", color = Color.White, fontSize = 12.sp, lineHeight = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun CreateUserDialog(onDismiss: () -> Unit, onConfirm: (name: String, avatarUri: Uri?) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建用户") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("")
                Row(
                    modifier = Modifier
                        .padding(bottom = 20.dp)
                        .size(80.dp)
                ) {
                    ImagePicker(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colors.background, CircleShape)
                            .clip(CircleShape),
                        imageUri = avatarUri,
                        onPick = { avatarUri = it }
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("用户名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, avatarUri) }, enabled = name.isNotBlank()) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        })
}

@Composable
fun UserListDialog(users: List<UserEntity>, onDismiss: () -> Unit, onConfirm: (UserEntity) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择账号") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                users.forEach {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(null, LocalIndication.current) {
                                onConfirm(it)
                            }) {
                        Avatar(it.avatar, it.name, 50)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(it.name, style = MaterialTheme.typography.h6)
                    }
                }
            }
        },
        buttons = {},
    )
}

@Composable
fun ImagePicker(modifier: Modifier = Modifier, imageUri: Uri?, onPick: (Uri) -> Unit) {
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onPick(it) }
    }

    Box(
        modifier = modifier
            .clickable(null, LocalIndication.current) {
                picker.launch("image/*")
            }, contentAlignment = Alignment.Center
    ) {
        if (imageUri != null) {
            AsyncImage(
                model = imageUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Column {
                Icon(Icons.Default.Add, null, Modifier.fillMaxSize(0.5f))
            }
        }
    }
}
