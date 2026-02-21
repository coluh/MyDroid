package com.destywen.mydroid.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// TODO: optimize
@Composable
fun EditableDropdown(
    modifier: Modifier = Modifier,
    options: List<String>,
    default: String = "",
    label: @Composable (() -> Unit)? = null,
    placeHolder: String = "",
    onValueChange: (text: String) -> Unit
) {
    var shouldExpand by rememberSaveable { mutableStateOf(false) }
    var input by rememberSaveable { mutableStateOf(default) }

    val filteredOptions = remember(input) {
        if (input.isEmpty()) {
            options
        } else {
            options.filter { it.contains(input, ignoreCase = true) }
        }
    }

    var wasFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

//    LaunchedEffect(shouldExpand) {
//        delay(10)
//        focusRequester.requestFocus()
//    }

    Box(modifier = modifier) {
        TextField(
            value = input,
            onValueChange = {
                input = it
                onValueChange(input)
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    when {
                        !wasFocused && focusState.isFocused -> {
                            shouldExpand = true
                        }

                        wasFocused && !focusState.isFocused -> {
                            shouldExpand = false
                        }
                    }
                },
            label = label,
            placeholder = { Text(placeHolder) },
            singleLine = true
        )

        DropdownMenu(
            expanded = shouldExpand,
            onDismissRequest = { },
            modifier = Modifier.focusable(false)
        ) {
            filteredOptions.forEach { option ->
                DropdownMenuItem(
                    onClick = {
                        input = option
                        onValueChange(input)
                    }
                ) { Text(option) }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun StringSelect(
    modifier: Modifier = Modifier,
    options: List<String>,
    default: String? = options.firstOrNull(),
    label: @Composable (() -> Unit)? = null,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selected by rememberSaveable { mutableStateOf(default) }

    LaunchedEffect(Unit) {
        default?.let {
            onValueChange(default)
        }
    }

    ExposedDropdownMenuBox(modifier = modifier, expanded = expanded, onExpandedChange = { expanded = it }) {
        TextField(
            value = selected ?: "",
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = label,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(
                backgroundColor = Color.Transparent,
            )
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    onClick = {
                        selected = option
                        expanded = false
                        onValueChange(option)
                    }
                ) { Text(option) }
            }
        }
    }
}

@Composable
fun BottomModal(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    if (visible) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            onDismissRequest()
                        }
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .imePadding()
                ) {
                    content()
                }
            }

            BackHandler() {
                onDismissRequest()
            }
        }
    }
}