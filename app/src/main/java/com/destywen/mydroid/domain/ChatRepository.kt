package com.destywen.mydroid.domain

import com.destywen.mydroid.data.local.AppSettings
import com.destywen.mydroid.data.local.ChatDao
import com.destywen.mydroid.data.local.ConversationEntity
import com.destywen.mydroid.data.local.MemberEntity
import com.destywen.mydroid.data.local.MessageEntity
import com.destywen.mydroid.data.local.UserEntity
import com.destywen.mydroid.domain.model.Conversation
import com.destywen.mydroid.domain.model.ConversationType
import com.destywen.mydroid.domain.model.Message
import com.destywen.mydroid.domain.model.MessageType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class ChatRepository(private val chatDao: ChatDao, private val settings: AppSettings) {
    fun getAllConversations(): Flow<List<Conversation>> {
        return combine(
            chatDao.getAllConversations(),
            chatDao.getAllMembers(),
            chatDao.getAllUsers(),
            chatDao.getLatestMessagesPerConv(),
            settings.userId,
        ) { convs, members, users, latestMessages, selfId ->
            val latestMsgMap = latestMessages.associateBy { it.convId }
            convs.mapNotNull { conv ->
                val convMembers = members.filter { it.convId == conv.id }
                val unreadCount = convMembers.find { it.userId == selfId }?.unread ?: 0
                val latestMsg = latestMsgMap[conv.id]

                when (conv.type) {
                    "private" -> {
                        val otherMember = convMembers.firstOrNull { it.userId != selfId } ?: convMembers.firstOrNull()
                        val otherUser = users.find { it.id == otherMember?.userId }
                        Conversation(
                            id = conv.id,
                            type = ConversationType.PRIVATE,
                            title = otherUser?.name ?: "?",
                            avatar = otherUser?.avatar,
                            lastMessagePreview = latestMsg?.content,
                            lastMessageTime = latestMsg?.timestamp ?: conv.updatedAt,
                            unreadCount = unreadCount,
                        )
                    }

                    "group" -> {
                        Conversation(
                            id = conv.id,
                            type = ConversationType.GROUP,
                            title = conv.title ?: "?",
                            avatar = conv.avatar,
                            lastMessagePreview = latestMsg?.content,
                            lastMessageTime = latestMsg?.timestamp ?: conv.createdAt,
                            unreadCount = unreadCount,
                        )
                    }

                    else -> null
                }
            }
        }
    }

    suspend fun createUser(name: String, avatar: String?) {
        val user = UserEntity(
            name = name,
            avatar = avatar,
        )
        val userId = chatDao.insertUser(user)
        if (settings.userId.first() == null || settings.userId.first() == 0L) {
            settings.updateUserId(userId)
        }

        // create private conversation
        val current = System.currentTimeMillis()
        val conversation = ConversationEntity(
            type = "private",
            title = null,
            avatar = null,
            createdAt = current,
            updatedAt = current,
        )
        val convId = chatDao.insertConversation(conversation)
        chatDao.addMember(MemberEntity(convId, userId, 0, current))
        val selfId = settings.userId.first()!!
        if (userId != selfId) {
            chatDao.addMember(MemberEntity(convId, selfId, 0, current))
        }
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
        // update time and  unread
        val conv = chatDao.getConversation(convId).copy(
            updatedAt = message.timestamp
        )
        chatDao.updateConversation(conv)
        chatDao.incrementUnread(convId, senderId)
    }

    suspend fun clear() {
        val convs = chatDao.getAllConversations().first()
        convs.forEach { conv ->
            val messages = chatDao.getMessagesByConversation(conv.id).first()
            messages.forEach { message ->
                chatDao.deleteMessage(message)
            }
            chatDao.deleteConversation(conv)
        }
        chatDao.getAllUsers().first().forEach { user ->
            chatDao.deleteUser(user)
        }
        chatDao.getAllMembers().first().forEach { member ->
            chatDao.removeMember(member.convId, member.userId)
        }
        settings.updateUserId(0)
    }
}