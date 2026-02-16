package com.destywen.mydroid.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "app_logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val level: String, // DEBUG, INFO, WARN, ERROR
    val tag: String,
    val message: String
)

enum class LogLevel { DEBUG, INFO, WARN, ERROR }

@Dao
interface LogDao {
    @Insert
    suspend fun insert(log: LogEntity)

    @Query("SELECT * FROM app_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 1000): Flow<List<LogEntity>>
}