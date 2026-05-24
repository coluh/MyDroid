package com.destywen.mydroid.data.local

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "app_settings")

data class AppConfig(
    val username: String? = null,
    val userId: Long? = null,
    val hideTags: String? = null,
    val journalPrompt: String? = null,
    val defaultEndpoint: String? = null,
    val defaultApiKey: String? = null,
    val defaultModel: String? = null,
    val defaultVisionModel: String? = null,
)

internal object Keys {
    // info
    val USERNAME = stringPreferencesKey("username")
    val USER_ID = longPreferencesKey("user_id") // used in chat

    // journal page
    val HIDE_TAGS = stringPreferencesKey("journal_hide_tags")
    val JOURNAL_PROMPT = stringPreferencesKey("journal_reply_prompt")

    // agents
    val DEFAULT_ENDPOINT = stringPreferencesKey("default_endpoint")
    val DEFAULT_API_KEY = stringPreferencesKey("default_api_key")
    val DEFAULT_MODEL = stringPreferencesKey("default_model")
    val DEFAULT_VISION_MODEL = stringPreferencesKey("default_vision_model")
}

class AppSettings(private val context: Context) {

    val config: Flow<AppConfig> = context.dataStore.data.map { prefs ->
        AppConfig(
            username = prefs[Keys.USERNAME],
            userId = prefs[Keys.USER_ID],
            hideTags = prefs[Keys.HIDE_TAGS],
            journalPrompt = prefs[Keys.JOURNAL_PROMPT],
            defaultEndpoint = prefs[Keys.DEFAULT_ENDPOINT],
            defaultApiKey = prefs[Keys.DEFAULT_API_KEY],
            defaultModel = prefs[Keys.DEFAULT_MODEL],
            defaultVisionModel = prefs[Keys.DEFAULT_VISION_MODEL],
        )
    }

    suspend fun update(block: (MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}