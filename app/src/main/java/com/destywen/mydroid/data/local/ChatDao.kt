package com.destywen.mydroid.data.local

import android.os.Parcelable
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.parcelize.Parcelize

@Entity(tableName = "chat_agents")
@Parcelize
data class AgentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val systemPrompt: String,
    val apiEndpoint: String?,
    val apiKey: String?,
    val modelName: String,
    val temperature: Float = 0.7f,
    val createdAt: Long = System.currentTimeMillis(),
) : Parcelable {
    val display: String
        get() = "$name-${modelName.take(3)}"
}

@Entity(
    tableName = "chat_messages",
    foreignKeys = [ForeignKey(
        entity = AgentEntity::class,
        parentColumns = ["id"],
        childColumns = ["agentId"]
    )],
    indices = [Index("agentId")]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val agentId: Long? = null,
    // val username,
    val role: String, // "user" | "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ChatDao {

    @Query("SELECT * FROM chat_agents")
    fun getAgents(): Flow<List<AgentEntity>>

    @Upsert
    suspend fun upsertAgent(agent: AgentEntity)

    @Query("DELETE FROM chat_agents WHERE id = :id")
    suspend fun deleteAgent(id: Long)

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Insert
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteMessageById(id: Long)
}