package com.example.pixelpuzzle

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Compact flash challenge banner for LevelMapScreen
 */
@Composable
fun FlashChallengeBanner(
    challenge: FlashChallenge?,
    onChallengeClick: (FlashChallenge) -> Unit,
    modifier: Modifier = Modifier
) {
    if (challenge == null) return

    var timeRemaining by remember { mutableStateOf(FlashChallengeManager.getTimeRemaining(challenge)) }

    LaunchedEffect(challenge.id) {
        while (timeRemaining > 0) {
            delay(1000)
            timeRemaining = FlashChallengeManager.getTimeRemaining(challenge)
        }
    }

    if (timeRemaining <= 0) return

    val infiniteTransition = rememberInfiniteTransition(label = "flashBanner")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flashBorder"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onChallengeClick(challenge) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFD700).copy(alpha = borderAlpha),
                            Color(0xFFFFA500).copy(alpha = borderAlpha),
                            Color(0xFFFFD700).copy(alpha = borderAlpha)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFFF9E6),
                            Color(0xFFFFE6CC)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "⚡",
                            fontSize = 24.sp
                        )
                        Text(
                            text = "FLASH CHALLENGE",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF6B6B)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Complete Level ${challenge.level} in ${challenge.timeLimit}s",
                        fontSize = 13.sp,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = getRewardText(challenge.reward),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF6650a4)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = formatTimeRemaining(timeRemaining),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (timeRemaining < 3600) Color(0xFFFF6B6B) else Color.Black
                    )
                    Text(
                        text = "remaining",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

/**
 * Timer display during flash challenge gameplay
 */
@Composable
fun FlashChallengeTimer(
    timeLimit: Int,
    elapsedTime: Int,
    modifier: Modifier = Modifier
) {
    val remainingTime = maxOf(0, timeLimit - elapsedTime)
    val progress = remainingTime.toFloat() / timeLimit
    val isWarning = remainingTime < 30

    val warningAlpha = if (isWarning) {
        val infiniteTransition = rememberInfiniteTransition(label = "warning")
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(500),
                repeatMode = RepeatMode.Reverse
            ),
            label = "warningAlpha"
        ).value
    } else 1f

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isWarning) {
                Color(0xFFFF6B6B).copy(alpha = warningAlpha)
            } else {
                Color(0xFF6650a4)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "⚡",
                    fontSize = 20.sp
                )
                Text(
                    text = formatTime(remainingTime),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .width(120.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
        }
    }
}

/**
 * Challenge result dialog
 */
@Composable
fun FlashChallengeResultDialog(
    success: Boolean,
    reward: ChallengeReward?,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (success) "⚡" else "⏰",
                    fontSize = 64.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (success) "Challenge Complete!" else "Time's Up!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (success) Color(0xFF4CAF50) else Color(0xFFFF6B6B),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (success && reward != null) {
                    Text(
                        text = "You earned:",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = getRewardText(reward),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6650a4),
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = "Try again tomorrow!",
                        fontSize = 16.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6650a4)
                    )
                ) {
                    Text("Continue", fontSize = 18.sp)
                }
            }
        }
    }
}

private fun formatTimeRemaining(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(mins, secs)
}

private fun getRewardText(reward: ChallengeReward): String {
    return when (reward) {
        is ChallengeReward.BonusPoints -> "🪙 ${reward.amount} Bonus Points"
        is ChallengeReward.SpecialTool -> when (reward.type) {
            ToolType.GOLDEN_BRUSH -> "🖌️ Golden Brush (Fill 10 pixels)"
            ToolType.MAGIC_WAND -> "✨ Magic Wand (Auto-complete)"
            ToolType.TIME_FREEZE -> "❄️ Time Freeze (30s pause)"
        }
        is ChallengeReward.ExclusivePalette -> "🎨 Exclusive Palette"
    }
}