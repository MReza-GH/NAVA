package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nava_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val LANGUAGE = stringPreferencesKey("app_language")
        val THEME = stringPreferencesKey("app_theme")
        val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        val REPEAT_MODE = intPreferencesKey("repeat_mode")
        val JAMENDO_CLIENT_ID = stringPreferencesKey("jamendo_client_id")
        val LAST_TRACK_ID = stringPreferencesKey("last_track_id")
        val LAST_POSITION_MS = longPreferencesKey("last_position_ms")
    }

    // Default language is Persian ("fa") as required
    val languageFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.LANGUAGE] ?: "fa"
        }

    // Default theme is Dark ("dark") as required
    val themeFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.THEME] ?: "dark"
        }

    val shuffleFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.SHUFFLE_ENABLED] ?: false
        }

    val repeatModeFlow: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.REPEAT_MODE] ?: 0
        }

    val jamendoClientIdFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.JAMENDO_CLIENT_ID] ?: ""
        }

    val lastTrackIdFlow: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.LAST_TRACK_ID]
        }

    val lastPositionFlow: Flow<Long> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.LAST_POSITION_MS] ?: 0L
        }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = language
        }
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme
        }
    }

    suspend fun setShuffleEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHUFFLE_ENABLED] = enabled
        }
    }

    suspend fun setRepeatMode(mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REPEAT_MODE] = mode
        }
    }

    suspend fun setJamendoClientId(clientId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.JAMENDO_CLIENT_ID] = clientId
        }
    }

    suspend fun setLastPlaybackState(trackId: String, positionMs: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_TRACK_ID] = trackId
            preferences[PreferencesKeys.LAST_POSITION_MS] = positionMs
        }
    }
}
