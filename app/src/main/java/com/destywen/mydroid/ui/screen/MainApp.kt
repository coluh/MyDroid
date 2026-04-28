package com.destywen.mydroid.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.destywen.mydroid.AppContainer
import com.destywen.mydroid.R
import com.destywen.mydroid.ui.screen.chat.ChatScreen
import com.destywen.mydroid.ui.screen.chat.ChatViewModel
import com.destywen.mydroid.ui.screen.chat.ConversationScreen
import com.destywen.mydroid.ui.screen.home.HomeScreen
import com.destywen.mydroid.ui.screen.journal.JournalScreen
import com.destywen.mydroid.ui.screen.journal.JournalViewModel
import com.destywen.mydroid.ui.screen.log.LogScreen
import com.destywen.mydroid.ui.screen.log.LogViewModel
import com.destywen.mydroid.ui.screen.schedule.ScheduleScreen
import com.destywen.mydroid.ui.screen.schedule.ScheduleViewModel
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Journal : Screen("journal")
    object Schedule : Screen("schedule")
    object Chat : Screen("chat")
    object Games : Screen("games")
    object Settings : Screen("settings")
    object Log : Screen("log")
    object Conversation : Screen("chat/{convId}") {
        fun passArgs(convId: Long): String = "chat/${convId}"
    }
}

enum class DrawerItem(val label: Int, val icon: ImageVector, val screen: Screen) {
    HOME(R.string.home, Icons.Default.Home, Screen.Home),
    JOURNAL(R.string.journal, Icons.Default.Create, Screen.Journal),
    SCHEDULE(R.string.schedule, Icons.Default.DateRange, Screen.Schedule),
    CHAT(R.string.chat, Icons.Default.Person, Screen.Chat),
    GAMES(R.string.games, Icons.Default.Star, Screen.Games),
    SETTINGS(R.string.settings, Icons.Default.Settings, Screen.Settings),
    LOG(R.string.log, Icons.Default.Info, Screen.Log),
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MainApp(container: AppContainer) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()

    val currentDestination = navController.currentBackStackEntryAsState().value?.destination?.route
    val currentDrawerItem = DrawerItem.entries.find { it.screen.route == currentDestination }

    ModalDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(currentDrawerItem = currentDrawerItem) { item ->
                navController.navigate(item.screen.route) {
                    popUpTo(Screen.Home.route) { saveState = true }
                    launchSingleTop = true
                }
                scope.launch { drawerState.close() }
            }
        }
    ) {
        NavHost(navController = navController, startDestination = Screen.Home.route) {
            composable(Screen.Home.route) {
                HomeScreen { scope.launch { drawerState.open() } }
            }
            composable(Screen.Journal.route) {
                val journalViewModel: JournalViewModel = viewModel(factory = JournalViewModel.Factory(container))
                JournalScreen(journalViewModel) { scope.launch { drawerState.open() } }
            }
            composable(Screen.Schedule.route) {
                val viewModel: ScheduleViewModel = viewModel(factory = ScheduleViewModel.Factory(container))
                ScheduleScreen(viewModel) { scope.launch { drawerState.open() } }
            }
            composable(Screen.Chat.route) {
                val viewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory(container))
                ChatScreen(viewModel, {
                    navController.navigate(Screen.Conversation.passArgs(it))
                }) { scope.launch { drawerState.open() } }
            }
            composable(Screen.Games.route) {
                HomeScreen { scope.launch { drawerState.open() } }
            }
            composable(Screen.Settings.route) {
                HomeScreen { scope.launch { drawerState.open() } }
            }
            composable(Screen.Log.route) {
                val viewModel: LogViewModel = viewModel(factory = LogViewModel.Factory(container))
                LogScreen(viewModel) { scope.launch { drawerState.open() } }
            }

            composable(
                route = Screen.Conversation.route,
                arguments = listOf(navArgument("convId") { type = NavType.LongType })
            ) {
                val convId = it.arguments?.getLong("convId") ?: 0L
                ConversationScreen(convId)
            }
        }
    }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }
}

@Composable
private fun DrawerContent(
    currentDrawerItem: DrawerItem?,
    onItemClick: (DrawerItem) -> Unit
) {
    Column(
        Modifier
            .systemBarsPadding()
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            stringResource(R.string.app_name),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.h5
        )
        Divider(Modifier.padding(vertical = 8.dp))

        DrawerItem.entries.forEach { item ->
            if (item == DrawerItem.SETTINGS) {
                Divider(Modifier.padding(vertical = 8.dp))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(null, LocalIndication.current) {
                        onItemClick(item)
                    }
                    .background(
                        if (item == currentDrawerItem) MaterialTheme.colors.primary.copy(alpha = 0.2f)
                        else Color.Transparent
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(item.icon, stringResource(item.label))
                Spacer(Modifier.width(16.dp))
                Text(stringResource(item.label))
            }
        }
    }
}