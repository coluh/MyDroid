package com.destywen.mydroid.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    val status: Int = 0,
    val priority: Int = 0,
    val category: String? = null,
    @ColumnInfo(name = "estimated_minutes")
    val estimatedMinutes: Int? = null,
    @ColumnInfo(name = "energy_level")
    val energyLevel: Int = 2,
    @ColumnInfo(name = "actual_minutes")
    val actualMinutes: Int? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,
    @ColumnInfo(name = "due_date")
    val dueDate: Long? = null
)

object TaskStatus {
    const val PENDING = 0
    const val PROCESSING = 1
    const val COMPLETED = 2
    const val CANCELLED = 3
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY status ASC, priority ASC, updated_at DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)
}