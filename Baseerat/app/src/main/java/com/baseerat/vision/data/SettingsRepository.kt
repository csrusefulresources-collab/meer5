package com.baseerat.vision.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "baseerat_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")          // "en" or "ur"
        val AUTO_SPEAK = booleanPreferencesKey("auto_speak")
        val DETAILED_DEFAULT = booleanPreferencesKey("detailed_default")
        val VIBRATE = booleanPreferencesKey("vibrate")
        val OFFLINE_ONLY = booleanPreferencesKey("offline_only")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
    }

    val language: Flow<String> = context.dataStore.data.map { it[Keys.LANGUAGE] ?: "en" }
    val autoSpeak: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_SPEAK] ?: true }
    val detailedDefault: Flow<Boolean> = context.dataStore.data.map { it[Keys.DETAILED_DEFAULT] ?: false }
    val vibrate: Flow<Boolean> = context.dataStore.data.map { it[Keys.VIBRATE] ?: true }
    val offlineOnly: Flow<Boolean> = context.dataStore.data.map { it[Keys.OFFLINE_ONLY] ?: true }
    val speechRate: Flow<Float> = context.dataStore.data.map { it[Keys.SPEECH_RATE] ?: 1.0f }

    suspend fun setLanguage(code: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = code }
    }

    suspend fun setAutoSpeak(value: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_SPEAK] = value }
    }

    suspend fun setDetailedDefault(value: Boolean) {
        context.dataStore.edit { it[Keys.DETAILED_DEFAULT] = value }
    }

    suspend fun setVibrate(value: Boolean) {
        context.dataStore.edit { it[Keys.VIBRATE] = value }
    }

    suspend fun setOfflineOnly(value: Boolean) {
        context.dataStore.edit { it[Keys.OFFLINE_ONLY] = value }
    }

    suspend fun setSpeechRate(value: Float) {
        context.dataStore.edit { it[Keys.SPEECH_RATE] = value.coerceIn(0.5f, 2.0f) }
    }
}
