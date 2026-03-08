package com.destywen.mydroid.data.local

import android.os.Parcelable
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.parcelize.Parcelize

@Entity(tableName = "schedules")
@Parcelize
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    val due: Long? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
) : Parcelable

@Dao
interface ScheduleDao {

    @Query("SELECT * FROM schedules")
    fun getAll(): Flow<List<ScheduleEntity>>

    @Upsert
    suspend fun upsert(schedule: ScheduleEntity)

    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun deleteById(id: Long)
}