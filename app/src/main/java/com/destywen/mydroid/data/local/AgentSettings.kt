package com.destywen.mydroid.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class ChatAgent(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val endpoint: String = "",
    val apiKey: String = "",
    val modelName: String = "",
    val systemPrompt: String = ""
)

private val Context.dataStore by preferencesDataStore(name = "agent_settings")

class AgentSettings(private val context: Context) {
    private object Keys {
        val AGENTS_JSON = stringPreferencesKey("agents_json")
        val SELECTED_AGENT_ID = stringPreferencesKey("selected_json_id")
    }

    val agentsFlow: Flow<List<ChatAgent>> = context.dataStore.data.map { prefs ->
        val json = prefs[Keys.AGENTS_JSON] ?: "[]"
        try {
            Json.decodeFromString<List<ChatAgent>>(json)
        } catch (_: Exception) {
            emptyList()
        }
    }
    val selectedAgentIdFlow: Flow<String?> = context.dataStore.data.map { it[Keys.SELECTED_AGENT_ID] }

    suspend fun saveAgents(agents: List<ChatAgent>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AGENTS_JSON] = Json.encodeToString(agents)
        }
    }
    suspend fun selectAgent(id: String) {
        context.dataStore.edit { it[Keys.SELECTED_AGENT_ID] = id }
    }
}