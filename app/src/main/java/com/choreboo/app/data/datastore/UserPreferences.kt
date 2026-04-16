package com.choreboo.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
        val TOTAL_LIFETIME_XP = intPreferencesKey("total_lifetime_xp")
        val THEME_MODE = stringPreferencesKey("theme_mode") // "system", "light", "dark"
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val PROFILE_PHOTO_URI = stringPreferencesKey("profile_photo_uri") // Custom profile photo path, null = use Google photo
        val LAST_MOOD_NOTIFICATION_TIME = longPreferencesKey("last_mood_notification_time") // Timestamp of last pet mood notification (0 = never)
    }

    val totalPoints: Flow<Int> = dataStore.data.map { it[TOTAL_POINTS] ?: 0 }

    val totalLifetimeXp: Flow<Int> = dataStore.data.map { it[TOTAL_LIFETIME_XP] ?: 0 }

    val themeMode: Flow<String> = dataStore.data.map { it[THEME_MODE] ?: "system" }

    val onboardingComplete: Flow<Boolean> = dataStore.data.map { it[ONBOARDING_COMPLETE] ?: false }

    val soundEnabled: Flow<Boolean> = dataStore.data.map { it[SOUND_ENABLED] ?: true }

    val profilePhotoUri: Flow<String?> = dataStore.data.map { it[PROFILE_PHOTO_URI] }

    val lastMoodNotificationTime: Flow<Long> = dataStore.data.map { it[LAST_MOOD_NOTIFICATION_TIME] ?: 0L }
    suspend fun addPoints(amount: Int): Int {
        var newValue = 0
        dataStore.edit { prefs ->
            val current = prefs[TOTAL_POINTS] ?: 0
            newValue = current + amount
            prefs[TOTAL_POINTS] = newValue
        }
        return newValue
    }

    suspend fun addLifetimeXp(amount: Int): Int {
        var newValue = 0
        dataStore.edit { prefs ->
            val current = prefs[TOTAL_LIFETIME_XP] ?: 0
            newValue = current + amount
            prefs[TOTAL_LIFETIME_XP] = newValue
        }
        return newValue
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

    /**
     * Atomically set both totalPoints and totalLifetimeXp in a single DataStore transaction.
     * Prefer this over calling [setPoints] and [setLifetimeXp] separately to prevent the two
     * values from diverging if the app crashes between the two separate edits.
     */
    suspend fun setPointsAndLifetimeXp(points: Int, lifetimeXp: Int) {
        dataStore.edit { prefs ->
            prefs[TOTAL_POINTS] = points
            prefs[TOTAL_LIFETIME_XP] = lifetimeXp
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

    suspend fun setProfilePhotoUri(uri: String?) {
        dataStore.edit { prefs ->
            if (uri == null) {
                prefs.remove(PROFILE_PHOTO_URI)
            } else {
                prefs[PROFILE_PHOTO_URI] = uri
            }
        }
    }

    suspend fun setLastMoodNotificationTime(timestamp: Long) {
        dataStore.edit { it[LAST_MOOD_NOTIFICATION_TIME] = timestamp }
    }

    /** Clear all preferences — used for sign-out data cleanup. */
    suspend fun clearAllData() {
        dataStore.edit { it.clear() }
    }
}
