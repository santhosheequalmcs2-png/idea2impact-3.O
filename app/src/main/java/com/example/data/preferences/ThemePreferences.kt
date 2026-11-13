package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "idea2impact_prefs")

class ThemePreferences(private val context: Context) {
    private val darkThemeKey = booleanPreferencesKey("is_dark_theme")
    private val soundEnabledKey = booleanPreferencesKey("is_sound_enabled")
    private val onboardingCompletedKey = booleanPreferencesKey("has_completed_onboarding")

    val isDarkTheme: Flow<Boolean> = context.themeDataStore.data.map { prefs ->
        prefs[darkThemeKey] ?: false
    }

    val isSoundEnabled: Flow<Boolean> = context.themeDataStore.data.map { prefs ->
        prefs[soundEnabledKey] ?: true
    }

    val hasCompletedOnboarding: Flow<Boolean> = context.themeDataStore.data.map { prefs ->
        prefs[onboardingCompletedKey] ?: false
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[darkThemeKey] = enabled
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[soundEnabledKey] = enabled
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[onboardingCompletedKey] = completed
        }
    }
}
