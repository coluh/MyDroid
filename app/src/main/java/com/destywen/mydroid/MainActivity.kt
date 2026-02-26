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
import com.destywen.mydroid.data.remote.AiChatService
import com.destywen.mydroid.data.remote.NetworkModule
import com.destywen.mydroid.domain.FileManager
import com.destywen.mydroid.ui.screen.MainApp
import com.destywen.mydroid.ui.theme.MyDroidTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = AppContainer(this)
        AppLogger.init(container.logDao)
        File(filesDir, "img").apply { mkdirs() }

        AppLogger.i("MainActivity", "------应用启动，祝一切顺利------")
        setContent {
            MyDroidTheme {
                MainApp(container)
            }
        }
    }
}

class AppContainer(private val context: Context) {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(context, AppDatabase::class.java, "mydroid.db")
            .addMigrations(migration1to2, migration2to3, migration3to4)
            .build()
    }
    val journalDao: JournalDao get() = database.journalDao()
    val chatDao: ChatDao get() = database.chatDao()
    val logDao: LogDao get() = database.logDao()

    val settings: AppSettings by lazy {
        AppSettings(context)
    }

    val chatService: AiChatService by lazy {
        AiChatService(NetworkModule.client)
    }

    val fileManager: FileManager by lazy {
        FileManager(context)
    }

    val migration1to2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                            CREATE TABLE IF NOT EXISTS `app_logs` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                                `timestamp` INTEGER NOT NULL, 
                                `level` TEXT NOT NULL, 
                                `tag` TEXT NOT NULL, 
                                `message` TEXT NOT NULL
                               )
                        """.trimIndent()
            )
        }
    }

    val migration2to3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE journals ADD COLUMN image TEXT DEFAULT NULL")
        }
    }

    val migration3to4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE comments ADD COLUMN role TEXT NOT NULL DEFAULT 'user'")
        }
    }
}