package com.destywen.mydroid.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "journals")
data class JournalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val content: String,
    val image: String? = null, // filename in image directory
    val tag: String = "", // tags separated by comma
    val time: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "comments",
    foreignKeys = [ForeignKey(
        entity = JournalEntity::class,
        parentColumns = ["id"],
        childColumns = ["journalId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["journalId"])]
)
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val journalId: Int,
    val name: String = "system", // agent id
    val content: String,
    val time: Long = System.currentTimeMillis()
)

@Dao
interface JournalDao {
    @Query("SELECT * FROM journals ORDER BY time DESC")
    fun getAllJournals(): Flow<List<JournalEntity>>

    @Query("SELECT * FROM comments")
    fun getAllComments(): Flow<List<CommentEntity>>

    @Upsert
    suspend fun upsertJournal(journal: JournalEntity)

    @Insert
    suspend fun insertComment(comment: CommentEntity)

    @Query("DELETE FROM journals WHERE id = :id")
    suspend fun deleteJournal(id: Int)
}