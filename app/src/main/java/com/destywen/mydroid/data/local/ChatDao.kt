package com.destywen.mydroid.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val avatar: String?,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "llm_config")
data class LlmConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long?,
    val name: String,
    val provider: String,
    val model: String,
    val apiKey: String = "",
    val systemPrompt: String,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val topP: Float = 0.9f,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val endpoint: String?
        get() = when (provider.lowercase()) {
            "deepseek" -> "https://api.deepseek.com/chat/completions"
            "dashscope" -> "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
            else -> provider
        }
}

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "private" | "group", 没什么用呢，仅标记
    val title: String?, // used for type == "group"
    val avatar: String?, // used for type == "group"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long,
)

@Entity(
    tableName = "members", primaryKeys = ["convId", "userId"]
)
data class MemberEntity(
    val convId: Long,
    val userId: Long,
    val unread: Int = 0,
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

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversationById(id: Long): ConversationEntity

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMember(member: MemberEntity)

    @Update
    suspend fun updateMember(member: MemberEntity)

    @Query("DELETE FROM members WHERE convId = :convId AND userId = :userId")
    suspend fun removeConvMember(convId: Long, userId: Long)

    @Query("DELETE FROM members WHERE convId = :convId")
    suspend fun removeConvMembers(convId: Long)

    @Query("SELECT * FROM members")
    fun getAllMembers(): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE convId = :convId")
    fun getConvMembers(convId: Long): Flow<List<MemberEntity>>

    @Insert
    suspend fun insertMessage(message: MessageEntity): Long

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Delete
    suspend fun deleteMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE convId = :convId")
    suspend fun deleteMessagesByConv(convId: Long)

    @Insert
    suspend fun insertAttachment(attachment: AttachmentEntity)

    @Delete
    suspend fun deleteAttachment(attachment: AttachmentEntity)

    @Query("SELECT * FROM attachments WHERE attachmentId = :attachmentId")
    suspend fun getAttachmentById(attachmentId: String): AttachmentEntity?

    @Insert
    suspend fun insertLlmConfig(config: LlmConfigEntity)

    @Update
    suspend fun updateLlmConfig(config: LlmConfigEntity)

    @Delete
    suspend fun deleteLlmConfig(config: LlmConfigEntity)

    @Query("SELECT * FROM llm_config")
    fun getAllLlmConfigs(): Flow<List<LlmConfigEntity>>

    @Query("SELECT * FROM llm_config WHERE userId = :userId")
    fun getLlmConfigByUser(userId: Long): LlmConfigEntity?

    @Query("SELECT * FROM messages WHERE convId = :convId ORDER BY timestamp ASC")
    fun getConvMessages(convId: Long): Flow<List<MessageEntity>>

    @Query(
        """
                SELECT * FROM messages WHERE id IN (
                    SELECT MAX(id) FROM messages 
                    WHERE (convId, timestamp) IN (
                        SELECT convId, MAX(timestamp) FROM messages GROUP BY convId
                    )
                    GROUP BY convId
                )
            """
    )
    fun getLatestMessagesPerConv(): Flow<List<MessageEntity>>

    @Query("UPDATE conversations SET updatedAt = :timestamp WHERE id = :convId")
    suspend fun updateConversationTime(convId: Long, timestamp: Long)

    @Query("UPDATE members SET unread = unread + 1 WHERE convId = :convId AND userId != :senderId")
    suspend fun incrementUnreadExceptSender(convId: Long, senderId: Long)

    @Query("UPDATE members SET unread = 0 WHERE convId = :convId AND userId = :userId")
    suspend fun clearUnread(convId: Long, userId: Long)
}
