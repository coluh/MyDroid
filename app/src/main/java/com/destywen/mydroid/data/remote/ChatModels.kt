package com.destywen.mydroid.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val stream: Boolean = false,
    val temperature: Float = 0.7f
    // responseFormat
)

@Serializable
data class Message (
    val role: String, // "system", "user", "assistant"
    val content: String
)

@Serializable
data class ChatResponse(
    val id: String,
    val created: Long,
    val model: String,
    val usage: TokenUsage,
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val message: Message,
)

@Serializable
data class ChatStreamResponse(
    val id: String,
    val created: Long,
    val model: String,
    val choices: List<StreamChoice>
)

@Serializable
data class StreamChoice(
    val delta: StreamDelta,
)

@Serializable
data class StreamDelta(
    val content: String? = null,
)

@Serializable
data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)