package com.destywen.mydroid.ui.screen.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.destywen.mydroid.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigate: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.home)) },
            navigationIcon = {
                IconButton(onClick = { onNavigate() }) {
                    Icon(Icons.Default.Menu, "Menu")
                }
            })
    }) { innerPadding ->
        Text(
            text = "Android", modifier = Modifier.padding(innerPadding)
        )
    }
}