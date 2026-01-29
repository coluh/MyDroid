package com.destywen.mydroid.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun timestampToLocalDateTime(timestamp: Long): LocalDateTime {
    return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();
}

fun timestampToLocalDateTimeString(timestamp: Long): String {
    return timestampToLocalDateTime(timestamp).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
}