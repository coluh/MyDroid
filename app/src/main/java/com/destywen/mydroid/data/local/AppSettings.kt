package com.destywen.mydroid.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
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

private val Context.dataStore by preferencesDataStore(name = "app_settings")

class AppSettings(private val context: Context) {

    private object Keys {
        // journal page
        val HIDE_TAGS = stringPreferencesKey("hide_tags")
        val JOURNAL_AGENT_ID = stringPreferencesKey("journal_agent_id")

        // chat page
        val AGENTS_JSON = stringPreferencesKey("agents_json")
        val SELECTED_AGENT_ID = stringPreferencesKey("selected_agent_id")
    }

    val hideTagsFlow = context.dataStore.data.map { it[Keys.HIDE_TAGS] }
    val journalAgentIdFlow = context.dataStore.data.map { it[Keys.JOURNAL_AGENT_ID] }
    val agentsFlow = context.dataStore.data.map { prefs ->
        prefs[Keys.AGENTS_JSON]?.let {
            runCatching {
                Json.decodeFromString<List<ChatAgent>>(it)
            }.getOrDefault(emptyList())
        } ?: emptyList()
    }
    val selectedAgentIdFlow = context.dataStore.data.map { it[Keys.SELECTED_AGENT_ID] }

    suspend fun updateHideTags(tags: String) {
        context.dataStore.edit { it[Keys.HIDE_TAGS] = tags }
    }

    suspend fun updateJournalAgentId(id: String) {
        context.dataStore.edit { it[Keys.JOURNAL_AGENT_ID] = id }
    }

    suspend fun updateAgents(list: List<ChatAgent>) {
        context.dataStore.edit { it[Keys.AGENTS_JSON] = Json.encodeToString(list) }
    }

    suspend fun selectAgent(id: String) {
        context.dataStore.edit { it[Keys.SELECTED_AGENT_ID] = id }
    }
}