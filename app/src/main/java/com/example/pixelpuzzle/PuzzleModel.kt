package com.example.pixelpuzzle

import androidx.compose.runtime.Immutable

@Immutable
data class PuzzlePiece(
    val id: Int,
    val originalRow: Int,
    val originalCol: Int,
    val currentPos: Int, // 0-8 for a 3x3 grid
    val unitId: Int = id
)

data class GameState(
    val pieces: List<PuzzlePiece> = emptyList(),
    val isLoading: Boolean = false,
    val isSolved: Boolean = false
)