package com.example.pixelpuzzle

import android.util.Log

object DebugConfig {
    // Set this to true to enable debug logs, false for production
    const val ENABLE_DEBUG_LOGS = false

    // Log levels
    private const val TAG_PREFIX = "PixelPuzzle"

    fun d(tag: String, message: String) {
        if (ENABLE_DEBUG_LOGS) {
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
