package com.example.pixelpuzzle

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.media.MediaPlayer
import androidx.annotation.RawRes

/**
 * Manages all sound effects and background music for the game
 * Uses SoundPool for short effects and MediaPlayer for background music
 */
class SoundManager private constructor(private val context: Context) {

    private var soundPool: SoundPool? = null
    private var backgroundMusic: MediaPlayer? = null
    private val soundMap = mutableMapOf<SoundEffect, Int>()

    companion object {
        @Volatile
        private var instance: SoundManager? = null

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

        // Note: Sound files need to be added to res/raw folder
        // For now, we'll use system sounds or you can add custom sounds later
        // loadSound(SoundEffect.POP, R.raw.pop_sound)
        // loadSound(SoundEffect.MERGE, R.raw.merge_sound)
        // loadSound(SoundEffect.COMPLETE, R.raw.complete_sound)
        // loadSound(SoundEffect.ERROR, R.raw.error_sound)
    }

    private fun loadSound(effect: SoundEffect, @RawRes resourceId: Int) {
        try {
            val soundId = soundPool?.load(context, resourceId, 1)
            if (soundId != null && soundId > 0) {
                soundMap[effect] = soundId
                DebugConfig.d("SoundManager", "Loaded sound: ${effect.name}")
            }
        } catch (e: Exception) {
            DebugConfig.e("SoundManager", "Error loading sound: ${effect.name}", e)
        }
    }

    /**
     * Play a sound effect
     */
    fun playSound(effect: SoundEffect, volume: Float = 1.0f) {
        if (!GamePreferences.isSoundEnabled(context)) {
            return
        }

        val soundId = soundMap[effect]
        if (soundId != null) {
            try {
                soundPool?.play(soundId, volume, volume, 1, 0, 1.0f)
                DebugConfig.d("SoundManager", "Playing sound: ${effect.name}")
            } catch (e: Exception) {
                DebugConfig.e("SoundManager", "Error playing sound: ${effect.name}", e)
            }
        } else {
            // Fallback: Use system sound or log
            DebugConfig.d("SoundManager", "Sound not loaded: ${effect.name} (add to res/raw)")
        }
    }

    /**
     * Play background music (looping)
     */
    fun playBackgroundMusic(@RawRes resourceId: Int) {
        if (!GamePreferences.isMusicEnabled(context)) {
            return
        }

        try {
            stopBackgroundMusic()
            backgroundMusic = MediaPlayer.create(context, resourceId).apply {
                isLooping = true
                setVolume(0.3f, 0.3f)
                start()
            }
            DebugConfig.d("SoundManager", "Background music started")
        } catch (e: Exception) {
            DebugConfig.e("SoundManager", "Error playing background music", e)
        }
    }

    /**
     * Stop background music
     */
    fun stopBackgroundMusic() {
        try {
            backgroundMusic?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            backgroundMusic = null
            DebugConfig.d("SoundManager", "Background music stopped")
        } catch (e: Exception) {
            DebugConfig.e("SoundManager", "Error stopping background music", e)
        }
    }

    /**
     * Pause background music
     */
    fun pauseBackgroundMusic() {
        try {
            backgroundMusic?.pause()
        } catch (e: Exception) {
            DebugConfig.e("SoundManager", "Error pausing background music", e)
        }
    }

    /**
     * Resume background music
     */
    fun resumeBackgroundMusic() {
        if (GamePreferences.isMusicEnabled(context)) {
            try {
                backgroundMusic?.start()
            } catch (e: Exception) {
                DebugConfig.e("SoundManager", "Error resuming background music", e)
            }
        }
    }

    /**
     * Release all resources
     */
    fun release() {
        try {
            stopBackgroundMusic()
            soundPool?.release()
            soundPool = null
            soundMap.clear()
            DebugConfig.d("SoundManager", "Resources released")
        } catch (e: Exception) {
            DebugConfig.e("SoundManager", "Error releasing resources", e)
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