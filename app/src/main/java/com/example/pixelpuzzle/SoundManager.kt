package com.example.pixelpuzzle

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper

/**
 * Manages all sound effects and background music for the game
 * Uses system notification sounds and default ringtones for a pleasant experience
 */
class SoundManager private constructor(private val context: Context) {

    private var soundPool: SoundPool? = null
    private var backgroundMusic: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isMusicPlaying = false

    companion object {
        @Volatile
        private var instance: SoundManager? = null
        private const val TAG = "SoundManager"

        fun getInstance(context: Context): SoundManager {
            return instance ?: synchronized(this) {
                instance ?: SoundManager(context.applicationContext).also { instance = it }
            }
        }
    }

    init {
        initializeSoundPool()
    }

    private fun initializeSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        DebugConfig.d(TAG, "SoundPool initialized")
    }

    /**
     * Get system notification sound URI based on effect type
     */
    private fun getSystemSoundUri(effect: SoundEffect): Uri? {
        return try {
            when (effect) {
                SoundEffect.POP -> {
                    // Soft notification sound
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }
                SoundEffect.MERGE -> {
                    // Message sent sound
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }
                SoundEffect.COMPLETE -> {
                    // Success/achievement sound
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }
                SoundEffect.ERROR -> {
                    // System error/warning sound
                    Uri.parse("content://settings/system/notification_sound")
                }
                SoundEffect.UNLOCK -> {
                    // Unlock/achievement sound
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }
                SoundEffect.COIN -> {
                    // Coin/points sound
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }
            }
        } catch (e: Exception) {
            DebugConfig.e(TAG, "Error getting system sound URI", e)
            null
        }
    }

    /**
     * Play a sound effect using system sounds
     */
    fun playSound(effect: SoundEffect, volume: Float = 1.0f) {
        if (!GamePreferences.isSoundEnabled(context)) {
            return
        }

        try {
            val soundUri = getSystemSoundUri(effect)
            if (soundUri != null) {
                val mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(context, soundUri)
                    setVolume(volume * 0.3f, volume * 0.3f) // Reduce volume to 30%
                    setOnCompletionListener { mp ->
                        mp.release()
                    }
                    setOnErrorListener { mp, what, extra ->
                        DebugConfig.e(TAG, "MediaPlayer error: what=$what, extra=$extra", null)
                        mp.release()
                        true
                    }
                    prepareAsync()
                    setOnPreparedListener { mp ->
                        mp.start()
                    }
                }

                DebugConfig.d(TAG, "Playing sound: ${effect.name}")
            }
        } catch (e: Exception) {
            DebugConfig.e(TAG, "Error playing sound: ${effect.name}", e)
        }
    }

    /**
     * Play background music using system ringtone or alarm
     */
    fun playBackgroundMusic() {
        if (!GamePreferences.isMusicEnabled(context)) {
            return
        }

        try {
            stopBackgroundMusic()

            // Use default alarm sound for background music (usually pleasant and loopable)
            val musicUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            if (musicUri != null) {
                backgroundMusic = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource(context, musicUri)
                    isLooping = true
                    setVolume(0.15f, 0.15f) // Very low volume for background music
                    setOnErrorListener { mp, what, extra ->
                        DebugConfig.e(TAG, "Background music error: what=$what, extra=$extra", null)
                        mp.release()
                        true
                    }
                    prepareAsync()
                    setOnPreparedListener { mp ->
                        mp.start()
                        isMusicPlaying = true
                    }
                }

                DebugConfig.d(TAG, "Background music started")
            } else {
                DebugConfig.w(TAG, "No system music URI available")
            }
        } catch (e: Exception) {
            DebugConfig.e(TAG, "Error playing background music", e)
        }
    }

    /**
     * Stop background music
     */
    fun stopBackgroundMusic() {
        try {
            isMusicPlaying = false

            backgroundMusic?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            backgroundMusic = null
            DebugConfig.d(TAG, "Background music stopped")
        } catch (e: Exception) {
            DebugConfig.e(TAG, "Error stopping background music", e)
        }
    }

    /**
     * Pause background music
     */
    fun pauseBackgroundMusic() {
        try {
            isMusicPlaying = false
            backgroundMusic?.pause()
        } catch (e: Exception) {
            DebugConfig.e(TAG, "Error pausing background music", e)
        }
    }

    /**
     * Resume background music
     */
    fun resumeBackgroundMusic() {
        if (GamePreferences.isMusicEnabled(context)) {
            try {
                if (backgroundMusic != null && !backgroundMusic!!.isPlaying) {
                    backgroundMusic?.start()
                    isMusicPlaying = true
                } else {
                    playBackgroundMusic()
                }
            } catch (e: Exception) {
                DebugConfig.e(TAG, "Error resuming background music", e)
            }
        }
    }

    /**
     * Release all resources
     */
    fun release() {
        try {
            stopBackgroundMusic()
            handler.removeCallbacksAndMessages(null)
            soundPool?.release()
            soundPool = null
            DebugConfig.d(TAG, "Resources released")
        } catch (e: Exception) {
            DebugConfig.e(TAG, "Error releasing resources", e)
        }
    }
}

/**
 * Available sound effects in the game
 */
enum class SoundEffect {
    POP,        // When a piece is placed
    MERGE,      // When pieces merge
    COMPLETE,   // When puzzle is solved
    ERROR,      // Invalid move
    UNLOCK,     // New level unlocked
    COIN        // Points earned
}