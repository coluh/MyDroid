package com.destywen.mydroid.ui.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.destywen.mydroid.R
import com.destywen.mydroid.data.local.AgentSettings
import com.destywen.mydroid.data.local.AppDatabase
import com.destywen.mydroid.data.local.JournalSettings
import com.destywen.mydroid.data.remote.AiChatService
import com.destywen.mydroid.data.remote.NetworkModule
import com.destywen.mydroid.ui.screen.chat.ChatScreen
import com.destywen.mydroid.ui.screen.chat.ChatViewModel
import com.destywen.mydroid.ui.screen.home.HomeScreen
import com.destywen.mydroid.ui.screen.journal.JournalScreen
import com.destywen.mydroid.ui.screen.journal.JournalViewModel
import kotlinx.coroutines.launch

enum class Screen(@StringRes val label: Int, val icon: ImageVector) {
    HOME(R.string.home, Icons.Default.Home),
    JOURNAL(R.string.journal, Icons.Default.Create),
    SCHEDULE(R.string.schedule, Icons.Default.DateRange),
    CHAT(R.string.chat, Icons.Default.Person),
    GAMES(R.string.games, Icons.Default.Star),
    SETTINGS(R.string.settings, Icons.Default.Settings),
    INFO(R.string.info, Icons.Default.Info)
}

@Composable
fun MainApp(database: AppDatabase, agentSettings: AgentSettings, journalSettings: JournalSettings) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentScreen by rememberSaveable { mutableStateOf(Screen.HOME) }

    ModalNavigationDrawer(drawerState = drawerState, drawerContent = {
        ModalDrawerSheet(drawerShape = RectangleShape, modifier = Modifier.width(300.dp)) {
            Column(
                Modifier
                    .padding(horizontal = 16.dp)
                    .verticalScroll(
                        rememberScrollState()
                    )
            ) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.app_name),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                Screen.entries.forEach {
                    if (it == Screen.SETTINGS) {
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    }
                    NavigationDrawerItem(
                        icon = { Icon(it.icon, stringResource(it.label)) },
                        label = { Text(stringResource(it.label)) },
                        selected = it == currentScreen,
                        onClick = {
                            currentScreen = it
                            scope.launch { drawerState.close() }
                        },
                        shape = RectangleShape
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }) {
        val onNavigate: () -> Unit = { scope.launch { drawerState.open() } }
        when (currentScreen) {
            Screen.HOME -> HomeScreen(onNavigate)
            Screen.JOURNAL -> {
                val journalViewModel: JournalViewModel =
                    viewModel(factory = JournalViewModel.Factory(database.journalDao(), journalSettings))
                JournalScreen(journalViewModel, onNavigate)
            }

            Screen.CHAT -> {
                val viewModel: ChatViewModel =
                    viewModel(
                        factory = ChatViewModel.Factory(
                            AiChatService(NetworkModule.client),
                            database.chatDao(),
                            agentSettings
                        )
                    )
                ChatScreen(viewModel, onNavigate)
            }

            else -> HomeScreen(onNavigate)
        }
    }
}
