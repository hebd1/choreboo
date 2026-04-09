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
         val TOTAL_LIFETIME_XP = intPreferencesKey("total_lifetime_xp")
         val THEME_MODE = stringPreferencesKey("theme_mode") // "system", "light", "dark"
         val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
         val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
         val PROFILE_PHOTO_URI = stringPreferencesKey("profile_photo_uri") // Custom profile photo path, null = use Google photo
         val IS_PREMIUM = booleanPreferencesKey("is_premium") // Local cache of Play Billing subscription status
     }

     val totalPoints: Flow<Int> = dataStore.data.map { it[TOTAL_POINTS] ?: 0 }

     val totalLifetimeXp: Flow<Int> = dataStore.data.map { it[TOTAL_LIFETIME_XP] ?: 0 }

     val themeMode: Flow<String> = dataStore.data.map { it[THEME_MODE] ?: "system" }

    val onboardingComplete: Flow<Boolean> = dataStore.data.map { it[ONBOARDING_COMPLETE] ?: false }

    val soundEnabled: Flow<Boolean> = dataStore.data.map { it[SOUND_ENABLED] ?: true }

    val profilePhotoUri: Flow<String?> = dataStore.data.map { it[PROFILE_PHOTO_URI] }

    /** Local cache of Play Billing subscription status — written by BillingRepository on every verification. */
    val isPremium: Flow<Boolean> = dataStore.data.map { it[IS_PREMIUM] ?: false }

    suspend fun addPoints(amount: Int) {
        dataStore.edit { prefs ->
            val current = prefs[TOTAL_POINTS] ?: 0
            prefs[TOTAL_POINTS] = current + amount
        }
    }

    suspend fun addLifetimeXp(amount: Int) {
        dataStore.edit { prefs ->
            val current = prefs[TOTAL_LIFETIME_XP] ?: 0
            prefs[TOTAL_LIFETIME_XP] = current + amount
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

    /** Directly set totalPoints — used for cloud-to-local sync (max wins). */
    suspend fun setPoints(amount: Int) {
        dataStore.edit { it[TOTAL_POINTS] = amount }
    }

    /** Directly set totalLifetimeXp — used for cloud-to-local sync (max wins). */
    suspend fun setLifetimeXp(amount: Int) {
        dataStore.edit { it[TOTAL_LIFETIME_XP] = amount }
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

    /** Update cached premium status — called by BillingRepository after every subscription verification. */
    suspend fun setIsPremium(premium: Boolean) {
        dataStore.edit { it[IS_PREMIUM] = premium }
    }

    /** Clear all preferences — used for sign-out data cleanup. */
    suspend fun clearAllData() {
        dataStore.edit { it.clear() }
    }
}

