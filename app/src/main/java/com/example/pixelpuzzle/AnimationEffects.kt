package com.example.pixelpuzzle

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Shimmer effect that appears when pieces merge correctly
 */
@Composable
fun ShimmerEffect(
    trigger: Boolean,
    modifier: Modifier = Modifier,
    duration: Int = 800,
    color: Color = Color.White
) {
    var shouldShow by remember { mutableStateOf(false) }

    LaunchedEffect(trigger) {
        if (trigger) {
            shouldShow = true
            delay(duration.toLong())
            shouldShow = false
        }
    }

    if (shouldShow) {
        val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
        val shimmerAlpha by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(duration / 2, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "shimmerAlpha"
        )

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = shimmerAlpha * 0.6f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

/**
 * Ripple effect that emanates from a point when pieces merge
 */
@Composable
fun RippleEffect(
    trigger: Boolean,
    center: Offset,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFFFD700),
    rippleCount: Int = 3
) {
    var shouldShow by remember { mutableStateOf(false) }

    LaunchedEffect(trigger) {
        if (trigger) {
            shouldShow = true
            delay(1500)
            shouldShow = false
        }
    }

    if (shouldShow) {
        val ripples = remember { List(rippleCount) { it } }

        ripples.forEach { index ->
            val delay = index * 150

            var scale by remember { mutableStateOf(0f) }
            var alpha by remember { mutableStateOf(1f) }

            LaunchedEffect(Unit) {
                delay(delay.toLong())
                animate(
                    initialValue = 0f,
                    targetValue = 2f,
                    animationSpec = tween(1000, easing = FastOutSlowInEasing)
                ) { value, _ ->
                    scale = value
                    alpha = 1f - value / 2f
                }
            }

            Canvas(modifier = modifier.fillMaxSize()) {
                drawCircle(
                    color = color.copy(alpha = alpha * 0.5f),
                    radius = scale * 100f,
                    center = center
                )
            }
        }
    }
}

/**
 * Pop animation with particle burst effect
 */
@Composable
fun PopEffect(
    trigger: Boolean,
    center: Offset,
    modifier: Modifier = Modifier
) {
    var shouldShow by remember { mutableStateOf(false) }

    LaunchedEffect(trigger) {
        if (trigger) {
            shouldShow = true
            delay(800)
            shouldShow = false
        }
    }

    if (shouldShow) {
        val particles = remember {
            List(12) {
                PopParticle(
                    angle = it * 30f,
                    color = listOf(
                        Color(0xFFFFD700),
                        Color(0xFFFF6B6B),
                        Color(0xFF4ECDC4),
                        Color(0xFFFFE66D)
                    ).random(),
                    startX = center.x,
                    startY = center.y
                )
            }
        }

        particles.forEach { particle ->
            var progress by remember { mutableStateOf(0f) }

            LaunchedEffect(Unit) {
                animate(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                ) { value, _ ->
                    progress = value
                }
            }

            val distance = progress * 80f
            val x = particle.startX + cos(Math.toRadians(particle.angle.toDouble())).toFloat() * distance
            val y = particle.startY + sin(Math.toRadians(particle.angle.toDouble())).toFloat() * distance
            val alpha = 1f - progress

            Canvas(modifier = modifier.fillMaxSize()) {
                drawCircle(
                    color = particle.color.copy(alpha = alpha),
                    radius = 4f,
                    center = Offset(x, y)
                )
            }
        }
    }
}

data class PopParticle(
    val angle: Float,
    val color: Color,
    val startX: Float,
    val startY: Float
)

/**
 * Glow effect for highlighted pieces
 */
@Composable
fun GlowEffect(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFFFD700)
) {
    if (isActive) {
        val infiniteTransition = rememberInfiniteTransition(label = "glow")
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowAlpha"
        )

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = glowAlpha * 0.4f),
                            color.copy(alpha = glowAlpha * 0.2f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

/**
 * Star burst effect for level completion
 */
@Composable
fun StarBurstEffect(
    trigger: Boolean,
    modifier: Modifier = Modifier
) {
    var shouldShow by remember { mutableStateOf(false) }

    LaunchedEffect(trigger) {
        if (trigger) {
            shouldShow = true
            delay(2000)
            shouldShow = false
        }
    }

    if (shouldShow) {
        val starCount = 20
        val stars = remember {
            List(starCount) {
                StarParticle(
                    angle = it * (360f / starCount),
                    color = listOf(
                        Color(0xFFFFD700),
                        Color(0xFFFFA500),
                        Color(0xFFFFE66D),
                        Color(0xFFFFFFFF)
                    ).random(),
                    size = Random.nextInt(4, 12).dp
                )
            }
        }

        stars.forEach { star ->
            var progress by remember { mutableStateOf(0f) }
            var rotation by remember { mutableStateOf(0f) }

            LaunchedEffect(Unit) {
                animate(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = tween(1500, easing = FastOutSlowInEasing)
                ) { value, _ ->
                    progress = value
                    rotation = value * 360f
                }
            }

            val centerX = 0.5f
            val centerY = 0.5f
            val distance = progress * 0.8f
            val x = centerX + cos(Math.toRadians(star.angle.toDouble())).toFloat() * distance
            val y = centerY + sin(Math.toRadians(star.angle.toDouble())).toFloat() * distance
            val alpha = 1f - progress

            Canvas(modifier = modifier.fillMaxSize()) {
                val offsetX = x * size.width
                val offsetY = y * size.height

                drawStar(
                    center = Offset(offsetX, offsetY),
                    radius = star.size.toPx(),
                    color = star.color.copy(alpha = alpha),
                    rotation = rotation
                )
            }
        }
    }
}

data class StarParticle(
    val angle: Float,
    val color: Color,
    val size: Dp
)

/**
 * Draw a star shape
 */
private fun DrawScope.drawStar(
    center: Offset,
    radius: Float,
    color: Color,
    rotation: Float = 0f,
    points: Int = 5
) {
    val path = Path()
    val angleStep = Math.PI * 2 / points
    val innerRadius = radius * 0.4f

    for (i in 0 until points * 2) {
        val angle = i * angleStep / 2 + Math.toRadians(rotation.toDouble())
        val r = if (i % 2 == 0) radius else innerRadius
        val x = center.x + cos(angle).toFloat() * r
        val y = center.y + sin(angle).toFloat() * r

        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()

    drawPath(path = path, color = color)
}

/**
 * Pulse effect for important UI elements
 */
@Composable
fun PulseEffect(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        content()
    }
}