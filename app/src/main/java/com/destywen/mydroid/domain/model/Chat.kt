package com.destywen.mydroid.domain.model

enum class ConversationType {
    PRIVATE, GROUP;
}

data class Conversation(
    val id: Long,
    val type: ConversationType,
    val targetId: Long?, // if type is PRIVATE
    val title: String,
    val avatar: String?,
    val lastMessagePreview: String?,
    val lastMessageTime: Long,
    val unreadCount: Int,
)

enum class MessageType {
    TEXT, IMAGE, FILE;
}

data class Message(
    val id: Long = 0,
    val convId: Long,
    val senderId: Long,
    val type: MessageType,
    val content: String, // or attachmentId
    val timestamp: Long,
    val isSelf: Boolean,

    val replyToMsgId: Long? = null,
    val forwardFromMsgId: Long? = null,
    val forwardFromConvId: Long? = null,
)