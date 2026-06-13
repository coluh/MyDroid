package com.destywen.mydroid.ui.screen.home

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.destywen.mydroid.R
import kotlin.math.absoluteValue

private enum class Target(val route: String, val title: String, val icon: ImageVector) {
    Journal("journal", "随笔", Icons.Default.Create),
    Schedule("schedule", "日程", Icons.Default.DateRange),
    Chat("chat", "聊天", Icons.Default.Person),
    Task("task", "任务", Icons.AutoMirrored.Filled.List),
}

@Composable
fun HomeScreen(onOpenDrawer: () -> Unit, onNavigateTo: (String) -> Unit) {
    Scaffold(topBar = {
        TopAppBar(
            windowInsets = AppBarDefaults.topAppBarWindowInsets,
            title = { Text(stringResource(R.string.home)) },
            navigationIcon = {
                IconButton(onClick = { onOpenDrawer() }) {
                    Icon(Icons.Default.Menu, "Menu")
                }
            })
    }) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Target.entries.forEach {
                val hue = it.title.hashCode().absoluteValue % 360
                NavCard(it.icon, it.title, Color.hsl(hue.toFloat(), 0.7f, 0.8f), modifier = Modifier.weight(1f)) {
                    onNavigateTo(it.route)
                }
            }
        }
    }
}

@Composable
fun NavCard(icon: ImageVector, title: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(backgroundColor = color, modifier = modifier.clickable(null, LocalIndication.current) {
        onClick()
    }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(icon, null)
            Spacer(Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.h5)
        }
    }
}