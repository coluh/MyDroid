package com.destywen.mydroid.data.remote

import com.destywen.mydroid.domain.AppLogger
import com.destywen.mydroid.data.local.ChatAgent
import com.destywen.mydroid.data.local.Role
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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

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

class AiChatService(private val client: HttpClient) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun chat(context: List<Message>, config: ChatAgent): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val prompt = listOf(Message(Role.SYSTEM, config.systemPrompt)) + context
            AppLogger.d(
                "chat",
                "request to ${config.endpoint}, model: ${config.modelName}, system prompt: ${config.systemPrompt.isNotBlank()}, with ${context.size} messages"
            )
            AppLogger.d("chat", "$prompt")
            val response = client.post(config.endpoint) {
                header(HttpHeaders.Authorization, "Bearer ${config.apiKey}")
                contentType(ContentType.Application.Json)
                setBody(ChatRequest(config.modelName, prompt, stream = false))
            }
            if (!response.status.isSuccess()) {
                val body = response.bodyAsText()
                val message = runCatching {
                    json.decodeFromString<ChatErrorWrapper>(body).error.message
                }.getOrElse {
                    AppLogger.w("chat", "fail to parse error body: ${it.message}, body: $body")
                    body
                }
                error("HTTP status ${response.status.value}, response $message")
            }

            val body = response.body<ChatResponse>()
            AppLogger.i("chat", "${body.usage}")
            body.choices.first().message.content
        }.onFailure {
            AppLogger.e("chat", it.message ?: "unknown error")
        }
    }

    fun chatStreaming(context: List<Message>, config: ChatAgent): Flow<String> = channelFlow {
        val prompt = listOf(Message(Role.SYSTEM, config.systemPrompt)) + context
        AppLogger.d(
            "chatStreaming",
            "request to ${config.endpoint}, model: ${config.modelName}, system prompt: ${config.systemPrompt.isNotBlank()}, with ${context.size} messages"
        )
        AppLogger.d("chatStreaming", "$prompt")
        client.preparePost(config.endpoint) {
            header(HttpHeaders.Authorization, "Bearer ${config.apiKey}")
            contentType(ContentType.Application.Json)
            setBody(ChatRequest(config.modelName, prompt, stream = true))
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