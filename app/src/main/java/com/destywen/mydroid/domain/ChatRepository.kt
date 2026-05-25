package com.destywen.mydroid.domain

import com.destywen.mydroid.data.local.AppSettings
import com.destywen.mydroid.data.local.ChatDao
import com.destywen.mydroid.data.local.ConversationEntity
import com.destywen.mydroid.data.local.Keys
import com.destywen.mydroid.data.local.LlmConfigEntity
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class ChatRepository(private val chatDao: ChatDao, private val settings: AppSettings) {
    // 都针对当前用户
    private val selfUserId = settings.config.map { it.userId }

    fun getAllUsers(): Flow<List<UserEntity>> = chatDao.getAllUsers()

    fun getAllConversations(): Flow<List<Conversation>> {
        return combine(
            chatDao.getAllConversations(),
            chatDao.getLatestMessagesPerConv(),
            chatDao.getAllMembers(),
            chatDao.getAllUsers(),
            selfUserId
        ) { convs, messages, members, users, selfId ->
            if (selfId == null) return@combine emptyList()
            val selfUser = users.find { it.id == selfId } ?: return@combine emptyList()

            convs.mapNotNull { conv ->
                val convMembers = members.filter { it.convId == conv.id }
                if (convMembers.none { it.userId == selfId }) return@mapNotNull null

                val latestMessage = messages.firstOrNull { it.convId == conv.id }
                val unread = convMembers.find { it.userId == selfId }?.unread ?: 0
                val memberCount = convMembers.size

                val (type, title, avatar) = when {
                    memberCount > 2 -> Triple(ConversationType.GROUP, conv.title ?: "?", conv.avatar)
                    memberCount == 2 -> {
                        val otherMember = convMembers.first { it.userId != selfId }
                        val otherUser = users.find { it.id == otherMember.userId }
                        Triple(ConversationType.PRIVATE, otherUser?.name ?: "?", otherUser?.avatar)
                    }

                    else -> Triple(ConversationType.PRIVATE, selfUser.name, selfUser.avatar)
                }

                Conversation(
                    id = conv.id,
                    type = type,
                    title = title,
                    avatar = avatar,
                    latestMessagePreview = latestMessage?.content,
                    latestMessageTime = latestMessage?.timestamp ?: conv.updatedAt,
                    unreadCount = unread,
                )
            }
        }
    }

    fun getConvMessages(convId: Long): Flow<List<Message>> {
        return combine(chatDao.getConvMessages(convId), chatDao.getAllUsers(), selfUserId) { messages, users, selfId ->
            messages.map { message ->
                val user = users.find { it.id == message.senderId }
                Message(
                    id = message.id,
                    convId = convId,
                    senderId = message.senderId,
                    senderName = user?.name ?: "?",
                    senderAvatar = user?.avatar,
                    type = MessageType.from(message.type),
                    content = message.content,
                    timestamp = message.timestamp,
                    isSelf = message.senderId == selfId,
                    replyToMsgId = message.replyToMsgId,
                    forwardFromMsgId = message.forwardFromMsgId,
                    forwardFromConvId = message.forwardFromConvId,
                )
            }
        }
    }

    fun getConvMembers(convId: Long): Flow<List<MemberEntity>> = chatDao.getConvMembers(convId)

    fun getAllLlmConfigs(): Flow<List<LlmConfigEntity>> = chatDao.getAllLlmConfigs()

    fun getLlmConfig(userId: Long): LlmConfigEntity? = chatDao.getLlmConfigByUser(userId)

    suspend fun createUser(name: String, avatar: String?) {
        val userId = chatDao.insertUser(UserEntity(name = name, avatar = avatar))
        val selfId = selfUserId.first()
        if (selfId == null || selfId == 0L) {
            settings.update { it[Keys.USER_ID] = userId }
        }

        // create all private conversations
        chatDao.getAllUsers().first().forEach { user ->
            createConversation(listOf(user.id, userId).distinct())
        }
    }

    suspend fun createConversation(userIds: List<Long>) {
        if (userIds.isEmpty()) return
        val users = chatDao.getAllUsers().first()
        val existingIds = users.map { it.id }.toSet()
        if (userIds.any { it !in existingIds }) return

        val (type, title, avatar) = when {
            userIds.size > 2 -> {
                val title = users.filter { it.id in userIds }.map { it.name }.joinToString("-")
                Triple("group", title, null)
            }

            userIds.size == 2 -> Triple("private", null, null)
            else -> {
                val user = users.find { it.id == userIds[0] }!!
                Triple("private", user.name, user.avatar)
            }
        }
        val convId = chatDao.insertConversation(
            ConversationEntity(
                type = type,
                title = title,
                avatar = avatar,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
        userIds.forEach { userId ->
            chatDao.insertMember(MemberEntity(convId, userId))
        }
    }

    // user id is in LlmConfigEntity
    suspend fun saveLlmConfig(config: LlmConfigEntity) {
        if (config.id == 0L) chatDao.insertLlmConfig(config)
        else chatDao.updateLlmConfig(config)
    }

    suspend fun sendTextMessage(convId: Long, senderId: Long, content: String) {
        chatDao.insertMessage(MessageEntity(convId = convId, senderId = senderId, type = "text", content = content))
        chatDao.updateConversationTime(convId, System.currentTimeMillis())
        chatDao.incrementUnreadExceptSender(convId, senderId)
    }

    suspend fun clearUnread(convId: Long, userId: Long) = chatDao.clearUnread(convId, userId)


    suspend fun resetRepo() {
        val convs = chatDao.getAllConversations().first()
        convs.forEach { conv ->
            chatDao.deleteMessagesByConv(conv.id)
            chatDao.removeConvMembers(conv.id)
            chatDao.deleteConversation(conv)
        }
        chatDao.getAllUsers().first().forEach { user ->
            chatDao.deleteUser(user)
        }
        settings.update { it[Keys.USER_ID] = 0 }
    }
}