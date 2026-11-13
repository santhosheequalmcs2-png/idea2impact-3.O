package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import com.example.R

object StartupSoundPlayer {
    private const val TAG = "StartupSoundPlayer"
    private const val PREFS_NAME = "idea2impact_sound_prefs"
    private const val KEY_SOUND_ENABLED = "sound_enabled"

    @Volatile
    private var soundPool: SoundPool? = null
    @Volatile
    private var tapSoundId: Int = 0
    @Volatile
    private var successSoundId: Int = 0
    @Volatile
    private var isTapLoaded = false
    @Volatile
    private var isSuccessLoaded = false
    @Volatile
    private var cachedSoundEnabled: Boolean? = null

    fun isSoundEnabled(context: Context): Boolean {
        cachedSoundEnabled?.let { return it }
        val loaded = runCatching {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_SOUND_ENABLED, true)
        }.getOrDefault(true)
        cachedSoundEnabled = loaded
        return loaded
    }

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        cachedSoundEnabled = enabled
        runCatching {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_SOUND_ENABLED, enabled)
                .apply()
        }
    }

    fun warmup(context: Context) {
        if (soundPool != null) return
        val appContext = context.applicationContext
        runCatching {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val pool = SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(attrs)
                .build()

            pool.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0) {
                    if (sampleId == tapSoundId) isTapLoaded = true
                    if (sampleId == successSoundId) isSuccessLoaded = true
                }
            }
            tapSoundId = pool.load(appContext, R.raw.sfx_ui_tap, 1)
            successSoundId = pool.load(appContext, R.raw.sfx_success_chime, 1)
            soundPool = pool
        }.onFailure { e ->
            Log.w(TAG, "SoundPool warmup skipped: ${e.message}")
        }
    }

    fun playAppOpenChime(context: Context) {
        val appContext = context.applicationContext
        warmup(appContext)
        if (!canPlayAudio(appContext)) return

        runCatching {
            val afd = appContext.resources.openRawResourceFd(R.raw.app_startup_chime) ?: return
            val player = MediaPlayer()
            try {
                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                player.setVolume(0.7f, 0.7f)
                player.setOnPreparedListener { mp ->
                    runCatching { mp.start() }
                        .onFailure { mp.release() }
                }
                player.setOnCompletionListener { mp ->
                    runCatching { mp.release() }
                }
                player.setOnErrorListener { mp, _, _ ->
                    runCatching { mp.release() }
                    true
                }
                player.prepareAsync()
            } catch (e: Exception) {
                runCatching { afd.close() }
                runCatching { player.release() }
                throw e
            }
        }.onFailure { e ->
            Log.w(TAG, "Unable to play app startup chime: ${e.message}")
        }
    }

    fun playTap(context: Context) {
        val appContext = context.applicationContext
        if (!canPlayAudio(appContext)) return
        val pool = soundPool ?: run {
            warmup(appContext)
            soundPool
        } ?: return
        if (isTapLoaded && tapSoundId != 0) {
            runCatching { pool.play(tapSoundId, 0.45f, 0.45f, 1, 0, 1.0f) }
        }
    }

    fun playSuccess(context: Context) {
        val appContext = context.applicationContext
        if (!canPlayAudio(appContext)) return
        val pool = soundPool ?: run {
            warmup(appContext)
            soundPool
        } ?: return
        if (isSuccessLoaded && successSoundId != 0) {
            runCatching { pool.play(successSoundId, 0.55f, 0.55f, 1, 0, 1.0f) }
        }
    }

    private fun canPlayAudio(context: Context): Boolean {
        if (!isSoundEnabled(context)) return false
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        return audioManager == null || audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL
    }
}
