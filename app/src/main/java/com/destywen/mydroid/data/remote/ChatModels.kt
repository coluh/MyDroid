package com.destywen.mydroid.data.remote

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
* Request
* */

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val stream: Boolean = false,
    val temperature: Double? = 0.7,
    @SerialName("top_p")
    val topP: Double? = null,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    val n: Int? = null,
    val stop: List<String>? = null,
    @SerialName("enable_thinking")
    val enableThinking: Boolean? =null,
    @SerialName("response_format")
    val responseFormat: ResponseFormat? = null,
)

@Serializable
@Parcelize
data class Message(
    val role: String, // "system" | "user" | "assistant"
    val content: String
) : Parcelable

@Serializable
data class ChatRequestVision(
    val model: String,
    val messages: List<MessageVision>,
    val stream: Boolean = false,
    val temperature: Double? = 0.7,
    @SerialName("top_p")
    val topP: Double? = null,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    val n: Int? = null,
    val stop: List<String>? = null,
    @SerialName("enable_thinking")
    val enableThinking: Boolean? =null,
    @SerialName("response_format")
    val responseFormat: ResponseFormat? = null,
)

@Serializable
data class MessageVision(
    val role: String,
    val content: List<ContentVision>
)

@Serializable
data class ContentVision(
    val type: String, // "text" | "image_url"
    val text: String? = null,
    @SerialName("image_url")
    val imageUrl: ContentImage? = null,
)

@Serializable
data class ContentImage(
    val url: String,
)

@Serializable
data class ResponseFormat(
    val type: String // "text" | "json_object" | "json_schema"
) {
    companion object {
        const val TEXT = "text"
        const val JSON = "json_object"
        const val JSON_SCHEMA  = "json_schema"
    }
}

/*
* Non-Streaming Response
* */

@Serializable
data class ChatResponse(
    val id: String,
    val created: Long,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage? = null,
)

@Serializable
data class Choice(
    val index: Int,
    val message: Message,
    @SerialName("finish_reason")
    val finishReason: String? = null,
)

@Serializable
data class Usage(
    @SerialName("prompt_tokens")
    val promptTokens: Int,
    @SerialName("completion_tokens")
    val completionTokens: Int,
    @SerialName("total_tokens")
    val totalTokens: Int
)

/*
* Streaming Chunk
* */

@Serializable
data class StreamChunk(
    val id: String,
    val created: Long,
    val model: String,
    val choices: List<StreamChoice>
)

@Serializable
data class StreamChoice(
    val delta: Delta,
)

@Serializable
data class Delta(
    val content: String? = null,
)

/*
* Error Response
* */

@Serializable
data class ChatErrorWrapper(
    val error: ChatError,
)

@Serializable
data class ChatError(
    val message: String,
    val type: String? = null,
    val param: String? = null,
    val code: String? = null,
)