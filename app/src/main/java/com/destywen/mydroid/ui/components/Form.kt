package com.destywen.mydroid.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.destywen.mydroid.util.toDateTime
import com.destywen.mydroid.util.toDateTimeString
import java.time.LocalDateTime
import java.time.ZoneId

fun pickDate(context: Context, initial: LocalDateTime? = null, onDateSelected: (Int, Int, Int) -> Unit) {
    val now = initial ?: LocalDateTime.now()
    DatePickerDialog(context, { _, selYear, selMonth, selDay ->
        onDateSelected(selYear, selMonth + 1, selDay)
    }, now.year, now.monthValue - 1, now.dayOfMonth).show()
}

fun pickTime(context: Context, initial: LocalDateTime? = null, onTimeSelected: (Int, Int) -> Unit) {
    val now = initial ?: LocalDateTime.now()
    TimePickerDialog(context, { _, hour, minute ->
        onTimeSelected(hour, minute)
    }, now.hour, now.minute, true).show()
}

@Composable
fun DateTimePickerButton(datetime: Long?, onSelected: (Long) -> Unit) {
    val context = LocalContext.current

    OutlinedButton({
        pickDate(context, datetime?.toDateTime()) { year, month, day ->
            pickTime(context, datetime?.toDateTime()) { hour, minute ->
                val result = LocalDateTime.of(year, month, day, hour, minute)
                onSelected(result.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
            }
        }
    }) {
        Icon(Icons.Default.DateRange, null)
        Spacer(Modifier.width(8.dp))
        Text(datetime?.toDateTimeString() ?: "点击选择时间")
    }
}

@Composable
fun ClickTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit),
) {
    var show by remember { mutableStateOf(!value.isBlank()) }
    val focusRequester = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }

    LaunchedEffect(show) {
        if (show) {
            focusRequester.requestFocus()
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        if (!show) {
            Button(onClick = { show = true }) { label() }
        } else {
            BasicTextField(
                value,
                onValueChange,
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .border(1.dp, Color.Gray, RectangleShape)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused != hasFocus) {
                            hasFocus = focusState.isFocused
                            if (!focusState.isFocused) {
                                if (value.isBlank()) {
                                    show = false
                                }
                            }
                        }
                    },
                decorationBox = {
                    Box {
                        it()
                    }
                }
            )
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
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .imePadding(),
                    color = MaterialTheme.colors.background
                ) {
                    content()
                }
            }

            BackHandler {
                onDismissRequest()
            }
        }
    }
}