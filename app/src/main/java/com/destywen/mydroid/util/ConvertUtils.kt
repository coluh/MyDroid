package com.destywen.mydroid.util

import com.destywen.mydroid.data.local.LlmConfigEntity
import com.destywen.mydroid.data.remote.ApiConfig

fun LlmConfigEntity.toRequestConfig() = ApiConfig(
    endpoint = endpoint,
    apiKey = apiKey,
    model = model,
    systemPrompt = systemPrompt,
    temperature = temperature,
    maxTokens = maxTokens,
    topP = topP,
)