package com.example.pixelpuzzle

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

class PuzzleViewModel : ViewModel() {
    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private var fullBitmap: Bitmap? = null
    private val accessKey = "oZS1ybE8EnX5SOSvsQ50noM-zOEaxsIthml15U36Mk8"

    private val PREFS_NAME = "puzzle_prefs"
    private val KEY_LEVEL = "current_level"
    private val KEY_COINS = "total_coins"

    fun loadSavedProgress(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedLevel = prefs.getInt(KEY_LEVEL, 1)
        val savedCoins = prefs.getInt(KEY_COINS, 0)
        _state.value = _state.value.copy(currentLevel = savedLevel, totalCoins = savedCoins)
    }

    private fun saveProgress(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt(KEY_LEVEL, _state.value.currentLevel)
            putInt(KEY_COINS, _state.value.totalCoins)
            apply()
        }
    }

    fun loadNewGame(context: Context) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, isSolved = false, showConfetti = false)

            val config = LevelConfig.getConfigForLevel(_state.value.currentLevel)
            val directImageUrl = fetchUnsplashImageUrl(config.category)

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
                fullBitmap = (result as? BitmapDrawable)?.bitmap

                if (fullBitmap != null) {
                    val totalPieces = config.totalPieces
                    val initialPieces = List(totalPieces) { i ->
                        PuzzlePiece(
                            id = i,
                            originalRow = i / config.cols,
                            originalCol = i % config.cols,
                            currentPos = i
                        )
                    }
                    val shuffledIndices = (0 until totalPieces).shuffled()
                    val shuffledPieces = initialPieces.mapIndexed { index, piece ->
                        piece.copy(currentPos = shuffledIndices[index], unitId = piece.id)
                    }
                    _state.value = _state.value.copy(
                        pieces = shuffledPieces,
                        isLoading = false,
                        gridRows = config.rows,
                        gridCols = config.cols
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    private suspend fun fetchUnsplashImageUrl(category: String): String? = withContext(Dispatchers.IO) {
        try {
            val apiUrl = "https://api.unsplash.com/photos/random?client_id=$accessKey&query=$category&orientation=portrait"
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                JSONObject(response).getJSONObject("urls").getString("regular")
            } else null
        } catch (e: Exception) { null }
    }

    fun onUnitMoveCompleted(unitId: Int, deltaPos: Int, context: Context) {
        if (deltaPos == 0) return

        val currentPieces = _state.value.pieces
        val movingPieces = currentPieces.filter { it.unitId == unitId }
        val cols = _state.value.gridCols
        val rows = _state.value.gridRows
        val totalPieces = rows * cols

        // Calculate target positions WITHOUT wrap-around
        val targetPositions = mutableMapOf<PuzzlePiece, Int>()
        var validMove = true

        for (piece in movingPieces) {
            val currentRow = piece.currentPos / cols
            val currentCol = piece.currentPos % cols
            val newPos = piece.currentPos + deltaPos

            if (newPos !in 0 until totalPieces) {
                validMove = false
                break
            }

            if (abs(deltaPos) == 1) {
                val newRow = newPos / cols
                if (newRow != currentRow) {
                    validMove = false
                    break
                }
            }

            targetPositions[piece] = newPos
        }

        if (!validMove) return

        val movingFrom = movingPieces.map { it.currentPos }.toSet()
        val movingTo = targetPositions.values.toSet()

        val obstacles = currentPieces.filter {
            it.unitId != unitId && movingTo.contains(it.currentPos)
        }

        val vacated = movingFrom.filter { !movingTo.contains(it) }.toMutableList()

        if (obstacles.size > vacated.size) return

        val nextPieces = currentPieces.map { piece ->
            when {
                piece.unitId == unitId -> piece.copy(currentPos = targetPositions[piece]!!)
                obstacles.any { it.id == piece.id } -> {
                    if (vacated.isEmpty()) {
                        piece
                    } else {
                        val targetSlot = if (deltaPos > 0) {
                            vacated.minOrNull() ?: piece.currentPos
                        } else {
                            vacated.maxOrNull() ?: piece.currentPos
                        }
                        vacated.remove(targetSlot)
                        piece.copy(currentPos = targetSlot)
                    }
                }
                else -> piece
            }
        }

        val brokenPieces = breakInvalidMerges(nextPieces, cols)
        val mergedPieces = checkMerges(brokenPieces, cols)
        val isSolved = mergedPieces.all { it.currentPos == (it.originalRow * cols + it.originalCol) }

        if (isSolved) {
            val newCoins = _state.value.totalCoins + 20
            _state.value = _state.value.copy(
                pieces = mergedPieces,
                isSolved = true,
                showConfetti = true,
                totalCoins = newCoins
            )
            saveProgress(context)

            viewModelScope.launch {
                delay(3000)
                _state.value = _state.value.copy(showConfetti = false)
            }
        } else {
            _state.value = _state.value.copy(pieces = mergedPieces, isSolved = false)
        }
    }

    fun nextLevel(context: Context) {
        val newLevel = _state.value.currentLevel + 1
        _state.value = _state.value.copy(currentLevel = newLevel, isSolved = false)
        saveProgress(context)
        loadNewGame(context)
    }

    private fun breakInvalidMerges(pieces: List<PuzzlePiece>, cols: Int): List<PuzzlePiece> {
        val result = pieces.toMutableList()
        var nextAvailableId = pieces.maxOf { it.id } + 1

        val unitGroups = result.groupBy { it.unitId }

        for ((unitId, unitPieces) in unitGroups) {
            if (unitPieces.size <= 1) continue

            val toSplit = mutableListOf<Int>()

            for (piece in unitPieces) {
                var hasValidNeighbor = false

                for (otherPiece in unitPieces) {
                    if (piece.id == otherPiece.id) continue

                    // Check if they're logical neighbors (adjacent in original image)
                    val isLogicalNeighbor = (abs(piece.originalRow - otherPiece.originalRow) == 1 && piece.originalCol == otherPiece.originalCol) ||
                            (abs(piece.originalCol - otherPiece.originalCol) == 1 && piece.originalRow == otherPiece.originalRow)

                    if (!isLogicalNeighbor) continue

                    // Check if they're physical neighbors (adjacent on current grid)
                    val r1 = piece.currentPos / cols
                    val c1 = piece.currentPos % cols
                    val r2 = otherPiece.currentPos / cols
                    val c2 = otherPiece.currentPos % cols

                    val isPhysicalNeighbor =
                        (abs(r1 - r2) == 1 && c1 == c2) ||  // Vertical neighbors
                                (abs(c1 - c2) == 1 && r1 == r2)     // Horizontal neighbors

                    if (!isPhysicalNeighbor) continue

                    // Check if they're in correct relative orientation
                    val correctOrientation = (r1 - r2 == piece.originalRow - otherPiece.originalRow) &&
                            (c1 - c2 == piece.originalCol - otherPiece.originalCol)

                    if (correctOrientation) {
                        hasValidNeighbor = true
                        break
                    }
                }

                if (!hasValidNeighbor) {
                    toSplit.add(piece.id)
                }
            }

            for (pieceId in toSplit) {
                val index = result.indexOfFirst { it.id == pieceId }
                if (index >= 0) {
                    result[index] = result[index].copy(unitId = nextAvailableId++)
                }
            }
        }

        return result
    }

    private fun checkMerges(pieces: List<PuzzlePiece>, cols: Int): List<PuzzlePiece> {
        val result = pieces.toMutableList()
        var changed = true
        while (changed) {
            changed = false
            for (i in result.indices) {
                for (j in result.indices) {
                    if (i == j) continue
                    val p1 = result[i]
                    val p2 = result[j]
                    if (p1.unitId != p2.unitId) {
                        // Check if they're logical neighbors (adjacent in original image)
                        val isLogicalNeighbor = (abs(p1.originalRow - p2.originalRow) == 1 && p1.originalCol == p2.originalCol) ||
                                (abs(p1.originalCol - p2.originalCol) == 1 && p1.originalRow == p2.originalRow)

                        if (!isLogicalNeighbor) continue

                        // Check if they're physical neighbors (adjacent on current grid)
                        val r1 = p1.currentPos / cols
                        val c1 = p1.currentPos % cols
                        val r2 = p2.currentPos / cols
                        val c2 = p2.currentPos % cols

                        val isPhysicalNeighbor =
                            (abs(r1 - r2) == 1 && c1 == c2) ||  // Vertical neighbors
                                    (abs(c1 - c2) == 1 && r1 == r2)     // Horizontal neighbors

                        if (!isPhysicalNeighbor) continue

                        // Check if they're in the correct relative orientation
                        val correctOrientation = (r1 - r2 == p1.originalRow - p2.originalRow) &&
                                (c1 - c2 == p1.originalCol - p2.originalCol)

                        if (correctOrientation) {
                            val oldId = p2.unitId
                            val newId = p1.unitId
                            result.indices.forEach { k ->
                                if (result[k].unitId == oldId) result[k] = result[k].copy(unitId = newId)
                            }
                            changed = true
                        }
                    }
                }
            }
        }
        return result
    }

    fun getBitmap() = fullBitmap
}