package com.destywen.mydroid.data.remote

import com.destywen.mydroid.domain.AppLogger
import com.destywen.mydroid.data.local.ChatAgent
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
import io.ktor.http.ContentType
import io.ktor.http.contentType
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

    private val decoder = Json { ignoreUnknownKeys = true }

    // TODO: use Result instead of try catch
    suspend fun chat(history: List<Message>, config: ChatAgent): String = withContext(Dispatchers.IO) {
        val prompt = listOf(Message("system", config.systemPrompt)) + history
        try {
            val rawResponse = client.post(config.endpoint) {
                header("Authorization", "Bearer ${config.apiKey}")
                contentType(ContentType.Application.Json)
                setBody(ChatRequest(model = config.modelName, messages = prompt, stream = false))
            }
            val response = rawResponse.body<ChatResponse>()
            AppLogger.i("chat", "Token消耗数据：${response.usage}")

            response.choices.firstOrNull()?.message?.content ?: ""
        } catch (e: Exception) {
            AppLogger.e("chat", "请求失败：${e.message}")
            throw e
        }
    }

    // TODO: system prompt here
    fun streamChat(prompt: List<Message>, config: ChatAgent): Flow<String> = channelFlow {
        client.preparePost(config.endpoint) {
            header("Authorization", "Bearer ${config.apiKey}")
            contentType(ContentType.Application.Json)
            setBody(ChatRequest(model = config.modelName, messages = prompt, stream = true))
        }.execute { response ->
            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readLine() ?: break
                if (line.startsWith("data: ")) {
                    val data = line.removePrefix("data: ").trim()
                    if (data == "[DONE]") break
                    try {
                        val delta = decoder.decodeFromString<ChatStreamResponse>(data)
                            .choices.firstOrNull()?.delta?.content
                        if (delta != null) {
                            send(delta)
                        }
                    } catch (e: Exception) {
                        AppLogger.e("streamChat", "解析出错了: ${e.message}")
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}