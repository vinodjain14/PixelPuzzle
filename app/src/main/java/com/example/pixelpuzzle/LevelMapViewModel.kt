package com.example.pixelpuzzle

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LevelMapState(
    val levelThumbnails: Map<Int, Bitmap> = emptyMap(),
    val isLoading: Boolean = false
)

class LevelMapViewModel : ViewModel() {
    private val _state = MutableStateFlow(LevelMapState())
    val state: StateFlow<LevelMapState> = _state.asStateFlow()

    companion object {
        private const val TAG = "LevelMapViewModel"
    }

    fun loadAllLevelThumbnails(context: android.content.Context, unlockedLevels: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            // Load saved thumbnails from storage
            val thumbnails = GamePreferences.getAllLevelThumbnails(context)

            _state.value = _state.value.copy(
                levelThumbnails = thumbnails,
                isLoading = false
            )

            DebugConfig.d(TAG, "Loaded ${thumbnails.size} level thumbnails from storage")
        }
    }
}