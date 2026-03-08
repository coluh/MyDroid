package com.destywen.mydroid.data.remote

import android.util.Base64
import com.destywen.mydroid.data.local.AgentEntity
import com.destywen.mydroid.data.local.AppSettings
import com.destywen.mydroid.data.local.Role
import com.destywen.mydroid.domain.AppLogger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream

object NetworkModule {
    val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
        install(Logging) {
            level = LogLevel.INFO
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            socketTimeoutMillis = 60_000
        }
    }
}

class AiChatService(private val client: HttpClient, settings: AppSettings) {

    private val json = Json { ignoreUnknownKeys = true }
    private val defaultEndpoint = settings.defaultEndpoint
    private val defaultApiKey = settings.defaultApiKey
    private val visionModel = settings.vlModel

    suspend fun chat(context: List<Message>, config: AgentEntity, image: File? = null): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = if (image == null) {
                    val prompt = listOf(Message(Role.SYSTEM, config.systemPrompt)) + context
                    val endpoint = config.apiEndpoint ?: defaultEndpoint.first()
                    val apiKey = config.apiKey ?: defaultApiKey.first()
                    if (endpoint == null) {
                        error("endpoint not set")
                    }
                    AppLogger.i(
                        "chat",
                        "request to ${endpoint}, model: ${config.modelName}, system prompt: ${config.systemPrompt.isNotBlank()}, with ${context.size} messages"
                    )
//                    AppLogger.d("chat", "$prompt")
                    client.post(endpoint) {
                        header(HttpHeaders.Authorization, "Bearer $apiKey")
                        contentType(ContentType.Application.Json)
                        setBody(ChatRequest(config.modelName, prompt, stream = false))
                    }
                } else {
                    val endpoint = config.apiEndpoint ?: defaultEndpoint.first()
                    val apiKey = config.apiKey ?: defaultApiKey.first()
                    val model = visionModel.first()
                    if (endpoint.isNullOrBlank()) error("endpoint not set")
                    if (model.isNullOrBlank()) error("vision model not set")

                    val imageBase64 = FileInputStream(image).use { inputStream ->
                        val bytes = inputStream.readBytes()
                        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        "data:image/${image.extension};base64,$base64"
                    }
                    val content1img = ContentVision(type = "image_url", imageUrl = ContentImage(imageBase64))
                    val content1txt =
                        ContentVision(type = "text", text = config.systemPrompt + "\n\n" + context[0].content)
                    val prompt1 = MessageVision(Role.USER, listOf(content1img, content1txt))
                    val prompt = listOf(prompt1) + context.drop(1).map {
                        MessageVision(it.role, listOf(ContentVision(type = "text", text = it.content)))
                    }
                    AppLogger.i(
                        "chat",
                        "request to ${endpoint}, model: $model, with ${prompt.size} messages"
                    )
//                    AppLogger.d("chat", "$prompt")
//                    val bodyJson = json.encodeToString(ChatRequestVision(model, prompt, stream = false))
//                    AppLogger.d("chat", "body: $bodyJson")
                    client.post(endpoint) {
                        header(HttpHeaders.Authorization, "Bearer $apiKey")
                        contentType(ContentType.Application.Json)
                        setBody(ChatRequestVision(model, prompt, stream = false, enableThinking = false))
                    }
                }

                if (!response.status.isSuccess()) {
                    val body = response.bodyAsText()
                    val message = runCatching {
                        json.decodeFromString<ChatErrorWrapper>(body).error.message
                    }.getOrElse {
                        AppLogger.w("chat", "fail to parse error body: ${it.message}, body: $body")
                        body
                    }
                    error("HTTP status ${response.status.value}, response message: $message")
                }

                val body = response.body<ChatResponse>()
                AppLogger.i("chat", "${body.usage}")
                body.choices.first().message.content
            }.onFailure {
                AppLogger.e("chat", it.message ?: "unknown error")
            }
        }

    fun chatStreaming(context: List<Message>, config: AgentEntity): Flow<String> = channelFlow {
        val prompt = listOf(Message(Role.SYSTEM, config.systemPrompt)) + context
        val endpoint = config.apiEndpoint ?: defaultEndpoint.first()
        val apiKey = config.apiKey ?: defaultApiKey.first()
        if (endpoint == null) {
            error("endpoint not set")
        }
        val thinking = if (config.modelName.contains("qwen3.5")) false else null // don't want default think
        AppLogger.i(
            "chatStreaming",
            "request to ${endpoint}, model: ${config.modelName}, system prompt: ${config.systemPrompt.isNotBlank()}, with ${context.size} messages"
        )
        AppLogger.d("chatStreaming", "$prompt")
        client.preparePost(endpoint) {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(ChatRequest(config.modelName, prompt, stream = true, enableThinking = thinking))
        }.execute { response ->
            if (!response.status.isSuccess()) {
                val body = response.bodyAsText()
                val message = runCatching {
                    json.decodeFromString<ChatErrorWrapper>(body).error.message
                }.getOrElse {
                    AppLogger.w("chat", "fail to parse error body: ${it.message}, body: $body")
                    body
                }
                error("HTTP ${response.status.value}: $message")
            }

            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readLine() ?: continue
                if (!line.startsWith("data:")) continue
                val data = line.removePrefix("data:").trim()
                if (data == "[DONE]") break

                val chunk = runCatching {
                    json.decodeFromString<StreamChunk>(data)
                }.getOrElse {
                    error("fail to parse chunk: ${it.message}, data: $data")
                }
                chunk.choices.firstOrNull()?.delta?.content?.let {
                    send(it)
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}