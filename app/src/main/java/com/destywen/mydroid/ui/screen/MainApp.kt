package com.destywen.mydroid.ui.screen

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Divider
import androidx.compose.material.DrawerValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalDrawer
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

enum class Screen(val label: Int, val icon: ImageVector) {
    HOME(R.string.home, Icons.Default.Home),
    JOURNAL(R.string.journal, Icons.Default.Create),
    SCHEDULE(R.string.schedule, Icons.Default.DateRange),
    CHAT(R.string.chat, Icons.Default.Person),
    GAMES(R.string.games, Icons.Default.Star),
    SETTINGS(R.string.settings, Icons.Default.Settings),
    INFO(R.string.info, Icons.Default.Info)
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MainApp(database: AppDatabase, agentSettings: AgentSettings, journalSettings: JournalSettings) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentScreen by rememberSaveable { mutableStateOf(Screen.HOME) }

    ModalDrawer(
        drawerState = drawerState,
        drawerContent = {
            Column(
                Modifier
                    .systemBarsPadding()
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(
                        rememberScrollState()
                    )
            ) {
                Text(
                    stringResource(R.string.app_name),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.h5
                )
                Divider(Modifier.padding(vertical = 8.dp))

                Screen.entries.forEach {
                    if (it == Screen.SETTINGS) {
                        Divider(Modifier.padding(vertical = 8.dp))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(null, LocalIndication.current) {
                                currentScreen = it
                                scope.launch { drawerState.close() }
                            }
                            .background(
                                if (it == currentScreen) MaterialTheme.colors.primary.copy(alpha = 0.2f)
                                else Color.Transparent
                            )
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(it.icon, stringResource(it.label))
                        Spacer(Modifier.width(16.dp))
                        Text(stringResource(it.label))
                    }
                }
            }
        }
    ) {
        when (currentScreen) {
            Screen.JOURNAL -> {
                val journalViewModel: JournalViewModel =
                    viewModel(factory = JournalViewModel.Factory(database.journalDao(), journalSettings))
                JournalScreen(journalViewModel) { scope.launch { drawerState.open() } }
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
                ChatScreen(viewModel) { scope.launch { drawerState.open() } }
            }

            else -> HomeScreen() { scope.launch { drawerState.open() } }
        }
    }
}
