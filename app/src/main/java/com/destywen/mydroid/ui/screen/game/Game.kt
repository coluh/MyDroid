package com.destywen.mydroid.ui.screen.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.destywen.mydroid.R


@Composable
fun GameScreen(onNavigate: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(
            windowInsets = AppBarDefaults.topAppBarWindowInsets,
            title = { Text(stringResource(R.string.games)) },
            navigationIcon = {
                IconButton(onClick = { onNavigate() }) {
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
            Text("hello")
        }
    }
}
