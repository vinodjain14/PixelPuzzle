package com.example.pixelpuzzle

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class LevelMapState(
    val backgroundBitmap: Bitmap? = null,
    val isLoading: Boolean = false
)

class LevelMapViewModel : ViewModel() {
    private val _state = MutableStateFlow(LevelMapState())
    val state: StateFlow<LevelMapState> = _state.asStateFlow()

    private val accessKey = "oZS1ybE8EnX5SOSvsQ50noM-zOEaxsIthml15U36Mk8"

    companion object {
        private const val TAG = "LevelMapViewModel"
    }

    fun loadBackgroundImage(context: android.content.Context) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val directImageUrl = fetchUnsplashImageUrl()
            if (directImageUrl == null) {
                _state.value = _state.value.copy(isLoading = false)
                return@launch
            }

            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(directImageUrl)
                .allowHardware(false)
                .crossfade(true)
                .build()

            try {
                val result = (loader.execute(request) as? SuccessResult)?.drawable
                val bitmap = (result as? BitmapDrawable)?.bitmap

                _state.value = _state.value.copy(
                    backgroundBitmap = bitmap,
                    isLoading = false
                )

                DebugConfig.d(TAG, "Background image loaded successfully")
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false)
                DebugConfig.e(TAG, "Error loading background image", e)
            }
        }
    }

    private suspend fun fetchUnsplashImageUrl(): String? = withContext(Dispatchers.IO) {
        try {
            // Categories suitable for backgrounds
            val categories = listOf(
                "landscape",
                "nature",
                "mountains",
                "ocean",
                "sky",
                "forest",
                "abstract",
                "gradient",
                "minimal",
                "space"
            )

            val randomCategory = categories.random()
            DebugConfig.d(TAG, "Fetching background image category: $randomCategory")

            val apiUrl = "https://api.unsplash.com/photos/random?client_id=$accessKey&query=$randomCategory&orientation=portrait"
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                JSONObject(response).getJSONObject("urls").getString("regular")
            } else null
        } catch (e: Exception) {
            DebugConfig.e(TAG, "Error fetching Unsplash background image", e)
            null
        }
    }
}