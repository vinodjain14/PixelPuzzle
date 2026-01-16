package com.example.pixelpuzzle

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Dynamic animated backgrounds with shifting gradients
 * Creates ambient, slow-moving color effects
 */

/**
 * Main dynamic background - Slow-shifting gradient
 */
@Composable
fun DynamicGradientBackground(
    modifier: Modifier = Modifier,
    type: BackgroundType = BackgroundType.LEVEL_MAP,
    speed: Float = 1f // 0.5 = slower, 2.0 = faster
) {
    var time by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16) // ~60fps
            time += 0.001f * speed
        }
    }

    val colors = getBackgroundColors(type)

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Create animated gradient positions
        val offsetX1 = width * (0.3f + 0.2f * sin(time * 2 * PI.toFloat()))
        val offsetY1 = height * (0.3f + 0.2f * cos(time * 2 * PI.toFloat()))

        val offsetX2 = width * (0.7f + 0.2f * cos(time * 2 * PI.toFloat() + PI.toFloat()))
        val offsetY2 = height * (0.7f + 0.2f * sin(time * 2 * PI.toFloat() + PI.toFloat()))

        val offsetX3 = width * (0.5f + 0.15f * sin(time * 3 * PI.toFloat()))
        val offsetY3 = height * (0.5f + 0.15f * cos(time * 3 * PI.toFloat()))

        // Multiple gradient layers for depth
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    colors[0].copy(alpha = 0.6f),
                    colors[1].copy(alpha = 0.4f),
                    colors[2].copy(alpha = 0.2f)
                ),
                center = Offset(offsetX1, offsetY1),
                radius = width * 0.8f
            )
        )

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    colors[3].copy(alpha = 0.5f),
                    colors[4].copy(alpha = 0.3f),
                    Color.Transparent
                ),
                center = Offset(offsetX2, offsetY2),
                radius = width * 0.7f
            )
        )

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    colors[5].copy(alpha = 0.4f),
                    Color.Transparent
                ),
                center = Offset(offsetX3, offsetY3),
                radius = width * 0.6f
            )
        )
    }
}

/**
 * Particle background - Floating glowing orbs
 */
@Composable
fun ParticleBackground(
    modifier: Modifier = Modifier,
    particleCount: Int = 20,
    colors: List<Color> = GlassColors.getGradient(GradientType.PRIMARY)
) {
    val particles = remember {
        List(particleCount) {
            Particle(
                x = Math.random().toFloat(),
                y = Math.random().toFloat(),
                speedX = (Math.random().toFloat() - 0.5f) * 0.0005f,
                speedY = (Math.random().toFloat() - 0.5f) * 0.0005f,
                size = (20 + Math.random() * 40).toFloat(),
                color = colors.random()
            )
        }
    }

    var time by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16)
            time += 1f

            particles.forEach { particle ->
                particle.x += particle.speedX
                particle.y += particle.speedY

                // Wrap around edges
                if (particle.x < 0) particle.x = 1f
                if (particle.x > 1) particle.x = 0f
                if (particle.y < 0) particle.y = 1f
                if (particle.y > 1) particle.y = 0f
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { particle ->
            val x = size.width * particle.x
            val y = size.height * particle.y

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        particle.color.copy(alpha = 0.4f),
                        particle.color.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    center = Offset(x, y),
                    radius = particle.size
                ),
                radius = particle.size,
                center = Offset(x, y)
            )
        }
    }
}

/**
 * Wave background - Flowing gradient waves
 */
@Composable
fun WaveBackground(
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(
        GlassColors.neonTeal,
        GlassColors.electricBlue,
        GlassColors.neonPurple
    )
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")

    val offset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1"
    )

    val offset2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        drawRect(
            brush = Brush.linearGradient(
                colors = colors,
                start = Offset(0f, height * offset1),
                end = Offset(width, height * (1f - offset1)),
                tileMode = TileMode.Mirror
            )
        )

        drawRect(
            brush = Brush.linearGradient(
                colors = colors.reversed().map { it.copy(alpha = 0.5f) },
                start = Offset(width * offset2, 0f),
                end = Offset(width * (1f - offset2), height),
                tileMode = TileMode.Mirror
            )
        )
    }
}

/**
 * Mesh gradient background - Modern iOS-style gradient
 */
@Composable
fun MeshGradientBackground(
    modifier: Modifier = Modifier,
    type: BackgroundType = BackgroundType.LEVEL_MAP
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mesh")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val colors = getBackgroundColors(type)

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2

        // Create mesh effect with multiple radial gradients
        for (i in 0..2) {
            val angle = (rotation + i * 120f) * (PI / 180f).toFloat()
            val offsetX = centerX + cos(angle) * width * 0.3f
            val offsetY = centerY + sin(angle) * height * 0.3f

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors[i * 2].copy(alpha = 0.6f),
                        colors[(i * 2 + 1) % colors.size].copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    center = Offset(offsetX, offsetY),
                    radius = width * 0.5f
                )
            )
        }
    }
}

/**
 * Background type determines color scheme
 */
enum class BackgroundType {
    LEVEL_MAP,      // Cool blues and purples
    GAME_PLAY,      // Darker, focused
    CELEBRATION,    // Bright, energetic
    MENU            // Balanced, welcoming
}

/**
 * Get color palette for background type
 */
private fun getBackgroundColors(type: BackgroundType): List<Color> {
    return when (type) {
        BackgroundType.LEVEL_MAP -> listOf(
            Color(0xFF0A0E27),
            GlassColors.neonTeal,
            GlassColors.electricBlue,
            GlassColors.neonPurple,
            Color(0xFF1A1A2E),
            GlassColors.neonPink
        )
        BackgroundType.GAME_PLAY -> listOf(
            Color(0xFF0D1117),
            Color(0xFF161B22),
            GlassColors.neonPurple.copy(alpha = 0.3f),
            Color(0xFF1A1A2E),
            GlassColors.electricBlue.copy(alpha = 0.2f),
            Color(0xFF0A0E27)
        )
        BackgroundType.CELEBRATION -> listOf(
            GlassColors.neonPink,
            GlassColors.glowYellow,
            GlassColors.neonTeal,
            GlassColors.neonOrange,
            GlassColors.cyberGreen,
            GlassColors.sunsetCoral
        )
        BackgroundType.MENU -> listOf(
            Color(0xFF16213E),
            GlassColors.neonTeal,
            Color(0xFF1A1A2E),
            GlassColors.electricBlue,
            GlassColors.neonPurple,
            Color(0xFF0F3460)
        )
    }
}

/**
 * Particle data class
 */
private class Particle(
    var x: Float,
    var y: Float,
    val speedX: Float,
    val speedY: Float,
    val size: Float,
    val color: Color
)

/**
 * Ambient glow overlay - Adds soft glow effect
 */
@Composable
fun AmbientGlow(
    modifier: Modifier = Modifier,
    intensity: Float = 0.3f,
    color: Color = GlassColors.neonTeal
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")

    val alpha by infiniteTransition.animateFloat(
        initialValue = intensity * 0.5f,
        targetValue = intensity,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = alpha),
                    Color.Transparent
                ),
                center = Offset(size.width / 2, size.height / 2),
                radius = size.width * 0.7f
            )
        )
    }
}