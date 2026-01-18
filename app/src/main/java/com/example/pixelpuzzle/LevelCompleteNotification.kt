package com.example.pixelpuzzle

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pixelpuzzle.ui.theme.NeonColors
import kotlinx.coroutines.delay

/**
 * Clean, professional level complete notification
 * Scales in with a subtle glow effect - no wobbling
 */
@Composable
fun LevelCompleteNotification(visible: Boolean) {
    var show by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            delay(300)
            show = true
        } else {
            show = false
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (show) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "completeScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (show) 1f else 0f,
        animationSpec = tween(400),
        label = "completeAlpha"
    )

    if (visible) {
        // Subtle pulsing glow effect
        val infiniteTransition = rememberInfiniteTransition(label = "glow")
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowAlpha"
        )

        Box(
            modifier = Modifier
                .graphicsLayer {
                    this.alpha = alpha
                    scaleX = scale
                    scaleY = scale
                }
        ) {
            // Glow background
            Box(
                modifier = Modifier
                    .scale(1.2f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                NeonColors.CyberGreen.copy(alpha = glowAlpha * 0.4f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(40.dp)
            )

            // Glass card
            GlassCard(
                backgroundColor = NeonColors.CyberGreen.copy(alpha = 0.2f),
                borderColor = NeonColors.CyberGreen,
                cornerRadius = 20.dp
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "✓",
                        fontSize = 48.sp,
                        color = NeonColors.CyberGreen
                    )

                    Text(
                        text = "LEVEL COMPLETE",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )

                    Text(
                        text = "Well Done!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}