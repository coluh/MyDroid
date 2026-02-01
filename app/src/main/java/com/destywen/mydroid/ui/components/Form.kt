package com.destywen.mydroid.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> Dropdown(
    modifier: Modifier = Modifier,
    options: List<T>,
    toText: (T) -> String,
    selected: T? = null,
    label: @Composable (() -> Unit)? = null,
    onSelect: (T) -> Unit
) {
    var shouldExpand by remember { mutableStateOf(false) }

    val expanded = shouldExpand && options.isNotEmpty()
    val showText = if (selected == null) "-- - --" else toText(selected)

    ExposedDropdownMenuBox(modifier = modifier, expanded = expanded, onExpandedChange = { shouldExpand = it }) {
        TextField(
            value = showText,
            onValueChange = {},
            readOnly = true,
            textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold),
            label = label,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(),
            colors = ExposedDropdownMenuDefaults.textFieldColors(
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
            ),
            singleLine = true
        )

        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { shouldExpand = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(toText(option)) },
                    onClick = {
                        shouldExpand = false
                        onSelect(option)
                    }
                )
            }
        }
    }
}

// TODO: optimize
@OptIn(ExperimentalMaterial3Api::class)
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
                    text = { Text(option) },
                    onClick = {
                        input = option
                        onValueChange(input)
                    }
                )
            }
        }
    }
}

@Composable
fun BottomModal(
    visible: Boolean,
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = modifier
                .imePadding()
                .fillMaxSize()
                .animateEnterExit(enter = fadeIn(), exit = fadeOut())
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(interactionSource = null, indication = LocalIndication.current) {
                    onDismissRequest()
                }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .animateEnterExit(
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    )
                    .clickable(null, LocalIndication.current, onClick = {})
            ) {
                content()
            }
        }
    }
}