package com.destywen.mydroid.data.local

import android.os.Parcelable
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.parcelize.Parcelize

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val avatar: String?,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "private" | "group"
    val targetId: Long?,
    val title: String?,
    val avatar: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "group_members", primaryKeys = ["convId", "userId"]
)
data class GroupMemberEntity(
    val convId: Long,
    val userId: Long,
    val joinedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "messages",
    indices = [
        Index("convId"),
        Index("senderId"),
        Index(value = ["convId", "timestamp"], name = "idx_conv_time")
    ]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val convId: Long,
    val senderId: Long,
    val type: String = "text", // "text" | "image" | "file"
    val content: String, // or attachmentId
    val timestamp: Long = System.currentTimeMillis(),

    val replyToMsgId: Long? = null,
    val forwardFromMsgId: Long? = null,
    val forwardFromConvId: Long? = null,
)

@Entity(tableName = "attachments")
data class AttachmentEntity(
    @PrimaryKey val attachmentId: String,
    val messageId: Long,
    val filePath: String,
    val mimeType: String,
    val size: Long,
)

@Dao
interface ChatDao {
    @Insert
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Insert
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Delete
    suspend fun deleteConversation(conversation: ConversationEntity)

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Insert
    suspend fun insertMessage(message: MessageEntity): Long

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Delete
    suspend fun deleteMessage(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE convId = :convId ORDER BY timestamp ASC")
    fun getMessagesByConversation(convId: Long): Flow<List<MessageEntity>>

    @Query("DELETE FROM messages WHERE convId = :convId")
    suspend fun deleteMessagesByConversation(convId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addGroupMember(member: GroupMemberEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addGroupMembers(members: List<GroupMemberEntity>)

    @Query("SELECT * FROM group_members WHERE convId = :convId")
    fun getGroupMembers(convId: Long): Flow<List<GroupMemberEntity>>

    @Query("DELETE FROM group_members WHERE convId = :convId AND userId = :userId")
    suspend fun removeGroupMember(convId: Long, userId: Long)

    @Query("DELETE FROM group_members WHERE convId = :convId")
    suspend fun removeAllMembers(convId: Long)

    @Insert
    suspend fun insertAttachment(attachment: AttachmentEntity)

    @Delete
    suspend fun deleteAttachment(attachment: AttachmentEntity)

    @Query("SELECT * FROM attachments WHERE attachmentId = :attachmentId")
    suspend fun getAttachmentById(attachmentId: String): AttachmentEntity?

    @Query("SELECT * FROM chat_agents")
    fun getAgents(): Flow<List<AgentEntity>>
}

@Entity(tableName = "chat_agents")
@Parcelize
data class AgentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val systemPrompt: String,
    val modelName: String,
    val apiEndpoint: String? = null,
    val apiKey: String? = null,
    val temperature: Float = 0.7f,
    val createdAt: Long = System.currentTimeMillis(),
) : Parcelable {
    val display: String
        get() = "$name-${modelName.take(3)}"
}

// only in old version
@Entity(
    tableName = "chat_messages", foreignKeys = [ForeignKey(
        entity = AgentEntity::class, parentColumns = ["id"], childColumns = ["agentId"]
    )], indices = [Index("agentId")]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, val agentId: Long? = null,
    // val username,
    val role: String, // "user" | "assistant"
    val content: String, val timestamp: Long = System.currentTimeMillis()
)
