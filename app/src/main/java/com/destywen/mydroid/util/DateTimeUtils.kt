package com.destywen.mydroid.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun Long.toDateTime(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())

fun Long.toDateTimeString(): String = this.toDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

fun Long.toSmartTime(): String {
    val now = LocalDateTime.now()
    val target = this.toDateTime()

    val formatter = when {
        target.toLocalDate() == now.toLocalDate() -> {
            DateTimeFormatter.ofPattern("HH:mm")
        }

        target.toLocalDate() == now.toLocalDate().minusDays(1) -> {
            return "昨天 ${target.format(DateTimeFormatter.ofPattern("HH:mm"))}"
        }

        target.year == now.year -> {
            DateTimeFormatter.ofPattern("MM-dd HH:mm")
        }

        else -> {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        }
    }

    return target.format(formatter)
}