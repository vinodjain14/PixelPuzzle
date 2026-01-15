package com.example.pixelpuzzle

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
    private var nextAvailableUnitId = 0
    private var currentImageUrl: String? = null // Store current image URL for restart

    companion object {
        private const val TAG = "PuzzleViewModel"
    }

    fun loadNewGame(context: android.content.Context, level: Int = 1) {
        viewModelScope.launch {
            val difficulty = getDifficultyForLevel(level)
            val rows = difficulty.rows
            val cols = difficulty.cols
            val totalPieces = rows * cols

            _state.value = _state.value.copy(
                isLoading = true,
                isSolved = false,
                rows = rows,
                cols = cols
            )

            // Fetch image with retry logic to avoid repeats
            var imageIdAndUrl: String? = null
            val maxRetries = 15
            var retries = 0

            while (retries < maxRetries) {
                val result = fetchUnsplashImageUrl()
                if (result != null) {
                    val parts = result.split("|")
                    if (parts.size == 2) {
                        val imageId = parts[0]
                        val imageUrl = parts[1]

                        if (!GamePreferences.isImageIdUsed(context, imageId)) {
                            // Found an unused image
                            GamePreferences.addUsedImageId(context, imageId)
                            imageIdAndUrl = imageUrl
                            DebugConfig.d(TAG, "Selected unused image: $imageId")
                            break
                        } else {
                            DebugConfig.d(TAG, "Image $imageId already used, fetching another...")
                        }
                    }
                }
                retries++
                delay(300)
            }

            if (imageIdAndUrl == null) {
                DebugConfig.w(TAG, "Could not find unused image after $maxRetries retries, using any image")
                val fallback = fetchUnsplashImageUrl()
                imageIdAndUrl = fallback?.split("|")?.getOrNull(1)
            }

            if (imageIdAndUrl == null) {
                _state.value = _state.value.copy(isLoading = false)
                return@launch
            }

            // Store the image URL for restart functionality
            currentImageUrl = imageIdAndUrl

            loadImageAndCreatePuzzle(context, imageIdAndUrl, rows, cols, totalPieces, level)
        }
    }

    fun restartCurrentGame() {
        viewModelScope.launch {
            val currentUrl = currentImageUrl
            if (currentUrl == null || fullBitmap == null) {
                _state.value = _state.value.copy(isLoading = false)
                return@launch
            }

            val rows = _state.value.rows
            val cols = _state.value.cols
            val totalPieces = rows * cols

            _state.value = _state.value.copy(
                isLoading = true,
                isSolved = false
            )

            // Add a small delay for visual feedback
            delay(300)

            // Reshuffle the existing puzzle
            val initialPieces = List(totalPieces) { i ->
                PuzzlePiece(
                    id = i,
                    originalRow = i / cols,
                    originalCol = i % cols,
                    currentPos = i
                )
            }

            val shuffledIndices = (0 until totalPieces).shuffled()
            val shuffledPieces = initialPieces.mapIndexed { index, piece ->
                piece.copy(currentPos = shuffledIndices[index], unitId = piece.id)
            }

            nextAvailableUnitId = totalPieces
            _state.value = _state.value.copy(pieces = shuffledPieces, isLoading = false)

            DebugConfig.d(TAG, "=== GAME RESTARTED (${rows}×${cols}) ===")
            DebugConfig.d(TAG, "Reshuffled positions: ${shuffledPieces.map { "Piece ${it.id} at pos ${it.currentPos}" }}")
        }
    }

    private suspend fun loadImageAndCreatePuzzle(
        context: android.content.Context,
        imageUrl: String,
        rows: Int,
        cols: Int,
        totalPieces: Int,
        level: Int
    ) {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .allowHardware(false)
            .crossfade(true)
            .build()

        try {
            val result = (loader.execute(request) as? SuccessResult)?.drawable
            fullBitmap = (result as? BitmapDrawable)?.bitmap

            if (fullBitmap != null) {
                val initialPieces = List(totalPieces) { i ->
                    PuzzlePiece(
                        id = i,
                        originalRow = i / cols,
                        originalCol = i % cols,
                        currentPos = i
                    )
                }

                val shuffledIndices = (0 until totalPieces).shuffled()
                val shuffledPieces = initialPieces.mapIndexed { index, piece ->
                    piece.copy(currentPos = shuffledIndices[index], unitId = piece.id)
                }

                nextAvailableUnitId = totalPieces
                _state.value = _state.value.copy(pieces = shuffledPieces, isLoading = false)

                DebugConfig.d(TAG, "=== NEW GAME LOADED (Level $level - ${rows}×${cols}) ===")
                DebugConfig.d(TAG, "Initial shuffled positions: ${shuffledPieces.map { "Piece ${it.id} at pos ${it.currentPos}" }}")
            }
        } catch (e: Exception) {
            _state.value = _state.value.copy(isLoading = false)
            DebugConfig.e(TAG, "Error loading game", e)
        }
    }

    private suspend fun fetchUnsplashImageUrl(): String? = withContext(Dispatchers.IO) {
        try {
            val categories = listOf(
                "abstract", "nature", "architecture", "patterns", "textures",
                "colors", "geometric", "landscape", "minimal", "gradient",
                "flowers", "animals", "food", "technology", "space",
                "ocean", "mountains", "city", "art", "sky"
            )

            val randomCategory = categories.random()
            val maxAttempts = 10
            var attempts = 0

            while (attempts < maxAttempts) {
                attempts++
                DebugConfig.d(TAG, "Fetching image category: $randomCategory (Attempt $attempts)")

                val apiUrl = "https://api.unsplash.com/photos/random?client_id=$accessKey&query=$randomCategory&orientation=squarish"
                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)
                    val imageId = jsonObject.getString("id")
                    val imageUrl = jsonObject.getJSONObject("urls").getString("regular")

                    // Check if this image has been used before
                    // Note: context needs to be passed, we'll handle this in the calling function
                    DebugConfig.d(TAG, "Found image ID: $imageId")
                    return@withContext "$imageId|$imageUrl" // Return both ID and URL
                }

                delay(500) // Small delay between attempts
            }

            DebugConfig.w(TAG, "Could not find unused image after $maxAttempts attempts")
            null
        } catch (e: Exception) {
            DebugConfig.e(TAG, "Error fetching Unsplash image", e)
            null
        }
    }

    fun onUnitMoveCompleted(unitId: Int, deltaPos: Int) {
        val currentState = _state.value
        val rows = currentState.rows
        val cols = currentState.cols

        DebugConfig.d(TAG, "")
        DebugConfig.d(TAG, "========================================")
        DebugConfig.d(TAG, "MOVE ATTEMPTED: Unit $unitId, Delta: $deltaPos (Grid: ${rows}×${cols})")

        if (deltaPos == 0) {
            DebugConfig.d(TAG, "MOVE CANCELLED: Delta is 0 (no movement)")
            DebugConfig.d(TAG, "========================================")
            return
        }

        val currentPieces = currentState.pieces
        val movingPieces = currentPieces.filter { it.unitId == unitId }

        DebugConfig.d(TAG, "Moving pieces: ${movingPieces.map { "Piece ${it.id} at pos ${it.currentPos}" }}")

        val targetPositions = mutableMapOf<PuzzlePiece, Int>()
        var validMove = true

        for (piece in movingPieces) {
            val currentRow = piece.currentPos / cols
            val currentCol = piece.currentPos % cols
            val newPos = piece.currentPos + deltaPos

            if (newPos !in 0..currentState.maxPosition) {
                validMove = false
                DebugConfig.d(TAG, "MOVE INVALID: Piece ${piece.id} would move out of bounds (pos ${piece.currentPos} -> $newPos)")
                break
            }

            // Check for horizontal wrapping (moving left/right)
            if (abs(deltaPos) == 1) {
                val newRow = newPos / cols
                val newCol = newPos % cols

                if (currentRow == newRow) {
                    if (abs(currentCol - newCol) != 1) {
                        validMove = false
                        DebugConfig.d(TAG, "MOVE INVALID: Piece ${piece.id} would wrap horizontally")
                        break
                    }
                }
            }

            targetPositions[piece] = newPos
        }

        if (!validMove) {
            DebugConfig.d(TAG, "========================================")
            return
        }

        DebugConfig.d(TAG, "Target positions: ${targetPositions.map { "Piece ${it.key.id} -> pos ${it.value}" }}")

        val movingFrom = movingPieces.map { it.currentPos }.toSet()
        val movingTo = targetPositions.values.toSet()

        val obstacles = currentPieces.filter {
            it.unitId != unitId && movingTo.contains(it.currentPos)
        }

        val vacated = movingFrom.filter { !movingTo.contains(it) }.toList()

        DebugConfig.d(TAG, "Obstacles found: ${obstacles.map { "Piece ${it.id} (Unit ${it.unitId}) at pos ${it.currentPos}" }}")
        DebugConfig.d(TAG, "Vacated positions: $vacated")

        if (obstacles.size > vacated.size) {
            DebugConfig.d(TAG, "MOVE INVALID: Not enough space for obstacles (${obstacles.size} obstacles, ${vacated.size} vacated spots)")
            DebugConfig.d(TAG, "========================================")
            return
        }

        val sortedObstacles = obstacles.sortedBy { it.currentPos }
        val sortedVacated = vacated.sorted()

        val obstacleTargets = sortedObstacles.mapIndexed { index, obstacle ->
            obstacle.id to sortedVacated.getOrNull(index)
        }.toMap()

        DebugConfig.d(TAG, "Obstacle movements: ${obstacleTargets.map { "Piece ${it.key} -> pos ${it.value}" }}")

        val nextPieces = currentPieces.map { piece ->
            when {
                piece.unitId == unitId -> {
                    val newPos = targetPositions[piece]!!
                    DebugConfig.d(TAG, "Moving piece ${piece.id}: pos ${piece.currentPos} -> $newPos")
                    piece.copy(currentPos = newPos)
                }
                obstacleTargets.containsKey(piece.id) -> {
                    val targetSlot = obstacleTargets[piece.id] ?: piece.currentPos
                    DebugConfig.d(TAG, "Pushing piece ${piece.id}: pos ${piece.currentPos} -> $targetSlot")
                    piece.copy(currentPos = targetSlot)
                }
                else -> piece
            }
        }

        DebugConfig.d(TAG, "Checking for invalid merges...")
        val brokenPieces = breakInvalidMerges(nextPieces, currentState)

        DebugConfig.d(TAG, "Checking for new merges...")
        val mergedPieces = checkMerges(brokenPieces, currentState)

        val isSolved = mergedPieces.all {
            it.currentPos == (it.originalRow * cols + it.originalCol)
        }

        DebugConfig.d(TAG, "Final state: ${mergedPieces.map { "Piece ${it.id} (Unit ${it.unitId}) at pos ${it.currentPos}" }}")
        DebugConfig.d(TAG, "Puzzle solved: $isSolved")
        DebugConfig.d(TAG, "========================================")

        _state.value = _state.value.copy(pieces = mergedPieces, isSolved = isSolved)
    }

    private fun breakInvalidMerges(pieces: List<PuzzlePiece>, gameState: GameState): List<PuzzlePiece> {
        val result = pieces.toMutableList()
        val unitGroups = result.groupBy { it.unitId }
        val cols = gameState.cols

        for ((unitId, unitPieces) in unitGroups) {
            if (unitPieces.size <= 1) continue

            val piecesToSplit = mutableListOf<Int>()

            for (piece in unitPieces) {
                var hasValidNeighbor = false

                for (otherPiece in unitPieces) {
                    if (piece.id == otherPiece.id) continue

                    val isLogicalNeighbor = (abs(piece.originalRow - otherPiece.originalRow) == 1 && piece.originalCol == otherPiece.originalCol) ||
                            (abs(piece.originalCol - otherPiece.originalCol) == 1 && piece.originalRow == otherPiece.originalRow)

                    if (!isLogicalNeighbor) continue

                    val r1 = piece.currentPos / cols
                    val c1 = piece.currentPos % cols
                    val r2 = otherPiece.currentPos / cols
                    val c2 = otherPiece.currentPos % cols
                    val isPhysicalNeighbor = (abs(r1 - r2) == 1 && c1 == c2) || (abs(c1 - c2) == 1 && r1 == r2)

                    if (!isPhysicalNeighbor) continue

                    val correctOrientation = (r1 - r2 == piece.originalRow - otherPiece.originalRow) &&
                            (c1 - c2 == piece.originalCol - otherPiece.originalCol)

                    if (correctOrientation) {
                        hasValidNeighbor = true
                        break
                    }
                }

                if (!hasValidNeighbor) {
                    piecesToSplit.add(piece.id)
                }
            }

            if (piecesToSplit.isNotEmpty()) {
                DebugConfig.d(TAG, "Breaking unit $unitId: splitting pieces $piecesToSplit")
            }

            for (pieceId in piecesToSplit) {
                val index = result.indexOfFirst { it.id == pieceId }
                if (index >= 0) {
                    val newUnitId = nextAvailableUnitId++
                    DebugConfig.d(TAG, "  Piece $pieceId: Unit $unitId -> Unit $newUnitId")
                    result[index] = result[index].copy(unitId = newUnitId)
                }
            }
        }

        return result
    }

    private fun checkMerges(pieces: List<PuzzlePiece>, gameState: GameState): List<PuzzlePiece> {
        val result = pieces.toMutableList()
        var changed = true
        var mergeCount = 0
        val cols = gameState.cols

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

                        val r1 = p1.currentPos / cols
                        val c1 = p1.currentPos % cols
                        val r2 = p2.currentPos / cols
                        val c2 = p2.currentPos % cols
                        val isPhysicalNeighbor = (abs(r1 - r2) == 1 && c1 == c2) || (abs(c1 - c2) == 1 && r1 == r2)

                        if (isLogicalNeighbor && isPhysicalNeighbor) {
                            if ((r1 - r2 == p1.originalRow - p2.originalRow) && (c1 - c2 == p1.originalCol - p2.originalCol)) {
                                val oldId = p2.unitId
                                val newId = p1.unitId

                                mergeCount++
                                DebugConfig.d(TAG, "MERGE #$mergeCount: Unit $oldId merged into Unit $newId (Pieces ${p1.id} & ${p2.id})")

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

        if (mergeCount == 0) {
            DebugConfig.d(TAG, "No merges occurred")
        }

        return result
    }

    fun getBitmap() = fullBitmap
}