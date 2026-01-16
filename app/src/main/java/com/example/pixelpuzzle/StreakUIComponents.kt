package com.example.pixelpuzzle

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

/**
 * Compact streak indicator for LevelMapScreen header
 */
@Composable
fun StreakIndicator(
    streakInfo: StreakInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "streak")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "streakGlow"
    )

    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = if (streakInfo.currentStreak >= 3) {
                            listOf(
                                Color(0xFFFF6B6B).copy(alpha = glowAlpha),
                                Color(0xFFFF8E53).copy(alpha = glowAlpha)
                            )
                        } else {
                            listOf(
                                Color(0xFFE0E0E0),
                                Color(0xFFBDBDBD)
                            )
                        }
                    )
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "🔥",
                    fontSize = 20.sp
                )
                Text(
                    text = "${streakInfo.currentStreak} Day${if (streakInfo.currentStreak != 1) "s" else ""}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Full streak details dialog
 */
@Composable
fun StreakDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val streakInfo = remember { DailyStreakManager.getStreakInfo(context) }
    val availableRewards = remember { DailyStreakManager.getAvailableRewards(context) }
    var claimedRewardId by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔥 Daily Streak",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6650a4)
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("✕", fontSize = 24.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Current Streak Display
                StreakProgressCard(streakInfo)

                Spacer(modifier = Modifier.height(24.dp))

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatCard(
                        icon = "👑",
                        value = streakInfo.longestStreak.toString(),
                        label = "Best Streak"
                    )
                    StatCard(
                        icon = "📅",
                        value = streakInfo.totalPlayDays.toString(),
                        label = "Total Days"
                    )
                    StatCard(
                        icon = if (streakInfo.playedToday) "✅" else "⏰",
                        value = if (streakInfo.playedToday) "Done" else "Play",
                        label = "Today"
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Rewards Section
                Text(
                    text = "Streak Rewards",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(StreakReward.allRewards) { reward ->
                        RewardCard(
                            reward = reward,
                            currentStreak = streakInfo.currentStreak,
                            isAvailable = availableRewards.contains(reward),
                            isClaimed = claimedRewardId == reward.id,
                            onClaim = {
                                if (DailyStreakManager.claimReward(context, reward)) {
                                    claimedRewardId = reward.id
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StreakProgressCard(streakInfo: StreakInfo) {
    val progress = (streakInfo.currentStreak % 7) / 7f
    val nextMilestone = ((streakInfo.currentStreak / 7) + 1) * 7

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF9E6)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${streakInfo.currentStreak}",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6B6B)
            )
            Text(
                text = "Day Streak! 🔥",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF6650a4)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Progress to next milestone
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${streakInfo.currentStreak} days",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "$nextMilestone days",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFFFF6B6B),
                    trackColor = Color(0xFFFFE0E0)
                )
            }

            if (streakInfo.isNewRecord) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "🎉 New Personal Record!",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6B6B)
                )
            }
        }
    }
}

@Composable
fun StatCard(icon: String, value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Text(
            text = icon,
            fontSize = 32.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RewardCard(
    reward: StreakReward,
    currentStreak: Int,
    isAvailable: Boolean,
    isClaimed: Boolean,
    onClaim: () -> Unit
) {
    val isLocked = currentStreak < reward.requiredDays
    val progress = if (isLocked) {
        (currentStreak.toFloat() / reward.requiredDays).coerceIn(0f, 1f)
    } else 1f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isClaimed -> Color(0xFFE8F5E9)
                isAvailable -> Color(0xFFFFF9E6)
                else -> Color(0xFFF5F5F5)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isClaimed -> Color(0xFF4CAF50)
                                isAvailable -> Color(0xFFFFD700)
                                else -> Color(0xFFBDBDBD)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isClaimed) "✅" else reward.icon,
                        fontSize = 24.sp
                    )
                }

                // Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reward.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLocked) Color.Gray else Color.Black
                    )
                    Text(
                        text = reward.description,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    if (isLocked) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "$currentStreak/${reward.requiredDays} days",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = Color(0xFF6650a4),
                                trackColor = Color(0xFFE0E0E0)
                            )
                        }
                    }
                }
            }

            // Claim Button
            if (isAvailable && !isClaimed) {
                Button(
                    onClick = onClaim,
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6650a4)
                    )
                ) {
                    Text("Claim", fontSize = 14.sp)
                }
            } else if (isClaimed) {
                Text(
                    text = "Claimed!",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}

/**
 * Streak celebration animation when hitting milestones
 */
@Composable
fun StreakCelebration(
    show: Boolean,
    streakCount: Int,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(show) {
        if (show) {
            visible = true
            delay(3000)
            visible = false
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable {
                    visible = false
                    onDismiss()
                },
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔥",
                        fontSize = 80.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "$streakCount Day Streak!",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6650a4)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Keep it up! 🎉",
                        fontSize = 18.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}