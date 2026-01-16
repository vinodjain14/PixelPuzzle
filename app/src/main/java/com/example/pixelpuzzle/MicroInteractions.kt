package com.example.pixelpuzzle

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pixelpuzzle.ui.theme.NeonColors
import kotlinx.coroutines.delay

/**
 * Micro-interactions for icons and UI elements
 * Every icon has personality and reacts to user actions
 */

/**
 * Settings gear that spins on hover/click
 */
@Composable
fun SpinningGear(
    modifier: Modifier = Modifier,
    tint: Color = NeonColors.ElectricTeal,
    size: androidx.compose.ui.unit.Dp = 28.dp,
    trigger: Boolean = false
) {
    var rotation by remember { mutableStateOf(0f) }

    LaunchedEffect(trigger) {
        if (trigger) {
            animate(
                initialValue = rotation,
                targetValue = rotation + 360f,
                animationSpec = tween(500, easing = FastOutSlowInEasing)
            ) { value, _ ->
                rotation = value
            }
        }
    }

    Icon(
        imageVector = Icons.Default.Settings,
        contentDescription = "Settings",
        tint = tint,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                rotationZ = rotation
            }
    )
}

/**
 * Heart that beats
 */
@Composable
fun BeatingHeart(
    modifier: Modifier = Modifier,
    tint: Color = NeonColors.NeonPink,
    size: androidx.compose.ui.unit.Dp = 24.dp,
    isBeating: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "heart")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isBeating) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartBeat"
    )

    Icon(
        imageVector = Icons.Default.Favorite,
        contentDescription = "Heart",
        tint = tint,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    )
}

/**
 * Coin that flips and shimmers
 */
@Composable
fun FlippingCoin(
    amount: Int,
    modifier: Modifier = Modifier,
    trigger: Boolean = false
) {
    var rotationY by remember { mutableStateOf(0f) }
    var isShimmering by remember { mutableStateOf(false) }

    LaunchedEffect(trigger) {
        if (trigger) {
            isShimmering = true
            animate(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = tween(600, easing = FastOutSlowInEasing)
            ) { value, _ ->
                rotationY = value
            }
            delay(600)
            isShimmering = false
        }
    }

    val shimmerAlpha = if (isShimmering) {
        val infiniteTransition = rememberInfiniteTransition(label = "coinShimmer")
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(200),
                repeatMode = RepeatMode.Reverse
            ),
            label = "shimmer"
        ).value
    } else {
        1f
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "🪙",
            fontSize = 28.sp,
            modifier = Modifier.graphicsLayer {
                this.rotationY = rotationY
                alpha = shimmerAlpha
            }
        )
        Text(
            text = amount.toString(),
            fontSize = 18.sp,
            color = NeonColors.LaserGold.copy(alpha = shimmerAlpha)
        )
    }
}

/**
 * Streak fire that flickers
 */
@Composable
fun FlickeringFire(
    streakDays: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fire")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fireFlicker"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fireRotate"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "🔥",
            fontSize = 24.sp,
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
            }
        )
        Text(
            text = "$streakDays",
            fontSize = 16.sp,
            color = NeonColors.NeonOrange
        )
    }
}

/**
 * Star that twinkles
 */
@Composable
fun TwinklingStar(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 32.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "star")

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starTwinkle"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starScale"
    )

    Text(
        text = "⭐",
        fontSize = size.value.sp,
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            scaleX = scale
            scaleY = scale
        }
    )
}

/**
 * Lock that shakes when clicked
 */
@Composable
fun ShakingLock(
    modifier: Modifier = Modifier,
    trigger: Boolean = false,
    tint: Color = Color.Gray
) {
    var offsetX by remember { mutableStateOf(0f) }

    LaunchedEffect(trigger) {
        if (trigger) {
            val shakeSequence = listOf(0f, -10f, 10f, -8f, 8f, -4f, 4f, 0f)
            shakeSequence.forEach { offset ->
                offsetX = offset
                delay(50)
            }
        }
    }

    Icon(
        imageVector = Icons.Default.Settings, // Use lock icon if available
        contentDescription = "Locked",
        tint = tint,
        modifier = modifier
            .size(28.dp)
            .graphicsLayer {
                translationX = offsetX
            }
    )
}

/**
 * Progress indicator that fills smoothly
 */
@Composable
fun SmoothProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = NeonColors.CyberGreen
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "progressFill"
    )

    Box(
        modifier = modifier
            .height(8.dp)
            .fillMaxWidth()
    ) {
        // Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = Color.Gray.copy(alpha = 0.3f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f, 20f)
            )
        }

        // Progress
        Canvas(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
        ) {
            drawRoundRect(
                color = color,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f, 20f)
            )
        }
    }
}

/**
 * Badge with pop-in animation
 */
@Composable
fun PopInBadge(
    text: String,
    visible: Boolean,
    modifier: Modifier = Modifier,
    backgroundColor: Color = NeonColors.NeonPink
) {
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "badgePop"
    )

    if (scale > 0f) {
        GlassBadge(
            text = text,
            backgroundColor = backgroundColor,
            modifier = modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        )
    }
}

/**
 * Animated icon with bounce
 */
@Composable
fun BouncyIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = NeonColors.ElectricTeal,
    size: androidx.compose.ui.unit.Dp = 24.dp,
    bounceOnAppear: Boolean = true
) {
    var scale by remember { mutableStateOf(if (bounceOnAppear) 0f else 1f) }

    LaunchedEffect(Unit) {
        if (bounceOnAppear) {
            delay(100)
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) { value, _ ->
                scale = value
            }
        }
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    )
}

/**
 * Floating particle effect around element
 */
@Composable
fun FloatingParticles(
    count: Int = 5,
    color: Color = NeonColors.ElectricTeal,
    modifier: Modifier = Modifier
) {
    var time by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16)
            time += 0.05f
        }
    }

    Canvas(modifier = modifier.size(100.dp)) {
        repeat(count) { index ->
            val angle = (time + index * (360f / count)) * (Math.PI / 180f)
            val radius = 40f + kotlin.math.sin(time * 2 + index).toFloat() * 10f
            val x = center.x + kotlin.math.cos(angle).toFloat() * radius
            val y = center.y + kotlin.math.sin(angle).toFloat() * radius
            val particleSize = 4f + kotlin.math.sin(time * 3 + index).toFloat() * 2f

            drawCircle(
                color = color.copy(alpha = 0.6f),
                radius = particleSize,
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
        }
    }
}