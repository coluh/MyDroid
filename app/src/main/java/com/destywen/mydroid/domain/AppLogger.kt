package com.destywen.mydroid.domain

import android.util.Log
import com.destywen.mydroid.data.local.LogDao
import com.destywen.mydroid.data.local.LogEntity
import com.destywen.mydroid.data.local.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object AppLogger {
    private var logDao: LogDao? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(dao: LogDao) {
        logDao = dao
    }

    fun log(level: LogLevel, tag: String, message: String) {
        when (level) {
            LogLevel.DEBUG -> Log.d(tag, message)
            LogLevel.INFO -> Log.i(tag, message)
            LogLevel.WARN -> Log.w(tag, message)
            LogLevel.ERROR -> Log.e(tag, message)
        }
        scope.launch {
            try {
                logDao?.insert(LogEntity(level = level.name, tag = tag, message = message))
            } catch (e: Exception) {
                Log.e("AppLogger", "Failed to save log to DB", e)
            }
        }
    }

    fun d(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)
    fun i(tag: String, message: String) = log(LogLevel.INFO, tag, message)
    fun w(tag: String, message: String) = log(LogLevel.WARN, tag, message)
    fun e(tag: String, message: String) = log(LogLevel.ERROR, tag, message)
}