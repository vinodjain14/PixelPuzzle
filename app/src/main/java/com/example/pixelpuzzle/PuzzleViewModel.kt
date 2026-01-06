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
import kotlin.math.abs

class PuzzleViewModel : ViewModel() {
    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private var fullBitmap: Bitmap? = null
    private val accessKey = "oZS1ybE8EnX5SOSvsQ50noM-zOEaxsIthml15U36Mk8"

    fun loadNewGame(context: android.content.Context) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, isSolved = false)
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
                fullBitmap = (result as? BitmapDrawable)?.bitmap

                if (fullBitmap != null) {
                    val initialPieces = List(9) { i ->
                        PuzzlePiece(id = i, originalRow = i / 3, originalCol = i % 3, currentPos = i)
                    }
                    val shuffledIndices = (0..8).shuffled()
                    val shuffledPieces = initialPieces.mapIndexed { index, piece ->
                        piece.copy(currentPos = shuffledIndices[index], unitId = piece.id)
                    }
                    _state.value = _state.value.copy(pieces = shuffledPieces, isLoading = false)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    private suspend fun fetchUnsplashImageUrl(): String? = withContext(Dispatchers.IO) {
        try {
            val apiUrl = "https://api.unsplash.com/photos/random?client_id=$accessKey&query=abstract,nature&orientation=portrait"
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                JSONObject(response).getJSONObject("urls").getString("regular")
            } else null
        } catch (e: Exception) { null }
    }

    fun onUnitMoveCompleted(unitId: Int, deltaPos: Int) {
        if (deltaPos == 0) return

        val currentPieces = _state.value.pieces
        val movingPieces = currentPieces.filter { it.unitId == unitId }

        // Calculate target positions WITHOUT wrap-around
        val targetPositions = mutableMapOf<PuzzlePiece, Int>()
        var validMove = true

        for (piece in movingPieces) {
            val currentRow = piece.currentPos / 3
            val currentCol = piece.currentPos % 3
            val newPos = piece.currentPos + deltaPos

            // Boundary check - no wrap around
            if (newPos !in 0..8) {
                validMove = false
                break
            }

            // Prevent horizontal wrap (moving from col 2 to 0 or vice versa)
            if (abs(deltaPos) == 1) {
                val newRow = newPos / 3
                if (newRow != currentRow) {
                    validMove = false
                    break
                }
            }

            targetPositions[piece] = newPos
        }

        if (!validMove) return

        // Check what pieces are in our target positions (obstacles)
        val movingFrom = movingPieces.map { it.currentPos }.toSet()
        val movingTo = targetPositions.values.toSet()

        // Find obstacles (pieces that are in our destination but not part of our moving unit)
        val obstacles = currentPieces.filter {
            it.unitId != unitId && movingTo.contains(it.currentPos)
        }

        // Vacated positions are where the moving unit was, minus where it's going
        val vacated = movingFrom.filter { !movingTo.contains(it) }.toMutableList()

        // If we don't have enough space for obstacles, don't allow the move
        if (obstacles.size > vacated.size) {
            return
        }

        // Create the updated pieces list
        val nextPieces = currentPieces.map { piece ->
            when {
                piece.unitId == unitId -> {
                    // This is part of the moving unit
                    piece.copy(currentPos = targetPositions[piece]!!)
                }
                obstacles.any { it.id == piece.id } -> {
                    // This piece is being pushed
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
                else -> {
                    // This piece is not affected
                    piece
                }
            }
        }

        // First break any invalid merges, then check for new merges
        val brokenPieces = breakInvalidMerges(nextPieces)
        val mergedPieces = checkMerges(brokenPieces)
        val isSolved = mergedPieces.all { it.currentPos == (it.originalRow * 3 + it.originalCol) }

        _state.value = _state.value.copy(pieces = mergedPieces, isSolved = isSolved)
    }

    private fun breakInvalidMerges(pieces: List<PuzzlePiece>): List<PuzzlePiece> {
        val result = pieces.toMutableList()
        var nextAvailableId = pieces.maxOf { it.id } + 1

        // Group pieces by unitId
        val unitGroups = result.groupBy { it.unitId }

        for ((unitId, unitPieces) in unitGroups) {
            if (unitPieces.size <= 1) continue // Single pieces can't break

            // Check each piece to see if it's still validly connected
            val toSplit = mutableListOf<Int>()

            for (piece in unitPieces) {
                // Check if this piece has any valid neighbors in the same unit
                var hasValidNeighbor = false

                for (otherPiece in unitPieces) {
                    if (piece.id == otherPiece.id) continue

                    // Check if they're logical neighbors (adjacent in original image)
                    val isLogicalNeighbor = (abs(piece.originalRow - otherPiece.originalRow) == 1 && piece.originalCol == otherPiece.originalCol) ||
                            (abs(piece.originalCol - otherPiece.originalCol) == 1 && piece.originalRow == otherPiece.originalRow)

                    if (!isLogicalNeighbor) continue

                    // Check if they're physical neighbors (adjacent on grid)
                    val r1 = piece.currentPos / 3
                    val c1 = piece.currentPos % 3
                    val r2 = otherPiece.currentPos / 3
                    val c2 = otherPiece.currentPos % 3
                    val isPhysicalNeighbor = (abs(r1 - r2) == 1 && c1 == c2) || (abs(c1 - c2) == 1 && r1 == r2)

                    if (!isPhysicalNeighbor) continue

                    // Check if they're in the correct relative orientation
                    val correctOrientation = (r1 - r2 == piece.originalRow - otherPiece.originalRow) &&
                            (c1 - c2 == piece.originalCol - otherPiece.originalCol)

                    if (correctOrientation) {
                        hasValidNeighbor = true
                        break
                    }
                }

                // If this piece has no valid neighbors, mark it for splitting
                if (!hasValidNeighbor) {
                    toSplit.add(piece.id)
                }
            }

            // Split off pieces that are no longer validly connected
            for (pieceId in toSplit) {
                val index = result.indexOfFirst { it.id == pieceId }
                if (index >= 0) {
                    result[index] = result[index].copy(unitId = nextAvailableId++)
                }
            }
        }

        return result
    }

    private fun checkMerges(pieces: List<PuzzlePiece>): List<PuzzlePiece> {
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
                        val isLogicalNeighbor = (abs(p1.originalRow - p2.originalRow) == 1 && p1.originalCol == p2.originalCol) ||
                                (abs(p1.originalCol - p2.originalCol) == 1 && p1.originalRow == p2.originalRow)

                        val r1 = p1.currentPos / 3; val c1 = p1.currentPos % 3
                        val r2 = p2.currentPos / 3; val c2 = p2.currentPos % 3
                        val isPhysicalNeighbor = (abs(r1 - r2) == 1 && c1 == c2) || (abs(c1 - c2) == 1 && r1 == r2)

                        if (isLogicalNeighbor && isPhysicalNeighbor) {
                            if ((r1 - r2 == p1.originalRow - p2.originalRow) && (c1 - c2 == p1.originalCol - p2.originalCol)) {
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
        }
        return result
    }

    fun getBitmap() = fullBitmap
}