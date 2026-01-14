package com.example.pixelpuzzle

/**
 * Defines the difficulty levels and their corresponding grid dimensions
 */
enum class GridDifficulty(val rows: Int, val cols: Int, val displayName: String) {
    SUPER_EASY(3, 3, "Super Easy"),
    EASY(3, 4, "Easy"),
    MEDIUM(4, 4, "Medium"),
    HARD(4, 5, "Hard"),
    SUPER_HARD(5, 5, "Super Hard");

    val totalPieces: Int get() = rows * cols
    val maxPosition: Int get() = totalPieces - 1
}

/**
 * Returns the appropriate difficulty for a given level
 * Level 1-10: Always 3x3 (SUPER_EASY)
 * Level 11+: Cycles through all difficulties
 */
fun getDifficultyForLevel(level: Int): GridDifficulty {
    // Levels 1-10 are always 3x3
    if (level <= 10) {
        return GridDifficulty.SUPER_EASY
    }

    // Level 11+ cycles through difficulties
    // Level 11 = SUPER_EASY (3x3)
    // Level 12 = EASY (3x4)
    // Level 13 = MEDIUM (4x4)
    // Level 14 = HARD (4x5)
    // Level 15 = SUPER_HARD (5x5)
    // Level 16 = SUPER_EASY again, and so on...

    val cyclePosition = (level - 11) % 5
    return when (cyclePosition) {
        0 -> GridDifficulty.SUPER_EASY
        1 -> GridDifficulty.EASY
        2 -> GridDifficulty.MEDIUM
        3 -> GridDifficulty.HARD
        4 -> GridDifficulty.SUPER_HARD
        else -> GridDifficulty.SUPER_EASY // Fallback (should never happen)
    }
}

/**
 * Returns display text for a level's difficulty
 */
fun getDifficultyDisplayText(level: Int): String {
    val difficulty = getDifficultyForLevel(level)
    return "${difficulty.rows}×${difficulty.cols} - ${difficulty.displayName}"
}