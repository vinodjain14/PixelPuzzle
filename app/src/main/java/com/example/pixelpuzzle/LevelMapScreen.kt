package com.example.pixelpuzzle

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pixelpuzzle.ui.theme.NeonColors
import com.example.pixelpuzzle.ui.theme.GameColors
import com.example.pixelpuzzle.ui.theme.TextColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LevelMapScreen(
    onLevelClick: (Int) -> Unit,
    viewModel: LevelMapViewModel = viewModel()
) {
    val context = LocalContext.current
    val mapState by viewModel.state.collectAsState()
    val unlockedLevels = remember { mutableStateOf(GamePreferences.getUnlockedLevels(context)) }
    val selectedLevel = remember { mutableStateOf(unlockedLevels.value) }
    val showSettings = remember { mutableStateOf(false) }
    val totalPoints = remember { mutableStateOf(GamePreferences.getTotalPoints(context)) }

    // Phase 2: Streak & Flash Challenge states
    val streakInfo = remember { mutableStateOf(DailyStreakManager.getStreakInfo(context)) }
    val showStreakDialog = remember { mutableStateOf(false) }
    val showStreakCelebration = remember { mutableStateOf(false) }
    val flashChallenge = remember { mutableStateOf(FlashChallengeManager.getActiveChallenge(context)) }

    // Animation state for moving pin
    var showMovingPin by remember { mutableStateOf(false) }
    var animationProgress by remember { mutableStateOf(0f) }
    val previousLevel = remember { mutableStateOf(maxOf(1, unlockedLevels.value - 1)) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val oldUnlockedLevels = unlockedLevels.value
        viewModel.loadAllLevelThumbnails(context, unlockedLevels.value)
        unlockedLevels.value = GamePreferences.getUnlockedLevels(context)
        selectedLevel.value = unlockedLevels.value
        totalPoints.value = GamePreferences.getTotalPoints(context)

        val newStreakInfo = DailyStreakManager.updateDailyStreak(context)
        streakInfo.value = newStreakInfo

        if (newStreakInfo.isNewStreak && newStreakInfo.currentStreak > 1) {
            showStreakCelebration.value = true
        }

        flashChallenge.value = FlashChallengeManager.getActiveChallenge(context)

        if (unlockedLevels.value > oldUnlockedLevels && unlockedLevels.value > 1) {
            previousLevel.value = unlockedLevels.value - 1
            showMovingPin = true

            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
            ) { value, _ ->
                animationProgress = value
            }

            delay(1600)
            showMovingPin = false
            animationProgress = 0f
        }

        val currentLevelIndex = unlockedLevels.value - 1
        if (currentLevelIndex > 0) {
            listState.scrollToItem(maxOf(0, currentLevelIndex - 2))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Dynamic gradient background
        DynamicGradientBackground(
            type = BackgroundType.LEVEL_MAP,
            speed = 0.5f
        )

        // Particle overlay for depth
        ParticleBackground(
            particleCount = 15,
            colors = listOf(NeonColors.ElectricTeal, NeonColors.NeonPurple)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Glass header panel
            FrostedPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Path of Discovery",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonColors.ElectricTeal,
                                style = MaterialTheme.typography.headlineMedium
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                // Coins with glass badge
                                GlassBadge(
                                    text = "🪙 ${totalPoints.value}",
                                    backgroundColor = GameColors.CoinGold
                                )

                                // Streak with glass badge
                                GlassBadge(
                                    text = "🔥 ${streakInfo.value.currentStreak}",
                                    backgroundColor = NeonColors.NeonOrange,
                                    modifier = Modifier.clickable { showStreakDialog.value = true }
                                )
                            }
                        }

                        IconButton(
                            onClick = { showSettings.value = true },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            NeonColors.ElectricTeal.copy(alpha = 0.3f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = NeonColors.ElectricTeal,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Flash Challenge with glass effect
            flashChallenge.value?.let { challenge ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLevelClick(challenge.level) },
                    backgroundColor = NeonColors.NeonOrange.copy(alpha = 0.2f),
                    borderColor = NeonColors.NeonOrange,
                    cornerRadius = 20.dp
                ) {
                    FlashChallengeBanner(
                        challenge = challenge,
                        onChallengeClick = { onLevelClick(it.level) },
                        modifier = Modifier.background(Color.Transparent)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Winding Path with glass effect
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(LevelPaths.TOTAL_LEVELS) { index ->
                    val level = index + 1
                    val isUnlocked = level <= unlockedLevels.value
                    val isCurrent = level == unlockedLevels.value
                    val isCompleted = level < unlockedLevels.value
                    val isSelected = level == selectedLevel.value

                    val isPreviousLevel = level == previousLevel.value
                    val isTargetLevel = level == unlockedLevels.value
                    val showAnimatedPin = showMovingPin && (isPreviousLevel || isTargetLevel)

                    Box {
                        WindingPathNodeGlass(
                            level = level,
                            isUnlocked = isUnlocked,
                            isCurrent = isCurrent,
                            isCompleted = isCompleted,
                            isSelected = isSelected,
                            thumbnail = mapState.levelThumbnails[level],
                            isNextNode = index < LevelPaths.TOTAL_LEVELS - 1,
                            onClick = {
                                if (isUnlocked) {
                                    selectedLevel.value = level
                                }
                            }
                        )

                        if (showAnimatedPin && showMovingPin) {
                            MovingPinAnimation(
                                fromLevel = previousLevel.value,
                                toLevel = unlockedLevels.value,
                                currentLevel = level,
                                progress = animationProgress
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Glass Play Button
            GlassButton(
                onClick = { onLevelClick(selectedLevel.value) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                enabled = selectedLevel.value <= unlockedLevels.value,
                backgroundColor = if (selectedLevel.value <= unlockedLevels.value) {
                    NeonColors.ElectricTeal
                } else {
                    Color.Gray
                }
            ) {
                Text(
                    text = "Play Level ${selectedLevel.value}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Dialogs with glass effect
        if (showSettings.value) {
            GlassSettingsDialog(onDismiss = { showSettings.value = false })
        }

        if (showStreakDialog.value) {
            StreakDialog(onDismiss = { showStreakDialog.value = false })
        }

        if (showStreakCelebration.value) {
            StreakCelebration(
                show = showStreakCelebration.value,
                streakCount = streakInfo.value.currentStreak,
                onDismiss = { showStreakCelebration.value = false }
            )
        }
    }
}

@Composable
fun WindingPathNodeGlass(
    level: Int,
    isUnlocked: Boolean,
    isCurrent: Boolean,
    isCompleted: Boolean,
    isSelected: Boolean,
    thumbnail: android.graphics.Bitmap?,
    isNextNode: Boolean,
    onClick: () -> Unit
) {
    val density = LocalDensity.current

    val infiniteTransition = rememberInfiniteTransition(label = "ring")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        val horizontalOffset = when {
            level % 4 == 1 -> (-60).dp
            level % 4 == 2 -> 0.dp
            level % 4 == 3 -> 60.dp
            else -> 0.dp
        }

        Box(
            modifier = Modifier
                .offset(x = horizontalOffset)
                .size(if (isCurrent) 96.dp else 80.dp),
            contentAlignment = Alignment.Center
        ) {
            // Animated glow for current level
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .scale(ringScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    GameColors.LevelCurrent.copy(alpha = glowAlpha),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // Glass level node
            GlassSurface(
                modifier = Modifier
                    .size(if (isCurrent) 88.dp else 72.dp)
                    .clickable(enabled = isUnlocked, onClick = onClick),
                backgroundColor = when {
                    isCurrent -> GameColors.LevelCurrent.copy(alpha = 0.3f)
                    isCompleted && thumbnail != null -> GameColors.LevelCompleted.copy(alpha = 0.2f)
                    isSelected -> NeonColors.ElectricTeal.copy(alpha = 0.3f)
                    isUnlocked -> NeonColors.ElectricBlue.copy(alpha = 0.2f)
                    else -> GameColors.LevelLocked.copy(alpha = 0.2f)
                },
                cornerRadius = 36.dp,
                elevation = if (isSelected || isCurrent) 12.dp else 4.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isCompleted && thumbnail != null -> {
                            Image(
                                bitmap = thumbnail.asImageBitmap(),
                                contentDescription = "Level $level Thumbnail",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        isCurrent -> {
                            Text(text = "⭐", fontSize = 36.sp)
                        }
                        isUnlocked -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = level.toString(),
                                    fontSize = if (isCurrent) 26.sp else 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextColors.Primary
                                )
                                Text(
                                    text = getDifficultyForLevel(level).displayName,
                                    fontSize = 9.sp,
                                    color = TextColors.Secondary
                                )
                            }
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = TextColors.Disabled,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }

        // Neon path line
        if (isNextNode) {
            val pathHeight = 40.dp
            val nextHorizontalOffset = when {
                (level + 1) % 4 == 1 -> (-60).dp
                (level + 1) % 4 == 2 -> 0.dp
                (level + 1) % 4 == 3 -> 60.dp
                else -> 0.dp
            }

            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .width(120.dp)
                    .height(pathHeight)
                    .offset(x = (horizontalOffset + nextHorizontalOffset) / 2, y = 0.dp)
            ) {
                val startX = size.width / 2 - with(density) { (nextHorizontalOffset - horizontalOffset).toPx() / 2 }
                val endX = size.width / 2 + with(density) { (nextHorizontalOffset - horizontalOffset).toPx() / 2 }

                drawLine(
                    brush = Brush.linearGradient(
                        colors = if (level < (level + 1)) {
                            listOf(
                                NeonColors.ElectricTeal.copy(alpha = 0.6f),
                                NeonColors.ElectricBlue.copy(alpha = 0.4f)
                            )
                        } else {
                            listOf(
                                Color.Gray.copy(alpha = 0.3f),
                                Color.Gray.copy(alpha = 0.2f)
                            )
                        }
                    ),
                    start = Offset(startX, 0f),
                    end = Offset(endX, size.height),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f))
                )
            }
        }
    }
}

@Composable
fun MovingPinAnimation(
    fromLevel: Int,
    toLevel: Int,
    currentLevel: Int,
    progress: Float
) {
    val horizontalOffset = when {
        currentLevel % 4 == 1 -> (-60).dp
        currentLevel % 4 == 2 -> 0.dp
        currentLevel % 4 == 3 -> 60.dp
        else -> 0.dp
    }

    if (currentLevel == fromLevel) {
        val alpha = 1f - progress
        if (alpha > 0f) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .offset(x = horizontalOffset, y = (-10).dp)
                    .graphicsLayer { this.alpha = alpha },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "📍", fontSize = 48.sp)
            }
        }
    } else if (currentLevel == toLevel) {
        val scale by animateFloatAsState(
            targetValue = if (progress > 0.8f) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "pinScale"
        )

        if (progress > 0.5f) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .offset(x = horizontalOffset, y = (-15).dp)
                    .scale(scale),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "📍", fontSize = 40.sp)
            }
        }
    }
}

@Composable
fun GlassSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var musicEnabled by remember { mutableStateOf(GamePreferences.isMusicEnabled(context)) }
    var soundEnabled by remember { mutableStateOf(GamePreferences.isSoundEnabled(context)) }
    var vibrationEnabled by remember { mutableStateOf(GamePreferences.isVibrationEnabled(context)) }

    GlassDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonColors.ElectricTeal
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Text("✕", fontSize = 24.sp, color = TextColors.Secondary)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        musicEnabled = !musicEnabled
                        GamePreferences.setMusicEnabled(context, musicEnabled)
                    }
                ) {
                    Text(text = if (musicEnabled) "🎵" else "🔇", fontSize = 52.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Music", fontSize = 14.sp, color = TextColors.Secondary)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        soundEnabled = !soundEnabled
                        GamePreferences.setSoundEnabled(context, soundEnabled)
                    }
                ) {
                    Text(text = if (soundEnabled) "🔊" else "🔈", fontSize = 52.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Sound", fontSize = 14.sp, color = TextColors.Secondary)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        vibrationEnabled = !vibrationEnabled
                        GamePreferences.setVibrationEnabled(context, vibrationEnabled)
                    }
                ) {
                    Text(text = if (vibrationEnabled) "📳" else "📴", fontSize = 52.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Vibration", fontSize = 14.sp, color = TextColors.Secondary)
                }
            }
        }
    }
}