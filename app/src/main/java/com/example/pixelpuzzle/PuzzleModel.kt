package com.example.pixelpuzzle

import androidx.compose.runtime.Immutable

@Immutable
data class PuzzlePiece(
    val id: Int,
    val originalRow: Int,
    val originalCol: Int,
    val currentPos: Int,
    val unitId: Int = id
)

data class GameState(
    val pieces: List<PuzzlePiece> = emptyList(),
    val isLoading: Boolean = false,
    val isSolved: Boolean = false,
    val showConfetti: Boolean = false,
    val currentLevel: Int = 1,
    val totalCoins: Int = 0,
    val gridRows: Int = 3,
    val gridCols: Int = 3
)

data class LevelConfig(
    val levelNumber: Int,
    val rows: Int,
    val cols: Int,
    val difficulty: String,
    val category: String
) {
    val totalPieces: Int get() = rows * cols

    companion object {
        private val categories = listOf(
            "nature", "art", "modern", "city", "party",
            "animals", "sports", "food", "travel", "architecture"
        )

        fun getConfigForLevel(level: Int): LevelConfig {
            val gameInSet = ((level - 1) % 25) + 1
            val categoryIndex = (level - 1) % categories.size

            return when ((gameInSet - 1) % 5) {
                0 -> LevelConfig(level, 3, 3, "Easy", categories[categoryIndex])
                1 -> LevelConfig(level, 3, 4, "Medium", categories[categoryIndex])
                2 -> LevelConfig(level, 4, 4, "Hard", categories[categoryIndex])
                3 -> LevelConfig(level, 4, 5, "Very Hard", categories[categoryIndex])
                4 -> LevelConfig(level, 5, 5, "Super Hard", categories[categoryIndex])
                else -> LevelConfig(level, 3, 3, "Easy", categories[categoryIndex])
            }
        }
    }
}