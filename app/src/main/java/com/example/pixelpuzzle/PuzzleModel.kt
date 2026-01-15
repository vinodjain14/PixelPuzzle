package com.example.pixelpuzzle

import androidx.compose.runtime.Immutable

@Immutable
data class PuzzlePiece(
    val id: Int,
    val originalRow: Int,
    val originalCol: Int,
    val currentPos: Int, // Position in the grid (0 to rows*cols-1)
    val unitId: Int = id
)

data class GameState(
    val pieces: List<PuzzlePiece> = emptyList(),
    val isLoading: Boolean = false,
    val isSolved: Boolean = false,
    val rows: Int = 3, // Dynamic grid rows
    val cols: Int = 3  // Dynamic grid columns
) {
    val totalPieces: Int get() = rows * cols
    val maxPosition: Int get() = totalPieces - 1

    /**
     * Converts a position to row index
     */
    fun getRow(pos: Int): Int = pos / cols

    /**
     * Converts a position to column index
     */
    fun getCol(pos: Int): Int = pos % cols

    /**
     * Converts row and column to position
     */
    fun getPos(row: Int, col: Int): Int = row * cols + col

    /**
     * Checks if two positions are horizontally adjacent
     */
    fun areHorizontalNeighbors(pos1: Int, pos2: Int): Boolean {
        val row1 = getRow(pos1)
        val col1 = getCol(pos1)
        val row2 = getRow(pos2)
        val col2 = getCol(pos2)

        return row1 == row2 && kotlin.math.abs(col1 - col2) == 1
    }

    /**
     * Checks if two positions are vertically adjacent
     */
    fun areVerticalNeighbors(pos1: Int, pos2: Int): Boolean {
        val row1 = getRow(pos1)
        val col1 = getCol(pos1)
        val row2 = getRow(pos2)
        val col2 = getCol(pos2)

        return col1 == col2 && kotlin.math.abs(row1 - row2) == 1
    }
}

/**
 * Level path configuration for the map
 */
data class LevelPath(
    val startLevel: Int,
    val endLevel: Int,
    val name: String
) {
    val levels: IntRange get() = startLevel..endLevel
}

/**
 * Predefined level paths for the game
 */
object LevelPaths {
    val paths = listOf(
        LevelPath(1, 25, "Beginner Trail"),
        LevelPath(26, 50, "Expert Path"),
        LevelPath(51, 100, "Master Journey")
    )

    const val TOTAL_LEVELS = 10000

    fun getPathForLevel(level: Int): LevelPath? {
        return paths.find { level in it.levels }
    }
}