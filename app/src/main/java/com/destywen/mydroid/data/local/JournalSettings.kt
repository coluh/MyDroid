package com.destywen.mydroid.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "journal_settings")

class JournalSettings(private val context: Context) {
    private object Keys {
        val HIDE_TAGS = stringPreferencesKey("hide_tags")
    }

    val hideTags: Flow<String?> = context.dataStore.data.map { it[Keys.HIDE_TAGS] }
    suspend fun updateHideTags(tags: String) {
        context.dataStore.edit { it[Keys.HIDE_TAGS] = tags }
    }
}