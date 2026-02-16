package com.destywen.mydroid.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [JournalEntity::class, CommentEntity::class, ChatMessageEntity::class, LogEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao
    abstract fun chatDao(): ChatDao
    abstract fun logDao(): LogDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null
        fun get(ctx: Context) = instance ?: synchronized(this) {
            val MIGRATION_1_2 = object : Migration(1, 2) {
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
            Room.databaseBuilder(ctx, AppDatabase::class.java, "mydroid.db")
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { instance = it }
        }
    }
}