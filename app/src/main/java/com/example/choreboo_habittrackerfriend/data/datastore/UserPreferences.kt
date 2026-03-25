package com.example.choreboo_habittrackerfriend.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "choreboo_preferences")

@Singleton
class UserPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val TOTAL_POINTS = intPreferencesKey("total_points")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val THEME_MODE = stringPreferencesKey("theme_mode") // "system", "light", "dark"
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
    }

    val totalPoints: Flow<Int> = dataStore.data.map { it[TOTAL_POINTS] ?: 0 }

    val reminderEnabled: Flow<Boolean> = dataStore.data.map { it[REMINDER_ENABLED] ?: false }

    val reminderHour: Flow<Int> = dataStore.data.map { it[REMINDER_HOUR] ?: 9 }

    val reminderMinute: Flow<Int> = dataStore.data.map { it[REMINDER_MINUTE] ?: 0 }

    val themeMode: Flow<String> = dataStore.data.map { it[THEME_MODE] ?: "system" }

    val onboardingComplete: Flow<Boolean> = dataStore.data.map { it[ONBOARDING_COMPLETE] ?: false }

    val soundEnabled: Flow<Boolean> = dataStore.data.map { it[SOUND_ENABLED] ?: true }

    suspend fun addPoints(amount: Int) {
        dataStore.edit { prefs ->
            val current = prefs[TOTAL_POINTS] ?: 0
            prefs[TOTAL_POINTS] = current + amount
        }
    }

    suspend fun deductPoints(amount: Int): Boolean {
        var success = false
        dataStore.edit { prefs ->
            val current = prefs[TOTAL_POINTS] ?: 0
            if (current >= amount) {
                prefs[TOTAL_POINTS] = current - amount
                success = true
            }
        }
        return success
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[REMINDER_ENABLED] = enabled }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        dataStore.edit {
            it[REMINDER_HOUR] = hour
            it[REMINDER_MINUTE] = minute
        }
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[THEME_MODE] = mode }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { it[ONBOARDING_COMPLETE] = complete }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { it[SOUND_ENABLED] = enabled }
    }
}

