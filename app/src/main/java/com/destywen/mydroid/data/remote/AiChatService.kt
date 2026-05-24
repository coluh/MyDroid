package com.destywen.mydroid.data.remote

import android.util.Base64
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

data class ApiConfig(
    val endpoint: String? = null,
    val apiKey: String? = null,
    val model: String? = null,
    val visionModel: String? = null,
    val systemPrompt: String = "You are a helpful assistant.",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val topP: Float = 0.9f,
)

class AiChatService(private val client: HttpClient, settings: AppSettings) {

    private val json = Json { ignoreUnknownKeys = true }
    private val defaultConfig = settings.config

    suspend fun callLlm(context: List<ApiMessage>, config: ApiConfig, image: File? = null): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val default = defaultConfig.first()
                val endpoint = config.endpoint ?: default.defaultEndpoint ?: error("endpoint not set")
                val apiKey = config.apiKey ?: default.defaultApiKey ?: error("apiKey not set")
                val model = if (image == null) {
                    config.model ?: default.defaultModel ?: error("model not set")
                } else {
                    config.visionModel ?: default.defaultVisionModel ?: error("vision model not set")
                }
                val messages = if (image == null) {
                    listOf(ApiMessage(Role.SYSTEM, config.systemPrompt)) + context
                } else emptyList()
                val visionMessages = if (image != null) {
                    val imageBase64 = FileInputStream(image).use { inputStream ->
                        val bytes = inputStream.readBytes()
                        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        "data:image/${image.extension};base64,$base64"
                    }
                    val contentImg = ContentVision(type = "image_url", imageUrl = ContentImage(imageBase64))
                    val contentTxt =
                        ContentVision(type = "text", text = config.systemPrompt + "\n\n" + context[0].content)
                    val messageFirst = MessageVision(Role.USER, listOf(contentImg, contentTxt))
                    listOf(messageFirst) + context.drop(1).map {
                        MessageVision(it.role, listOf(ContentVision(type = "text", text = it.content)))
                    }
                } else emptyList()

                val response = client.post(endpoint) {
                    header(HttpHeaders.Authorization, "Bearer $apiKey")
                    contentType(ContentType.Application.Json)
                    setBody(
                        if (image == null) {
                            ChatRequest(
                                model = model,
                                messages = messages,
                                stream = false,
                                temperature = config.temperature.toDouble(),
                                topP = config.topP.toDouble(),
                                maxTokens = config.maxTokens,
                                enableThinking = false,
                            )
                        } else {
                            ChatRequestVision(
                                model = model,
                                messages = visionMessages,
                                stream = false,
                                temperature = config.temperature.toDouble(),
                                topP = config.topP.toDouble(),
                                maxTokens = config.maxTokens,
                                enableThinking = false,
                            )
                        }
                    )
                }

                if (!response.status.isSuccess()) {
                    val body = response.bodyAsText()
                    val message = runCatching { json.decodeFromString<ChatErrorWrapper>(body).error.message }
                        .getOrElse { body }
                    error("status ${response.status.value}, response: $message")
                }

                response.body<ChatResponse>().choices[0].message.content
            }.onFailure {
                AppLogger.e("chat", it.message ?: "unknown error")
            }
        }

    fun streamLlm(context: List<ApiMessage>, config: ApiConfig): Flow<String> = channelFlow {
        val default = defaultConfig.first()
        val endpoint = config.endpoint ?: default.defaultEndpoint ?: error("endpoint not set")
        val apiKey = config.apiKey ?: default.defaultApiKey ?: error("apiKey not set")
        val model = config.model ?: default.defaultModel ?: error("model not set")
        client.preparePost(endpoint) {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(
                ChatRequest(
                    model = model,
                    messages = listOf(ApiMessage(Role.SYSTEM, config.systemPrompt)) + context,
                    stream = true,
                    temperature = config.temperature.toDouble(),
                    topP = config.topP.toDouble(),
                    maxTokens = config.maxTokens,
                    enableThinking = false,
                )
            )
        }.execute { response ->
            if (!response.status.isSuccess()) {
                val body = response.bodyAsText()
                val message = runCatching { json.decodeFromString<ChatErrorWrapper>(body).error.message }
                    .getOrElse { body }
                error("status ${response.status.value}, response: $message")
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