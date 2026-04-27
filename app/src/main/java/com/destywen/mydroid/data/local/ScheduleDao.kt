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
    val groupId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
) : Parcelable

@Entity(tableName = "schedule_groups")
data class ScheduleGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Dao
interface ScheduleDao {

    @Query("SELECT * FROM schedules")
    fun getAll(): Flow<List<ScheduleEntity>>

    @Upsert
    suspend fun upsert(schedule: ScheduleEntity)

    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM schedule_groups ORDER BY createdAt")
    fun getAllGroups(): Flow<List<ScheduleGroupEntity>>

    @Upsert
    suspend fun upsertGroup(group: ScheduleGroupEntity)

    @Query("DELETE FROM schedule_groups WHERE id = :id")
    suspend fun deleteGroupById(id: Long)

    @Query("UPDATE schedules SET groupId = NULL WHERE groupId = :groupId")
    suspend fun clearGroupFromSchedules(groupId: Long)
}