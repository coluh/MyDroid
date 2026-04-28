package com.destywen.mydroid.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        JournalEntity::class, CommentEntity::class, AgentEntity::class, ChatMessageEntity::class, LogEntity::class,
        ScheduleEntity::class, ScheduleGroupEntity::class, UserEntity::class,
        ConversationEntity::class, GroupMemberEntity::class, MessageEntity::class, AttachmentEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao
    abstract fun chatDao(): ChatDao
    abstract fun logDao(): LogDao
    abstract fun scheduleDao(): ScheduleDao
}