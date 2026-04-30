package com.destywen.mydroid.domain

import com.destywen.mydroid.data.local.AppSettings
import com.destywen.mydroid.data.local.ChatDao
import com.destywen.mydroid.data.local.ConversationEntity
import com.destywen.mydroid.data.local.MessageEntity
import com.destywen.mydroid.data.local.UserEntity
import com.destywen.mydroid.domain.model.Conversation
import com.destywen.mydroid.domain.model.ConversationType
import com.destywen.mydroid.domain.model.Message
import com.destywen.mydroid.domain.model.MessageType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ChatRepository(private val chatDao: ChatDao, private val settings: AppSettings) {
    fun getAllConversations(): Flow<List<Conversation>> {
        return combine(
            chatDao.getAllConversations(),
            chatDao.getAllUsers(),
            chatDao.getLatestMessagesPerConv(),
        ) { convs, users, latestMessages ->
            val userNameMap = users.associate { it.id to it.name }
            val userAvatarMap = users.associate { it.id to it.avatar }
            val latestMsgMap = latestMessages.associateBy { it.convId }
            convs.map { conv ->
                val type = if (conv.type == "private") ConversationType.PRIVATE else ConversationType.GROUP
                val title = conv.title ?: userNameMap[conv.targetId]
                val avatar = conv.avatar ?: userAvatarMap[conv.targetId]
                Conversation(
                    id = conv.id,
                    type = type,
                    targetId = conv.targetId,
                    title = title ?: "undefined",
                    avatar = avatar,
                    lastMessagePreview = latestMsgMap[conv.id]?.content,
                    lastMessageTime = latestMsgMap[conv.id]?.timestamp ?: conv.createdAt,
                    unreadCount = 123,
                )
            }
        }
    }

    suspend fun createUser(name: String, avatar: String?) {
        val user = UserEntity(
            name = name,
            avatar = avatar,
        )
        val userId = chatDao.insertUser(user)

        // create conversation
        val current = System.currentTimeMillis()
        val conversation = ConversationEntity(
            type = "private",
            targetId = userId,
            title = null,
            avatar = null,
            createdAt = current,
            updatedAt = current,
        )
        chatDao.insertConversation(conversation)
    }

    fun getMessages(convId: Long): Flow<List<Message>> {
        return combine(chatDao.getMessagesByConversation(convId), settings.userId) { entities, userId ->
            entities.map { entity ->
                Message(
                    id = entity.id,
                    convId = entity.convId,
                    senderId = entity.senderId,
                    type = when (entity.type) {
                        "text" -> MessageType.TEXT
                        "image" -> MessageType.IMAGE
                        "FILE" -> MessageType.FILE
                        else -> MessageType.TEXT
                    },
                    content = entity.content,
                    timestamp = entity.timestamp,
                    isSelf = entity.senderId == userId,
                    replyToMsgId = entity.replyToMsgId,
                    forwardFromMsgId = entity.forwardFromMsgId,
                    forwardFromConvId = entity.forwardFromConvId,
                )
            }
        }
    }

    fun getUsers() = chatDao.getAllUsers()

    suspend fun sendTextMessage(convId: Long, senderId: Long, content: String) {
        val message = MessageEntity(
            convId = convId,
            senderId = senderId,
            type = "text",
            content = content,
        )
        chatDao.insertMessage(message)
        // update time and TODO: unread
        val conv = chatDao.getConversation(convId).copy(
            updatedAt = message.timestamp
        )
        chatDao.updateConversation(conv)
    }
}