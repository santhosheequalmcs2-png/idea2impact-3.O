package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "idea2impact_prefs")
private const val FAST_THEME_PREFS = "idea2impact_theme_cache"
private const val KEY_FAST_DARK_THEME = "is_dark_theme_fast"

class ThemePreferences(private val context: Context) {
    private val appContext = context.applicationContext
    private val fastPrefs = appContext.getSharedPreferences(FAST_THEME_PREFS, Context.MODE_PRIVATE)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val darkThemeKey = booleanPreferencesKey("is_dark_theme")
    private val soundEnabledKey = booleanPreferencesKey("is_sound_enabled")
    private val onboardingCompletedKey = booleanPreferencesKey("has_completed_onboarding")

    private val _isDarkTheme = MutableStateFlow(
        fastPrefs.getBoolean(KEY_FAST_DARK_THEME, false)
    )
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    @Volatile
    private var hasLocalDarkThemeChange = false

    init {
        if (!fastPrefs.contains(KEY_FAST_DARK_THEME)) {
            ioScope.launch {
                val persisted = runCatching {
                    appContext.themeDataStore.data.first()[darkThemeKey]
                }.getOrNull()
                if (persisted != null && !hasLocalDarkThemeChange) {
                    _isDarkTheme.value = persisted
                    fastPrefs.edit().putBoolean(KEY_FAST_DARK_THEME, persisted).apply()
                }
            }
        }
    }

    val isSoundEnabled: Flow<Boolean> = appContext.themeDataStore.data.map { prefs ->
        prefs[soundEnabledKey] ?: true
    }

    val hasCompletedOnboarding: Flow<Boolean> = appContext.themeDataStore.data.map { prefs ->
        prefs[onboardingCompletedKey] ?: false
    }

    fun setDarkThemeImmediate(enabled: Boolean) {
        hasLocalDarkThemeChange = true
        _isDarkTheme.value = enabled
        fastPrefs.edit().putBoolean(KEY_FAST_DARK_THEME, enabled).apply()
    }

    suspend fun persistDarkTheme(enabled: Boolean) {
        appContext.themeDataStore.edit { prefs ->
            prefs[darkThemeKey] = enabled
        }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        setDarkThemeImmediate(enabled)
        persistDarkTheme(enabled)
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        appContext.themeDataStore.edit { prefs ->
            prefs[soundEnabledKey] = enabled
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        appContext.themeDataStore.edit { prefs ->
            prefs[onboardingCompletedKey] = completed
        }
    }
}
