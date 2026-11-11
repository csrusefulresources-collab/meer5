package com.baseerat.vision.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "baseerat_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_AUTO_SPEAK = booleanPreferencesKey("auto_speak")
        val KEY_DETAILED_DEFAULT = booleanPreferencesKey("detailed_default")
        val KEY_VIBRATE = booleanPreferencesKey("vibrate")
        val KEY_OFFLINE_ONLY = booleanPreferencesKey("offline_only")
        val KEY_SPEECH_RATE = floatPreferencesKey("speech_rate")
        val KEY_VOLUME_UP_SCREEN = booleanPreferencesKey("volume_up_screen")
        val KEY_VOLUME_DOWN_FOCUS = booleanPreferencesKey("volume_down_focus")
    }

    val language: Flow<String> = context.dataStore.data.map { it[KEY_LANGUAGE] ?: "ur" }
    val autoSpeak: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_SPEAK] ?: true }
    val detailedDefault: Flow<Boolean> = context.dataStore.data.map { it[KEY_DETAILED_DEFAULT] ?: false }
    val vibrate: Flow<Boolean> = context.dataStore.data.map { it[KEY_VIBRATE] ?: true }
    val offlineOnly: Flow<Boolean> = context.dataStore.data.map { it[KEY_OFFLINE_ONLY] ?: true }
    val speechRate: Flow<Float> = context.dataStore.data.map { it[KEY_SPEECH_RATE] ?: 1.0f }
    val volumeUpScreen: Flow<Boolean> = context.dataStore.data.map { it[KEY_VOLUME_UP_SCREEN] ?: true }
    val volumeDownFocus: Flow<Boolean> = context.dataStore.data.map { it[KEY_VOLUME_DOWN_FOCUS] ?: true }

    suspend fun setLanguage(value: String) {
        context.dataStore.edit { it[KEY_LANGUAGE] = value }
    }

    suspend fun setAutoSpeak(value: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_SPEAK] = value }
    }

    suspend fun setDetailedDefault(value: Boolean) {
        context.dataStore.edit { it[KEY_DETAILED_DEFAULT] = value }
    }

    suspend fun setVibrate(value: Boolean) {
        context.dataStore.edit { it[KEY_VIBRATE] = value }
    }

    suspend fun setOfflineOnly(value: Boolean) {
        context.dataStore.edit { it[KEY_OFFLINE_ONLY] = value }
    }

    suspend fun setSpeechRate(value: Float) {
        context.dataStore.edit { it[KEY_SPEECH_RATE] = value }
    }

    suspend fun setVolumeUpScreen(value: Boolean) {
        context.dataStore.edit { it[KEY_VOLUME_UP_SCREEN] = value }
    }

    suspend fun setVolumeDownFocus(value: Boolean) {
        context.dataStore.edit { it[KEY_VOLUME_DOWN_FOCUS] = value }
    }
}

