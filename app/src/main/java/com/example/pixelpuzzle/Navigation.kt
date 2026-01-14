package com.example.pixelpuzzle

sealed class Screen(val route: String) {
    object Terms : Screen("terms")
    object LevelMap : Screen("level_map")
    object Game : Screen("game/{level}") {
        fun createRoute(level: Int) = "game/$level"
    }
}