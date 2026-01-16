package com.example.pixelpuzzle.ui.theme

import androidx.compose.ui.graphics.Color

// Original colors (kept for backward compatibility)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// ========================================
// CYBER-GLOW NEON-PASTEL PALETTE (2026)
// ========================================

/**
 * Primary Neon Colors
 */
object NeonColors {
    // Electric Blues & Teals
    val ElectricTeal = Color(0xFF00FFF5)
    val ElectricBlue = Color(0xFF00D9FF)
    val CyberBlue = Color(0xFF0096FF)
    val DeepElectric = Color(0xFF0066FF)

    // Neon Pinks & Magentas
    val NeonPink = Color(0xFFFF006E)
    val SunsetCoral = Color(0xFFFF6B9D)
    val HotMagenta = Color(0xFFFF1B8D)
    val BubblegumPink = Color(0xFFFF69EB)

    // Neon Purples
    val NeonPurple = Color(0xFF9D4EDD)
    val VividViolet = Color(0xFFB537F2)
    val ElectricPurple = Color(0xFF8B2EFF)
    val CyberLavender = Color(0xFFAE7AFF)

    // Neon Greens
    val CyberGreen = Color(0xFF00FF88)
    val NeonLime = Color(0xFF39FF14)
    val MatrixGreen = Color(0xFF00FF00)
    val MintGlow = Color(0xFF00FFB9)

    // Neon Oranges & Yellows
    val NeonOrange = Color(0xFFFF7F11)
    val ElectricAmber = Color(0xFFFFB627)
    val GlowYellow = Color(0xFFFFD60A)
    val LaserGold = Color(0xFFFFD700)
}

/**
 * Pastel Glow Colors (softer variants)
 */
object PastelGlow {
    val SoftTeal = Color(0xFF7FFFD4)
    val SoftBlue = Color(0xFF87CEEB)
    val SoftPink = Color(0xFFFFB6C1)
    val SoftPurple = Color(0xFFDDA0DD)
    val SoftGreen = Color(0xFF98FB98)
    val SoftYellow = Color(0xFFFFFACD)
    val SoftOrange = Color(0xFFFFDAB9)
}

/**
 * Dark Mode Base Colors
 */
object DarkBase {
    val DeepSpace = Color(0xFF0A0E27)
    val DarkSlate = Color(0xFF0D1117)
    val Midnight = Color(0xFF16213E)
    val Obsidian = Color(0xFF1A1A2E)
    val CharcoalBlue = Color(0xFF161B22)
    val VoidBlack = Color(0xFF0F1419)
}

/**
 * UI Element Colors (Glassmorphism)
 */
object GlassUI {
    // Card backgrounds
    val CardDark = DarkBase.Obsidian.copy(alpha = 0.7f)
    val CardMedium = DarkBase.Midnight.copy(alpha = 0.6f)
    val CardLight = DarkBase.CharcoalBlue.copy(alpha = 0.5f)

    // Borders
    val BorderBright = Color.White.copy(alpha = 0.3f)
    val BorderMedium = Color.White.copy(alpha = 0.15f)
    val BorderSubtle = Color.White.copy(alpha = 0.08f)

    // Overlays
    val OverlayDark = Color.Black.copy(alpha = 0.6f)
    val OverlayMedium = Color.Black.copy(alpha = 0.4f)
    val OverlayLight = Color.Black.copy(alpha = 0.2f)
}

/**
 * Status Colors (Neon variants)
 */
object StatusNeon {
    val Success = NeonColors.CyberGreen
    val Warning = NeonColors.NeonOrange
    val Error = NeonColors.NeonPink
    val Info = NeonColors.ElectricBlue
}

/**
 * Gradient Definitions
 */
object CyberGradients {
    // Primary gradients
    val TealBlue = listOf(NeonColors.ElectricTeal, NeonColors.ElectricBlue)
    val PinkCoral = listOf(NeonColors.NeonPink, NeonColors.SunsetCoral)
    val PurplePink = listOf(NeonColors.NeonPurple, NeonColors.NeonPink)
    val GreenTeal = listOf(NeonColors.CyberGreen, NeonColors.ElectricTeal)

    // Multi-color gradients
    val Rainbow = listOf(
        NeonColors.NeonPink,
        NeonColors.NeonPurple,
        NeonColors.ElectricBlue,
        NeonColors.ElectricTeal,
        NeonColors.CyberGreen,
        NeonColors.GlowYellow,
        NeonColors.NeonOrange
    )

    val Sunset = listOf(
        NeonColors.NeonOrange,
        NeonColors.NeonPink,
        NeonColors.NeonPurple,
        NeonColors.ElectricBlue
    )

    val Matrix = listOf(
        NeonColors.MatrixGreen,
        NeonColors.CyberGreen,
        NeonColors.ElectricTeal
    )

    // Dark backgrounds with accent
    val DarkTeal = listOf(
        DarkBase.DeepSpace,
        DarkBase.Midnight,
        NeonColors.ElectricTeal.copy(alpha = 0.3f)
    )

    val DarkPurple = listOf(
        DarkBase.Obsidian,
        DarkBase.Midnight,
        NeonColors.NeonPurple.copy(alpha = 0.3f)
    )
}

/**
 * Text Colors (optimized for dark backgrounds)
 */
object TextColors {
    val Primary = Color.White
    val Secondary = Color.White.copy(alpha = 0.8f)
    val Tertiary = Color.White.copy(alpha = 0.6f)
    val Disabled = Color.White.copy(alpha = 0.4f)

    // Neon text for emphasis
    val NeonAccent = NeonColors.ElectricTeal
    val NeonHighlight = NeonColors.NeonPink
}

/**
 * Game-specific colors
 */
object GameColors {
    // Streak colors
    val StreakFire = listOf(NeonColors.NeonOrange, NeonColors.NeonPink)
    val StreakGold = NeonColors.LaserGold

    // Points/Coins
    val CoinGold = NeonColors.LaserGold
    val CoinShine = NeonColors.GlowYellow

    // Level status
    val LevelLocked = Color(0xFF4A4A4A)
    val LevelUnlocked = NeonColors.ElectricTeal
    val LevelCurrent = NeonColors.LaserGold
    val LevelCompleted = NeonColors.CyberGreen

    // Puzzle elements
    val PuzzleBackground = DarkBase.CharcoalBlue
    val PuzzleBorder = NeonColors.ElectricBlue.copy(alpha = 0.5f)
    val PuzzleMerge = NeonColors.CyberGreen

    // Hint colors
    val HintFree = NeonColors.CyberGreen
    val HintPremium = NeonColors.NeonPurple
    val HintMaster = NeonColors.LaserGold
}