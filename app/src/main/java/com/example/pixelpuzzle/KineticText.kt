package com.example.pixelpuzzle

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.pixelpuzzle.ui.theme.NeonColors
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.math.cos

/**
 * Kinetic typography - Bouncy, liquid, stretchy text effects
 * Text that comes alive with personality
 */

/**
 * Main level complete text - Bounces and stretches in
 */
@Composable
fun LevelCompleteText(
    visible: Boolean,
    modifier: Modifier = Modifier,
    text: String = "LEVEL COMPLETE!"
) {
    var animationStarted by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            animationStarted = true
        }
    }

    if (animationStarted) {
        val infiniteTransition = rememberInfiniteTransition(label = "complete")

        // Main scale animation - bouncy entrance
        val scale by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "completeScale"
        )

        // Rotation wobble
        val rotation by infiniteTransition.animateFloat(
            initialValue = -2f,
            targetValue = 2f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "completeRotation"
        )

        // Breathing effect
        val breathe by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "completeBreathe"
        )

        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NeonColors.ElectricTeal,
                modifier = Modifier.graphicsLayer {
                    scaleX = scale * breathe
                    scaleY = scale * breathe
                    rotationZ = rotation
                }
            )
        }
    }
}

/**
 * Bouncy text - Each character bounces individually
 */
@Composable
fun BouncyText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 32.sp,
    color: Color = Color.White,
    delayBetweenChars: Long = 50
) {
    var startAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        startAnimation = true
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        text.forEachIndexed { index, char ->
            var bounceState by remember { mutableStateOf(0f) }

            LaunchedEffect(startAnimation) {
                if (startAnimation) {
                    delay(index * delayBetweenChars)
                    animate(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) { value, _ ->
                        bounceState = value
                    }
                }
            }

            val yOffset = if (bounceState < 1f) {
                -50f * sin(bounceState * Math.PI.toFloat())
            } else {
                0f
            }

            Text(
                text = char.toString(),
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.graphicsLayer {
                    translationY = yOffset
                    alpha = bounceState
                }
            )
        }
    }
}

/**
 * Liquid text - Stretches and squashes
 */
@Composable
fun LiquidText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 36.sp,
    color: Color = NeonColors.NeonPink
) {
    val infiniteTransition = rememberInfiniteTransition(label = "liquid")

    val scaleX by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liquidX"
    )

    val scaleY by infiniteTransition.animateFloat(
        initialValue = 1.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liquidY"
    )

    Text(
        text = text,
        fontSize = fontSize,
        fontWeight = FontWeight.ExtraBold,
        color = color,
        modifier = modifier.graphicsLayer {
            this.scaleX = scaleX
            this.scaleY = scaleY
        }
    )
}

/**
 * Wave text - Characters move in wave pattern
 */
@Composable
fun WaveText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 28.sp,
    color: Color = NeonColors.ElectricTeal
) {
    var time by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16) // ~60fps
            time += 0.05f
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        text.forEachIndexed { index, char ->
            val offset = sin((time + index * 0.5f) * Math.PI.toFloat()) * 10f

            Text(
                text = char.toString(),
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.graphicsLayer {
                    translationY = offset
                }
            )
        }
    }
}

/**
 * Explosive text - Flies in from all directions
 */
@Composable
fun ExplosiveText(
    text: String,
    visible: Boolean,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 40.sp,
    color: Color = NeonColors.NeonPink
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        text.forEachIndexed { index, char ->
            var charState by remember { mutableStateOf(0f) }

            LaunchedEffect(visible) {
                if (visible) {
                    delay(index * 30L)
                    animate(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) { value, _ ->
                        charState = value
                    }
                }
            }

            // Random direction for each character
            val direction = remember { (index % 4) * 90f }
            val distance = 200f * (1f - charState)
            val xOffset = cos(Math.toRadians(direction.toDouble())).toFloat() * distance
            val yOffset = sin(Math.toRadians(direction.toDouble())).toFloat() * distance

            Text(
                text = char.toString(),
                fontSize = fontSize,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                modifier = Modifier.graphicsLayer {
                    translationX = xOffset
                    translationY = yOffset
                    alpha = charState
                    scaleX = charState
                    scaleY = charState
                    rotationZ = (1f - charState) * 360f
                }
            )
        }
    }
}

/**
 * Glitch text - Random jitter effect
 */
@Composable
fun GlitchText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 32.sp,
    color: Color = NeonColors.ElectricBlue,
    glitchIntensity: Float = 5f
) {
    var time by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(50)
            time++
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        text.forEachIndexed { index, char ->
            val glitchX = if (time % 10 == index % 10) {
                (Math.random().toFloat() - 0.5f) * glitchIntensity * 2
            } else {
                0f
            }

            val glitchY = if (time % 8 == index % 8) {
                (Math.random().toFloat() - 0.5f) * glitchIntensity
            } else {
                0f
            }

            Text(
                text = char.toString(),
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.graphicsLayer {
                    translationX = glitchX
                    translationY = glitchY
                }
            )
        }
    }
}

/**
 * Pulsing text - Scale pulses in and out
 */
@Composable
fun PulsingText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 36.sp,
    color: Color = NeonColors.GlowYellow
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Text(
        text = text,
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    )
}