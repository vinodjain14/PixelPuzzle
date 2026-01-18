package com.example.pixelpuzzle

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

object GamePreferences {
    private const val PREFS_NAME = "pixel_puzzle_prefs"
    private const val KEY_TERMS_ACCEPTED = "terms_accepted"
    private const val KEY_UNLOCKED_LEVELS = "unlocked_levels"
    private const val KEY_TOTAL_POINTS = "total_points"
    private const val KEY_MUSIC_ENABLED = "music_enabled"
    private const val KEY_SOUND_ENABLED = "sound_enabled"
    private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
    private const val KEY_USED_IMAGE_IDS = "used_image_ids"
    private const val THUMBNAILS_DIR = "level_thumbnails"

    // Made public for DailyStreakManager and FlashChallengeManager
    fun getPrefs(context: Context): SharedPreferences {
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
        if (current < LevelPaths.TOTAL_LEVELS) {
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

    // Thumbnail storage methods
    fun saveLevelThumbnail(context: Context, level: Int, bitmap: Bitmap) {
        try {
            val thumbnailsDir = File(context.filesDir, THUMBNAILS_DIR)
            if (!thumbnailsDir.exists()) {
                thumbnailsDir.mkdirs()
            }

            val thumbnailFile = File(thumbnailsDir, "level_$level.jpg")
            FileOutputStream(thumbnailFile).use { out ->
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true)
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                scaledBitmap.recycle()
            }
            DebugConfig.d("GamePreferences", "Saved thumbnail for level $level")
        } catch (e: Exception) {
            DebugConfig.e("GamePreferences", "Error saving thumbnail for level $level", e)
        }
    }

    fun getLevelThumbnail(context: Context, level: Int): Bitmap? {
        return try {
            val thumbnailsDir = File(context.filesDir, THUMBNAILS_DIR)
            val thumbnailFile = File(thumbnailsDir, "level_$level.jpg")

            if (thumbnailFile.exists()) {
                BitmapFactory.decodeFile(thumbnailFile.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            DebugConfig.e("GamePreferences", "Error loading thumbnail for level $level", e)
            null
        }
    }

    fun getAllLevelThumbnails(context: Context): Map<Int, Bitmap> {
        val thumbnails = mutableMapOf<Int, Bitmap>()
        val unlockedLevels = getUnlockedLevels(context)

        for (level in 1 until unlockedLevels) {
            getLevelThumbnail(context, level)?.let { bitmap ->
                thumbnails[level] = bitmap
            }
        }

        return thumbnails
    }

    // Used image IDs tracking to avoid repeats
    fun addUsedImageId(context: Context, imageId: String) {
        val usedIds = getUsedImageIds(context).toMutableSet()
        usedIds.add(imageId)
        getPrefs(context).edit().putStringSet(KEY_USED_IMAGE_IDS, usedIds).apply()
        DebugConfig.d("GamePreferences", "Added used image ID: $imageId (Total: ${usedIds.size})")
    }

    fun getUsedImageIds(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_USED_IMAGE_IDS, emptySet()) ?: emptySet()
    }

    fun isImageIdUsed(context: Context, imageId: String): Boolean {
        return getUsedImageIds(context).contains(imageId)
    }

    private const val KEY_DEVELOPER_MODE = "developer_mode_enabled"

    fun isDeveloperModeEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_DEVELOPER_MODE, false)
    }

    fun setDeveloperModeEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DEVELOPER_MODE, enabled).apply()
    }
}