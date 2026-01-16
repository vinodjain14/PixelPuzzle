package com.example.pixelpuzzle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Glassmorphism components for modern, layered UI design
 * Creates frosted glass effect with blur and transparency
 */

/**
 * Glass Card - Main container with frosted glass effect
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = GlassColors.cardBackground,
    borderColor: Color = GlassColors.cardBorder,
    blurRadius: Dp = 20.dp,
    cornerRadius: Dp = 24.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = 0.7f),
                        backgroundColor.copy(alpha = 0.5f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        borderColor.copy(alpha = 0.8f),
                        borderColor.copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
    ) {
        content()
    }
}

/**
 * Glass Dialog - Full-screen dialog with blur background
 */
@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismissRequest) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Blurred backdrop
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .blur(radius = 10.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            )

            // Glass content
            GlassCard(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                cornerRadius = 32.dp
            ) {
                content()
            }
        }
    }
}

/**
 * Glass Button - Interactive button with glass effect
 */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = GlassColors.buttonBackground,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            backgroundColor.copy(alpha = 0.8f),
                            backgroundColor.copy(alpha = 0.6f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            GlassColors.buttonBorder.copy(alpha = 0.8f),
                            GlassColors.buttonBorder.copy(alpha = 0.3f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row {
                content()
            }
        }
    }
}

/**
 * Glass Surface - Subtle glass effect for smaller components
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    backgroundColor: Color = GlassColors.surfaceBackground,
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 4.dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        color = Color.Transparent,
        shadowElevation = elevation
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            backgroundColor.copy(alpha = 0.6f),
                            backgroundColor.copy(alpha = 0.4f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = GlassColors.surfaceBorder.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(cornerRadius)
                )
        ) {
            content()
        }
    }
}

/**
 * Glass Badge - Small glass element for indicators
 */
@Composable
fun GlassBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = GlassColors.badgeBackground,
    textColor: Color = Color.White
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = 0.9f),
                        backgroundColor.copy(alpha = 0.7f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

/**
 * Glass Overlay - Full-screen overlay with blur
 */
@Composable
fun GlassOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (visible) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(GlassColors.overlayBackground)
                .blur(radius = 8.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
        ) {
            content()
        }
    }
}

/**
 * Frosted glass panel for headers/footers
 */
@Composable
fun FrostedPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        GlassColors.panelBackground.copy(alpha = 0.95f),
                        GlassColors.panelBackground.copy(alpha = 0.85f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(0.dp)
            )
    ) {
        content()
    }
}

/**
 * Glass color palette - Cyber-glow neon-pastel theme
 */
object GlassColors {
    // Card backgrounds
    val cardBackground = Color(0xFF1A1A2E)
    val cardBorder = Color(0xFF00D9FF)

    // Button colors
    val buttonBackground = Color(0xFF6650A4)
    val buttonBorder = Color(0xFF9D8FFF)

    // Surface colors
    val surfaceBackground = Color(0xFF16213E)
    val surfaceBorder = Color(0xFF00FFF5)

    // Badge colors
    val badgeBackground = Color(0xFFFF6B9D)

    // Overlay
    val overlayBackground = Color(0xFF000000).copy(alpha = 0.5f)

    // Panel
    val panelBackground = Color(0xFF0F1419)

    // Cyber-glow gradients
    val neonTeal = Color(0xFF00FFF5)
    val neonPink = Color(0xFFFF006E)
    val neonPurple = Color(0xFF9D4EDD)
    val neonOrange = Color(0xFFFF7F11)
    val electricBlue = Color(0xFF00D9FF)
    val sunsetCoral = Color(0xFFFF6B9D)
    val cyberGreen = Color(0xFF00FF88)
    val glowYellow = Color(0xFFFFD60A)

    /**
     * Get gradient for specific use cases
     */
    fun getGradient(type: GradientType): List<Color> {
        return when (type) {
            GradientType.PRIMARY -> listOf(neonTeal, electricBlue)
            GradientType.SECONDARY -> listOf(neonPink, sunsetCoral)
            GradientType.SUCCESS -> listOf(cyberGreen, neonTeal)
            GradientType.WARNING -> listOf(neonOrange, glowYellow)
            GradientType.ACCENT -> listOf(neonPurple, neonPink)
            GradientType.DARK -> listOf(Color(0xFF1A1A2E), Color(0xFF16213E))
        }
    }
}

enum class GradientType {
    PRIMARY,
    SECONDARY,
    SUCCESS,
    WARNING,
    ACCENT,
    DARK
}

/**
 * Glass theme configuration
 */
object GlassTheme {
    val smallCornerRadius = 12.dp
    val mediumCornerRadius = 16.dp
    val largeCornerRadius = 24.dp
    val xlCornerRadius = 32.dp

    val thinBorder = 1.dp
    val mediumBorder = 2.dp
    val thickBorder = 3.dp

    val subtleBlur = 8.dp
    val mediumBlur = 16.dp
    val strongBlur = 24.dp
}