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
            .addMigrations(
                migration1to2, migration2to3, migration3to4, migration4to5,
                migration5to6, migration6to7, migration7to8, migration8to9
            )
            .build()
    }
    val journalDao: JournalDao get() = database.journalDao()
    val chatDao: ChatDao get() = database.chatDao()
    val logDao: LogDao get() = database.logDao()
    val scheduleDao: ScheduleDao get() = database.scheduleDao()

    val settings: AppSettings by lazy {
        AppSettings(context)
    }

    val chatRepository: ChatRepository by lazy {
        ChatRepository(chatDao, settings)
    }

    val chatService: AiChatService by lazy {
        AiChatService(NetworkModule.client, settings)
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

    val migration4to5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `chat_agents` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `systemPrompt` TEXT NOT NULL,
                    `apiEndpoint` TEXT,
                    `apiKey` TEXT,
                    `modelName` TEXT NOT NULL,
                    `temperature` REAL NOT NULL,
                    `createdAt` INTEGER NOT NULL
                )
            """.trimIndent()
            )

            db.execSQL("DROP TABLE chat_messages")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `chat_messages` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `agentId` INTEGER,
                    `role` TEXT NOT NULL,
                    `content` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL,
                    FOREIGN KEY(`agentId`) REFERENCES `chat_agents`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
                )
            """.trimIndent()
            )
        }
    }

    val migration5to6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_agentId ON chat_messages(agentId)")
        }
    }

    val migration6to7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `schedules` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title` TEXT NOT NULL,
                    `description` TEXT,
                    `due` INTEGER,
                    `isCompleted` INTEGER NOT NULL DEFAULT 0,
                    `createdAt` INTEGER NOT NULL
                )
            """.trimIndent()
            )
        }
    }

    val migration7to8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `schedule_groups` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL
                )
            """.trimIndent()
            )
            db.execSQL("ALTER TABLE `schedules` ADD COLUMN `groupId` INTEGER DEFAULT NULL")
        }
    }

    val migration8to9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `users` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `avatar` TEXT,
                    `createdAt` INTEGER NOT NULL
                )
            """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `conversations` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `type` TEXT NOT NULL,
                    `targetId` INTEGER,
                    `title` TEXT,
                    `avatar` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL
                )
            """.trimIndent()
            )

            // 3. group_members 表（联合主键）
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `group_members` (
                    `convId` INTEGER NOT NULL,
                    `userId` INTEGER NOT NULL,
                    `joinedAt` INTEGER NOT NULL,
                    PRIMARY KEY (`convId`, `userId`)
                )
            """.trimIndent()
            )

            // 4. messages 表（带索引）
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `messages` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `convId` INTEGER NOT NULL,
                    `senderId` INTEGER NOT NULL,
                    `type` TEXT NOT NULL,
                    `content` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL,
                    `replyToMsgId` INTEGER,
                    `forwardFromMsgId` INTEGER,
                    `forwardFromConvId` INTEGER
                )
            """.trimIndent()
            )

            // messages 表的索引
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_convId` ON `messages` (`convId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_senderId` ON `messages` (`senderId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `idx_conv_time` ON `messages` (`convId`, `timestamp`)")

            // 5. attachments 表（带外键）
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `attachments` (
                    `attachmentId` TEXT NOT NULL PRIMARY KEY,
                    `messageId` INTEGER NOT NULL,
                    `filePath` TEXT NOT NULL,
                    `mimeType` TEXT NOT NULL,
                    `size` INTEGER NOT NULL
                )
            """.trimIndent()
            )
        }
    }
}