package com.destywen.mydroid.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [JournalEntity::class, CommentEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao
    companion object {
        @Volatile
        private var instance: AppDatabase? = null
        fun get(ctx: Context) = instance ?: synchronized(this) {
//            ctx.deleteDatabase("app.db")
            Room.databaseBuilder(ctx, AppDatabase::class.java, "app.db")
                .build()
                .also { instance = it }
        }
    }
}