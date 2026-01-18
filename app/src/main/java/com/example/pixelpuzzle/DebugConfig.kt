package com.example.pixelpuzzle

import android.content.Context
import android.util.Log

object DebugConfig {
    // Default value - can be changed at runtime
    private var enableDebugLogs = false

    // Log levels
    private const val TAG_PREFIX = "PixelPuzzle"
    private const val PREF_DEBUG_LOGS = "debug_logs_enabled"

    /**
     * Initialize debug settings from SharedPreferences
     */
    fun init(context: Context) {
        val prefs = GamePreferences.getPrefs(context)
        enableDebugLogs = prefs.getBoolean(PREF_DEBUG_LOGS, false)
        Log.i("$TAG_PREFIX:DebugConfig", "Debug logs ${if (enableDebugLogs) "enabled" else "disabled"}")
    }

    /**
     * Toggle debug logging on/off at runtime
     */
    fun setDebugLogsEnabled(context: Context, enabled: Boolean) {
        enableDebugLogs = enabled
        GamePreferences.getPrefs(context)
            .edit()
            .putBoolean(PREF_DEBUG_LOGS, enabled)
            .apply()
        Log.i("$TAG_PREFIX:DebugConfig", "Debug logs ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Check if debug logs are enabled
     */
    fun isDebugEnabled(): Boolean = enableDebugLogs

    fun d(tag: String, message: String) {
        if (enableDebugLogs) {
            Log.d("$TAG_PREFIX:$tag", message)
        }
    }

    fun i(tag: String, message: String) {
        Log.i("$TAG_PREFIX:$tag", message)
    }

    fun w(tag: String, message: String) {
        Log.w("$TAG_PREFIX:$tag", message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e("$TAG_PREFIX:$tag", message, throwable)
        } else {
            Log.e("$TAG_PREFIX:$tag", message)
        }
    }
}