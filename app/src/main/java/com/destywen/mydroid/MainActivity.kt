package com.destywen.mydroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
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
import com.destywen.mydroid.ui.screen.home.HomeScreen
import com.destywen.mydroid.ui.theme.MyDroidTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyDroidTheme {
                App()
            }
        }
    }
}

@Composable
fun App() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    ModalNavigationDrawer(drawerState = drawerState, drawerContent = {
        ModalDrawerSheet(drawerShape = RectangleShape, modifier = Modifier.width(300.dp)) {
            Column(
                modifier = Modifier
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
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                AppDestinations.entries.forEach {
                    if (it.name == "SETTINGS") {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                    NavigationDrawerItem(
                        icon = { Icon(it.icon, stringResource(it.label)) },
                        label = { Text(stringResource(it.label)) },
                        selected = it == currentDestination,
                        onClick = {
                            currentDestination = it
                            scope.launch { drawerState.close() }
                        },
                        shape = RectangleShape
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }) {
        when (currentDestination) {
            AppDestinations.HOME -> HomeScreen(onNavigate = { scope.launch { drawerState.open() } })
            else -> HomeScreen(onNavigate = { scope.launch { drawerState.open() } })
        }
    }
}

enum class AppDestinations(@StringRes val label: Int, val icon: ImageVector) {
    HOME(R.string.home, Icons.Default.Home),
    SCHEDULE(R.string.schedule, Icons.Default.Create),
    GAMES(R.string.games, Icons.Default.Star),
    SETTINGS(R.string.settings, Icons.Default.Settings),
    INFO(R.string.info, Icons.Default.Info)
}