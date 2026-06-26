package com.chargealarm.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesRepository(private val context: Context) {
    
    companion object {
        private val MASTER_ALARM_KEY = booleanPreferencesKey("master_alarm")
        private val HAS_SEEN_WELCOME_KEY = booleanPreferencesKey("has_seen_welcome")
        private val SELECTED_LIMITS_KEY = stringSetPreferencesKey("selected_limits")
        val CUSTOM_AUDIO_URI = stringPreferencesKey("custom_audio_uri")
    }

    val masterAlarmFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[MASTER_ALARM_KEY] ?: true
    }

    val hasSeenWelcomeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HAS_SEEN_WELCOME_KEY] ?: false
    }

    val selectedLimitsFlow: Flow<Set<Int>> = context.dataStore.data.map { preferences ->
        val defaultSet = setOf("20", "30", "40", "50", "60", "70", "80", "90", "100")
        (preferences[SELECTED_LIMITS_KEY] ?: defaultSet).map { it.toInt() }.toSet()
    }

    val customAudioUriFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[CUSTOM_AUDIO_URI]
    }

    suspend fun setMasterAlarm(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MASTER_ALARM_KEY] = enabled
        }
    }

    suspend fun setHasSeenWelcome(seen: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAS_SEEN_WELCOME_KEY] = seen
        }
    }

    suspend fun toggleLimit(limit: Int) {
        context.dataStore.edit { preferences ->
            val current = preferences[SELECTED_LIMITS_KEY] ?: setOf("20", "30", "40", "50", "60", "70", "80", "90", "100")
            val currentMutable = current.toMutableSet()
            if (currentMutable.contains(limit.toString())) {
                currentMutable.remove(limit.toString())
            } else {
                currentMutable.add(limit.toString())
            }
            preferences[SELECTED_LIMITS_KEY] = currentMutable
        }
    }

    suspend fun setCustomAudioUri(uri: String?) {
        context.dataStore.edit { preferences ->
            if (uri == null) {
                preferences.remove(CUSTOM_AUDIO_URI)
            } else {
                preferences[CUSTOM_AUDIO_URI] = uri
            }
        }
    }
}
