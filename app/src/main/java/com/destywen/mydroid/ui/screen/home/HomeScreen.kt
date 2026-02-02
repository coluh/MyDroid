package com.destywen.mydroid.ui.screen.home

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
fun HomeScreen(onNavigate: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(
            windowInsets = AppBarDefaults.topAppBarWindowInsets,
            title = { Text(stringResource(R.string.home)) },
            navigationIcon = {
                IconButton(onClick = { onNavigate() }) {
                    Icon(Icons.Default.Menu, "Menu")
                }
            })
    }) { innerPadding ->
        Text(
            text = "Android", modifier = Modifier
                .padding(innerPadding)
                .padding(8.dp)
        )
    }
}