package com.destywen.mydroid.data.remote

data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val stream: Boolean = false,
    val temperature: Float = 0.7f
    // responseFormat
)

data class Message (
    val role: String, // "system", "user", "assistant"
    val content: String
)

data class ChatResponse(
    val id: String,
    val created: Long,
    val model: String,
    val usage: TokenUsage,
    val choices: List<Choice>
)

data class Choice(
    val message: Message,
)

data class ChatStreamResponse(
    val id: String,
    val created: Long,
    val model: String,
    val choices: List<StreamChoice>
)

data class StreamChoice(
    val delta: StreamDelta,
)

data class StreamDelta(
    val content: String? = null,
)

data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)