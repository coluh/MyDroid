package com.destywen.mydroid

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.destywen.mydroid.data.local.AppDatabase
import com.destywen.mydroid.domain.AppLogger
import com.destywen.mydroid.data.local.AppSettings
import com.destywen.mydroid.data.local.ChatDao
import com.destywen.mydroid.data.local.JournalDao
import com.destywen.mydroid.data.local.LogDao
import com.destywen.mydroid.data.local.ScheduleDao
import com.destywen.mydroid.data.remote.AiChatService
import com.destywen.mydroid.data.remote.NetworkModule
import com.destywen.mydroid.domain.ChatRepository
import com.destywen.mydroid.domain.FileManager
import com.destywen.mydroid.ui.screen.MainApp
import com.destywen.mydroid.ui.theme.MyDroidTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyDroidTheme {
                MainApp()
            }
        }
    }
}
