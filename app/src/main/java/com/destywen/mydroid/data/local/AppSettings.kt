package com.destywen.mydroid.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "app_settings")

class AppSettings(private val context: Context) {

    private object Keys {
        // info
        val USERNAME = stringPreferencesKey("username")
        val USER_ID = longPreferencesKey("user_id") // used in chat

        // journal page
        val HIDE_TAGS = stringPreferencesKey("hide_tags")
        val JOURNAL_AGENT_ID = stringPreferencesKey("journal_agent_id")
        val VL_MODEL = stringPreferencesKey("vision_model_name")

        // agents
        val DEFAULT_ENDPOINT = stringPreferencesKey("default_endpoint")
        val DEFAULT_API_KEY = stringPreferencesKey("default_api_key")
    }

    val username = context.dataStore.data.map { it[Keys.USERNAME]?:"Destywen" }
    val userId = context.dataStore.data.map { it[Keys.USER_ID] }
    val hideTags = context.dataStore.data.map { it[Keys.HIDE_TAGS] }
    val journalAgentId = context.dataStore.data.map { it[Keys.JOURNAL_AGENT_ID] }
    val vlModel = context.dataStore.data.map { it[Keys.VL_MODEL] }
    val defaultEndpoint = context.dataStore.data.map { it[Keys.DEFAULT_ENDPOINT] }
    val defaultApiKey = context.dataStore.data.map { it[Keys.DEFAULT_API_KEY] }

    suspend fun updateUsername(name: String) = context.dataStore.edit { it[Keys.USERNAME] = name }

    suspend fun updateUserId(id: Long) = context.dataStore.edit { it[Keys.USER_ID] = id }

    suspend fun updateHideTags(tags: String) = context.dataStore.edit { it[Keys.HIDE_TAGS] = tags }

    suspend fun updateJournalAgentId(id: Long) = context.dataStore.edit { it[Keys.JOURNAL_AGENT_ID] = id.toString() }

    suspend fun updateVlModel(name: String) = context.dataStore.edit { it[Keys.VL_MODEL] = name }

    suspend fun updateDefaultEndpoint(value: String) = context.dataStore.edit { it[Keys.DEFAULT_ENDPOINT] = value }

    suspend fun updateDefaultApiKey(apiKey: String) = context.dataStore.edit { it[Keys.DEFAULT_API_KEY] = apiKey }
}