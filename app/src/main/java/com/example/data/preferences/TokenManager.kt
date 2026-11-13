package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(context: Context) {
    private val appContext = context.applicationContext

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w(TAG, "Falling back to standard SharedPreferences in restricted environment", e)
            appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun getToken(): String? = prefs.getString(KEY_JWT_TOKEN, null)?.takeIf { it.isNotBlank() }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_JWT_TOKEN, token.trim()).apply()
    }

    fun clearToken() {
        prefs.edit().remove(KEY_JWT_TOKEN).apply()
    }

    fun getCustomApiKey(): String? = prefs.getString(KEY_GEMINI_CUSTOM, null)?.takeIf { it.isNotBlank() }

    fun saveCustomApiKey(apiKey: String?) {
        val clean = apiKey?.trim()
        if (clean.isNullOrBlank()) {
            prefs.edit().remove(KEY_GEMINI_CUSTOM).apply()
        } else {
            prefs.edit().putString(KEY_GEMINI_CUSTOM, clean).apply()
        }
    }

    private companion object {
        const val TAG = "TokenManager"
        const val PREFS_NAME = "idea2impact_encrypted_prefs"
        const val KEY_JWT_TOKEN = "auth_bearer_token"
        const val KEY_GEMINI_CUSTOM = "custom_google_ai_key"
    }
}
