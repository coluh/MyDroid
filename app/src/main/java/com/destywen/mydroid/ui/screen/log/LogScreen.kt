package com.destywen.mydroid.ui.screen.log

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Divider
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.destywen.mydroid.R
import com.destywen.mydroid.data.local.LogEntity
import com.destywen.mydroid.data.local.LogLevel
import com.destywen.mydroid.util.timestampToLocalDateTimeString


@Composable
fun LogScreen(viewModel: LogViewModel, onNavigate: () -> Unit) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = AppBarDefaults.topAppBarWindowInsets,
                title = { Text(stringResource(R.string.log)) },
                navigationIcon = {
                    IconButton(onClick = { onNavigate() }) {
                        Icon(Icons.Default.Menu, null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.deleteDebugLog()
            }, modifier = Modifier.padding(24.dp)) {
                Icon(Icons.Default.Delete, null)
            }
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize(),
            reverseLayout = true
        ) {
            item { Spacer(Modifier.height(200.dp)) }
            items(logs) {
                LogItem(it)
                Divider(Modifier.alpha(0.5f))
            }
        }
    }
}

@Composable
fun LogItem(log: LogEntity) {
    val color = when (log.level) {
        LogLevel.ERROR.name -> Color.Red
        LogLevel.WARN.name -> Color(0xFFF59E0B)
        LogLevel.INFO.name -> Color.Blue
        LogLevel.DEBUG.name -> Color.Gray
        else -> Color.Black
    }

    Column(Modifier.padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = timestampToLocalDateTimeString(log.timestamp),
                color = Color.Gray,
                style = MaterialTheme.typography.caption
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = log.level,
                color = color,
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            Text(text = "[${log.tag}]", style = MaterialTheme.typography.caption)
        }
        Text(text = log.message, style = MaterialTheme.typography.body2)
    }
}