package com.example.pixelpuzzle

import android.content.Context
import android.content.SharedPreferences

object GamePreferences {
    private const val PREFS_NAME = "pixel_puzzle_prefs"
    private const val KEY_TERMS_ACCEPTED = "terms_accepted"
    private const val KEY_UNLOCKED_LEVELS = "unlocked_levels"
    private const val KEY_TOTAL_POINTS = "total_points"
    private const val KEY_MUSIC_ENABLED = "music_enabled"
    private const val KEY_SOUND_ENABLED = "sound_enabled"
    private const val KEY_VIBRATION_ENABLED = "vibration_enabled"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun hasAcceptedTerms(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_TERMS_ACCEPTED, false)
    }

    fun setTermsAccepted(context: Context) {
        getPrefs(context).edit().putBoolean(KEY_TERMS_ACCEPTED, true).apply()
    }

    fun getUnlockedLevels(context: Context): Int {
        return getPrefs(context).getInt(KEY_UNLOCKED_LEVELS, 1)
    }

    fun unlockNextLevel(context: Context) {
        val current = getUnlockedLevels(context)
        if (current < 20) {
            getPrefs(context).edit().putInt(KEY_UNLOCKED_LEVELS, current + 1).apply()
        }
    }

    fun getTotalPoints(context: Context): Int {
        return getPrefs(context).getInt(KEY_TOTAL_POINTS, 0)
    }

    fun addPoints(context: Context, points: Int) {
        val current = getTotalPoints(context)
        getPrefs(context).edit().putInt(KEY_TOTAL_POINTS, current + points).apply()
    }

    fun isMusicEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_MUSIC_ENABLED, false)
    }

    fun setMusicEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_MUSIC_ENABLED, enabled).apply()
    }

    fun isSoundEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SOUND_ENABLED, false)
    }

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    fun isVibrationEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_VIBRATION_ENABLED, true)
    }

    fun setVibrationEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply()
    }
}