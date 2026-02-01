package com.destywen.mydroid.ui.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> Dropdown(
    modifier: Modifier = Modifier,
    options: List<T>,
    toText: (T) -> String,
    default: Int? = null,
    label: @Composable (() -> Unit)? = null,
    onSelect: (T) -> Unit
) {
    var shouldExpand by remember { mutableStateOf(false) }
    var selectedIndex by rememberSaveable { mutableStateOf(default) }

    val expanded = shouldExpand && options.isNotEmpty()
    val selected = if (selectedIndex == null) null else options.getOrNull(selectedIndex!!)
    val showText = if (selected == null) "-- - --" else toText(selected)

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { shouldExpand = it }, modifier = modifier) {
        TextField(
            value = showText,
            onValueChange = {},
            readOnly = true,
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
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(toText(option)) },
                    onClick = {
                        shouldExpand = false
                        selectedIndex = index
                        onSelect(option)
                    }
                )
            }
        }
    }
}

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
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(input) {
        shouldExpand = true
        delay(10)
        focusRequester.requestFocus()
    }

    val filteredOptions = remember(input) {
        if (input.isEmpty()) {
            options
        } else {
            options.filter { it.contains(input, ignoreCase = true) }
        }
    }
    val expanded = shouldExpand && filteredOptions.isNotEmpty()

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { shouldExpand = it }, modifier = modifier) {
        TextField(
            value = input,
            onValueChange = {
                input = it
                onValueChange(input)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .focusRequester(focusRequester),
            label = label,
            placeholder = { Text(placeHolder) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true
        )

        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { shouldExpand = false }) {
            filteredOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        shouldExpand = false
                        input = option
                        onValueChange(input)
                    }
                )
            }
        }
    }
}

@Composable
fun BottomModal(modifier: Modifier = Modifier, onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .imePadding()
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(interactionSource = null, indication = LocalIndication.current) {
                onDismissRequest()
            }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clickable(null, LocalIndication.current, onClick = {})
        ) {
            content()
        }
    }
}