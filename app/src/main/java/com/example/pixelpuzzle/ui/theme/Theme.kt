package com.example.pixelpuzzle.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Legacy color schemes (kept for compatibility)
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

/**
 * Cyber-Glow Dark Theme (Primary theme for 2026)
 */
private val CyberGlowDarkScheme = darkColorScheme(
    // Primary colors
    primary = NeonColors.ElectricTeal,
    onPrimary = DarkBase.DeepSpace,
    primaryContainer = NeonColors.CyberBlue,
    onPrimaryContainer = Color.White,

    // Secondary colors
    secondary = NeonColors.NeonPurple,
    onSecondary = Color.White,
    secondaryContainer = NeonColors.VividViolet,
    onSecondaryContainer = Color.White,

    // Tertiary colors
    tertiary = NeonColors.NeonPink,
    onTertiary = Color.White,
    tertiaryContainer = NeonColors.SunsetCoral,
    onTertiaryContainer = Color.White,

    // Background & Surface
    background = DarkBase.DeepSpace,
    onBackground = TextColors.Primary,
    surface = DarkBase.Obsidian,
    onSurface = TextColors.Primary,
    surfaceVariant = DarkBase.Midnight,
    onSurfaceVariant = TextColors.Secondary,

    // Outline & Error
    outline = GlassUI.BorderMedium,
    outlineVariant = GlassUI.BorderSubtle,
    error = StatusNeon.Error,
    onError = Color.White,
    errorContainer = StatusNeon.Error.copy(alpha = 0.2f),
    onErrorContainer = StatusNeon.Error,

    // Inverse colors
    inverseSurface = Color.White,
    inverseOnSurface = DarkBase.DeepSpace,
    inversePrimary = NeonColors.ElectricTeal,

    // Scrim
    scrim = Color.Black.copy(alpha = 0.5f),

    // Surface tints
    surfaceTint = NeonColors.ElectricTeal.copy(alpha = 0.1f)
)

/**
 * Cyber-Glow Light Theme (Alternative theme)
 */
private val CyberGlowLightScheme = lightColorScheme(
    // Primary colors
    primary = NeonColors.CyberBlue,
    onPrimary = Color.White,
    primaryContainer = PastelGlow.SoftTeal,
    onPrimaryContainer = DarkBase.DeepSpace,

    // Secondary colors
    secondary = NeonColors.NeonPurple,
    onSecondary = Color.White,
    secondaryContainer = PastelGlow.SoftPurple,
    onSecondaryContainer = DarkBase.Obsidian,

    // Tertiary colors
    tertiary = NeonColors.NeonPink,
    onTertiary = Color.White,
    tertiaryContainer = PastelGlow.SoftPink,
    onTertiaryContainer = DarkBase.Midnight,

    // Background & Surface
    background = Color(0xFFF5F5F5),
    onBackground = DarkBase.DeepSpace,
    surface = Color.White,
    onSurface = DarkBase.Obsidian,
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = DarkBase.CharcoalBlue,

    // Outline & Error
    outline = Color(0xFFCCCCCC),
    outlineVariant = Color(0xFFE0E0E0),
    error = StatusNeon.Error,
    onError = Color.White,
    errorContainer = StatusNeon.Error.copy(alpha = 0.1f),
    onErrorContainer = StatusNeon.Error,

    // Inverse colors
    inverseSurface = DarkBase.Obsidian,
    inverseOnSurface = Color.White,
    inversePrimary = NeonColors.ElectricTeal,

    // Scrim
    scrim = Color.Black.copy(alpha = 0.3f),

    // Surface tints
    surfaceTint = NeonColors.ElectricTeal.copy(alpha = 0.05f)
)

@Composable
fun PixelPuzzleTheme(
    darkTheme: Boolean = true, // Default to dark theme for cyber-glow effect
    useCyberGlow: Boolean = true, // Enable new theme by default
    dynamicColor: Boolean = false, // Disabled for consistent cyber-glow look
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Dynamic colors (Android 12+) - optional
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        // Cyber-Glow theme (recommended)
        useCyberGlow -> {
            if (darkTheme) CyberGlowDarkScheme else CyberGlowLightScheme
        }

        // Legacy theme (fallback)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()

            // Make status bar icons visible
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}