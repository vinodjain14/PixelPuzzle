package com.example.pixelpuzzle

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Manages haptic feedback patterns for different game events
 * Provides tactile feedback to enhance user experience
 */
class VibrationManager(private val context: Context) {

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    companion object {
        @Volatile
        private var instance: VibrationManager? = null

        fun getInstance(context: Context): VibrationManager {
            return instance ?: synchronized(this) {
                instance ?: VibrationManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Trigger vibration based on pattern type
     */
    fun vibrate(pattern: VibrationPattern) {
        if (!GamePreferences.isVibrationEnabled(context)) {
            return
        }

        if (!vibrator.hasVibrator()) {
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                when (pattern) {
                    VibrationPattern.LIGHT_TAP -> {
                        vibrator.vibrate(
                            VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE)
                        )
                    }
                    VibrationPattern.MEDIUM_TAP -> {
                        vibrator.vibrate(
                            VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE)
                        )
                    }
                    VibrationPattern.MERGE -> {
                        // Two quick pulses
                        val timings = longArrayOf(0, 50, 30, 50)
                        val amplitudes = intArrayOf(0, 100, 0, 150)
                        vibrator.vibrate(
                            VibrationEffect.createWaveform(timings, amplitudes, -1)
                        )
                    }
                    VibrationPattern.SUCCESS -> {
                        // Three ascending pulses
                        val timings = longArrayOf(0, 50, 30, 60, 30, 70)
                        val amplitudes = intArrayOf(0, 80, 0, 120, 0, 200)
                        vibrator.vibrate(
                            VibrationEffect.createWaveform(timings, amplitudes, -1)
                        )
                    }
                    VibrationPattern.COMPLETE -> {
                        // Long celebratory pattern
                        val timings = longArrayOf(0, 100, 50, 100, 50, 150)
                        val amplitudes = intArrayOf(0, 150, 0, 200, 0, 255)
                        vibrator.vibrate(
                            VibrationEffect.createWaveform(timings, amplitudes, -1)
                        )
                    }
                    VibrationPattern.ERROR -> {
                        // Sharp double tap
                        val timings = longArrayOf(0, 30, 50, 30)
                        val amplitudes = intArrayOf(0, 255, 0, 255)
                        vibrator.vibrate(
                            VibrationEffect.createWaveform(timings, amplitudes, -1)
                        )
                    }
                    VibrationPattern.UNLOCK -> {
                        // Rising wave
                        val timings = longArrayOf(0, 80, 40, 120)
                        val amplitudes = intArrayOf(0, 100, 0, 255)
                        vibrator.vibrate(
                            VibrationEffect.createWaveform(timings, amplitudes, -1)
                        )
                    }
                }

                DebugConfig.d("VibrationManager", "Vibration pattern: ${pattern.name}")
            } else {
                // Fallback for older devices
                @Suppress("DEPRECATION")
                when (pattern) {
                    VibrationPattern.LIGHT_TAP -> vibrator.vibrate(10)
                    VibrationPattern.MEDIUM_TAP -> vibrator.vibrate(25)
                    VibrationPattern.MERGE -> vibrator.vibrate(longArrayOf(0, 50, 30, 50), -1)
                    VibrationPattern.SUCCESS -> vibrator.vibrate(longArrayOf(0, 50, 30, 60, 30, 70), -1)
                    VibrationPattern.COMPLETE -> vibrator.vibrate(longArrayOf(0, 100, 50, 100, 50, 150), -1)
                    VibrationPattern.ERROR -> vibrator.vibrate(longArrayOf(0, 30, 50, 30), -1)
                    VibrationPattern.UNLOCK -> vibrator.vibrate(longArrayOf(0, 80, 40, 120), -1)
                }
            }
        } catch (e: Exception) {
            DebugConfig.e("VibrationManager", "Error triggering vibration", e)
        }
    }

    /**
     * Cancel any ongoing vibration
     */
    fun cancel() {
        try {
            vibrator.cancel()
        } catch (e: Exception) {
            DebugConfig.e("VibrationManager", "Error canceling vibration", e)
        }
    }
}

/**
 * Predefined vibration patterns for different game events
 */
enum class VibrationPattern {
    LIGHT_TAP,      // Light touch feedback (10ms)
    MEDIUM_TAP,     // Regular tap (25ms)
    MERGE,          // Pieces merging together
    SUCCESS,        // Correct placement
    COMPLETE,       // Puzzle solved
    ERROR,          // Invalid move
    UNLOCK          // New level unlocked
}