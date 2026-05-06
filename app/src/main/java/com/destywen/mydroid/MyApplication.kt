package com.destywen.mydroid

import android.app.Application
import com.destywen.mydroid.data.local.AppDatabase
import com.destywen.mydroid.data.local.AppSettings
import com.destywen.mydroid.data.remote.AiChatService
import com.destywen.mydroid.data.remote.NetworkModule
import com.destywen.mydroid.domain.AppLogger
import com.destywen.mydroid.domain.ChatRepository
import com.destywen.mydroid.domain.FileManager

class MyApplication : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val settings by lazy { AppSettings(this) }
    val chatRepository by lazy { ChatRepository(database.chatDao(), settings) }
    val fileManager by lazy { FileManager(this) }
    val apiService by lazy { AiChatService(NetworkModule.client, settings) }

    override fun onCreate() {
        super.onCreate()
        AppLogger.init(database.logDao())
    }
}