package com.destywen.mydroid.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.destywen.mydroid.util.toDateTime
import com.destywen.mydroid.util.toDateTimeString
import java.time.LocalDateTime
import java.time.ZoneId

// TODO: optimize
@Composable
fun EditableDropdown(
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {

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
        Text(datetime?.toDateTimeString()?:"点击选择时间")
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

            BackHandler() {
                onDismissRequest()
            }
        }
    }
}