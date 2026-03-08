package com.destywen.mydroid.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun Long.toDateTime(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())

fun Long.toDateTimeString(pattern: String? = null): String {
    return this.toDateTime().format(DateTimeFormatter.ofPattern(pattern ?: "yyyy-MM-dd HH:mm:ss"))
}

fun Long.toSmartTime(ref: LocalDateTime? = null): String {
    val now = ref ?: LocalDateTime.now()
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

fun Long.toShortTime(ref: LocalDateTime? = null): String {
    val now = ref ?: LocalDateTime.now()
    val target = this.toDateTime()

    val formatter = when {
        target.hour == 0 && target.minute == 0 -> when {
            target.toLocalDate() == now.toLocalDate() -> return "今天"
            target.year == now.year -> DateTimeFormatter.ofPattern("MM-dd")
            else -> DateTimeFormatter.ofPattern("yyyy-MM-dd")
        }
        else -> when {
            target.toLocalDate() == now.toLocalDate() -> return "HH:mm"
            target.year == now.year -> DateTimeFormatter.ofPattern("MM-dd HH:mm")
            else -> DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        }
    }

    return target.format(formatter)
}