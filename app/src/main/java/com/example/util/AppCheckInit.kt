package com.example.util

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

private const val TAG = "AppCheckInit"

private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

data class AppCheckInitStatus(
    val initialized: Boolean = false,
    val buildType: String = if (BuildConfig.DEBUG) "debug" else "release",
    val projectId: String = "unknown",
    val packageName: String = "",
    val provider: String = "uninitialized",
    val tokenSource: String = "not_configured"
)

@Volatile
var lastAppCheckInitStatus: AppCheckInitStatus = AppCheckInitStatus()
    private set

fun configureAppCheck(context: Context, intent: Intent?): String {
    val buildType = if (BuildConfig.DEBUG) "debug" else "release"
    val packageName = context.packageName
    return try {
        val app = if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context) ?: FirebaseApp.getInstance()
        } else {
            FirebaseApp.getInstance()
        }
        val projectId = app.options.projectId.orEmpty().ifBlank { "unknown" }

        if (BuildConfig.DEBUG) {
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)
            val persistenceKey = app.persistenceKey
            val prefs = context.getSharedPreferences(
                "$DEBUG_STORE_PREFIX$persistenceKey",
                Context.MODE_PRIVATE
            )

            val tokenSource = if (!debugToken.isNullOrEmpty()) {
                // Synchronous commit() ensures the secret is written before DebugAppCheckProviderFactory reads it
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
                Log.d(TAG, "App Check debug secret applied from launch Intent [project=$projectId, pkg=$packageName].")
                "intent_debug_token"
            } else if (!prefs.getString(DEBUG_SECRET_KEY, null).isNullOrBlank()) {
                Log.d(TAG, "App Check using persisted debug secret from SharedPreferences [project=$projectId, pkg=$packageName].")
                "persisted_debug_token"
            } else {
                Log.w(
                    TAG,
                    "No Launch Intent debug token found [project=$projectId, pkg=$packageName]. " +
                        "On a physical device, check Logcat tag 'DebugAppCheckProvider' ('Enter this debug secret into the allow list...') " +
                        "and register that UUID in Firebase Console > App Check > Apps > Manage debug tokens."
                )
                "generated_or_default_debug_token"
            }

            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            lastAppCheckInitStatus = AppCheckInitStatus(
                initialized = true,
                buildType = buildType,
                projectId = projectId,
                packageName = packageName,
                provider = "DebugAppCheckProviderFactory",
                tokenSource = tokenSource
            )
            Log.d(TAG, "DebugAppCheckProviderFactory installed [project=$projectId, source=$tokenSource].")
            tokenSource
        } else {
            val resId = context.resources.getIdentifier(
                RECAPTCHA_SITE_KEY_RES,
                "string",
                packageName
            )
            if (resId != 0) {
                val siteKey = context.getString(resId)
                if (siteKey.isNotBlank()) {
                    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                    )
                    lastAppCheckInitStatus = AppCheckInitStatus(
                        initialized = true,
                        buildType = buildType,
                        projectId = projectId,
                        packageName = packageName,
                        provider = "RecaptchaAppCheckProviderFactory",
                        tokenSource = "recaptcha_configured"
                    )
                    Log.d(TAG, "RecaptchaAppCheckProviderFactory installed [project=$projectId, pkg=$packageName].")
                    "recaptcha_configured"
                } else {
                    lastAppCheckInitStatus = AppCheckInitStatus(
                        initialized = false,
                        buildType = buildType,
                        projectId = projectId,
                        packageName = packageName,
                        provider = "RecaptchaAppCheckProviderFactory",
                        tokenSource = "recaptcha_key_blank"
                    )
                    Log.e(TAG, "recaptcha_site_key resource string is blank [project=$projectId, pkg=$packageName].")
                    "recaptcha_key_blank"
                }
            } else {
                lastAppCheckInitStatus = AppCheckInitStatus(
                    initialized = false,
                    buildType = buildType,
                    projectId = projectId,
                    packageName = packageName,
                    provider = "RecaptchaAppCheckProviderFactory",
                    tokenSource = "recaptcha_key_missing"
                )
                Log.e(TAG, "recaptcha_site_key string resource not found in package [project=$projectId, pkg=$packageName].")
                "recaptcha_key_missing"
            }
        }
    } catch (e: Exception) {
        lastAppCheckInitStatus = AppCheckInitStatus(
            initialized = false,
            buildType = buildType,
            projectId = "unknown",
            packageName = packageName,
            provider = "error",
            tokenSource = "init_failed:${e.javaClass.simpleName}"
        )
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
        "init_failed:${e.javaClass.simpleName}"
    }
}

