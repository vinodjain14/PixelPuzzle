package com.example.pixelpuzzle

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
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

    // Animation state for moving pin
    var showMovingPin by remember { mutableStateOf(false) }
    var animationProgress by remember { mutableStateOf(0f) }
    val previousLevel = remember { mutableStateOf(maxOf(1, unlockedLevels.value - 1)) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Reload thumbnails whenever we return to this screen
    LaunchedEffect(Unit) {
        val oldUnlockedLevels = unlockedLevels.value
        viewModel.loadAllLevelThumbnails(context, unlockedLevels.value)
        unlockedLevels.value = GamePreferences.getUnlockedLevels(context)
        selectedLevel.value = unlockedLevels.value
        totalPoints.value = GamePreferences.getTotalPoints(context)

        // Show pin animation if level increased
        if (unlockedLevels.value > oldUnlockedLevels && unlockedLevels.value > 1) {
            previousLevel.value = unlockedLevels.value - 1
            showMovingPin = true

            // Animate the pin
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

        // Scroll to current level
        val currentLevelIndex = unlockedLevels.value - 1
        if (currentLevelIndex > 0) {
            listState.scrollToItem(maxOf(0, currentLevelIndex - 2))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF5F5F5),
                        Color(0xFFE8E8E8)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Path of Discovery",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6650a4)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "🪙",
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${totalPoints.value}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFFD700)
                        )
                    }
                }

                IconButton(onClick = { showSettings.value = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFF6650a4),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Winding Path with Levels
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

                    // Check if this is part of the animation
                    val isPreviousLevel = level == previousLevel.value
                    val isTargetLevel = level == unlockedLevels.value
                    val showAnimatedPin = showMovingPin && (isPreviousLevel || isTargetLevel)

                    Box {
                        WindingPathNode(
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

                        // Animated moving pin
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

            // Play Button - Skeuomorphic tile style
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clickable(enabled = selectedLevel.value <= unlockedLevels.value) {
                        onLevelClick(selectedLevel.value)
                    },
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 8.dp,
                color = if (selectedLevel.value <= unlockedLevels.value) Color(0xFF6650a4) else Color.Gray
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = if (selectedLevel.value <= unlockedLevels.value) {
                                    listOf(
                                        Color(0xFF7965b3),
                                        Color(0xFF6650a4),
                                        Color(0xFF5a4692)
                                    )
                                } else {
                                    listOf(Color.Gray, Color.DarkGray)
                                }
                            )
                        )
                        .border(
                            width = 2.dp,
                            color = Color.White.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Play Level ${selectedLevel.value}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        if (showSettings.value) {
            LevelMapSettingsDialog(onDismiss = { showSettings.value = false })
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
        // Starting position - fade out pin
        val alpha = 1f - progress
        if (alpha > 0f) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .offset(x = horizontalOffset, y = (-10).dp)
                    .graphicsLayer { this.alpha = alpha },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📍",
                    fontSize = 48.sp
                )
            }
        }
    } else if (currentLevel == toLevel) {
        // Destination level - pin arrives here
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
                Text(
                    text = "📍",
                    fontSize = 40.sp
                )
            }
        }
    }
}

@Composable
fun WindingPathNode(
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

    // Animated ring for current level
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

    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Winding path positioning - alternates left and right
        val horizontalOffset = when {
            level % 4 == 1 -> (-60).dp
            level % 4 == 2 -> 0.dp
            level % 4 == 3 -> 60.dp
            else -> 0.dp
        }

        Box(
            modifier = Modifier
                .offset(x = horizontalOffset)
                .size(if (isCurrent) 88.dp else 72.dp),
            contentAlignment = Alignment.Center
        ) {
            // Animated ring for current level
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .scale(ringScale)
                        .clip(CircleShape)
                        .background(
                            Color(0xFFFFD700).copy(alpha = ringAlpha)
                        )
                )
            }

            // Level Node - Skeuomorphic tile style
            Surface(
                modifier = Modifier
                    .size(if (isCurrent) 80.dp else 68.dp)
                    .clickable(enabled = isUnlocked, onClick = onClick),
                shape = CircleShape,
                shadowElevation = if (isSelected || isCurrent) 12.dp else 6.dp,
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            when {
                                isCurrent -> Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFFD700),
                                        Color(0xFFFFA500),
                                        Color(0xFFFF8C00)
                                    )
                                )
                                isCompleted && thumbnail != null -> Brush.radialGradient(
                                    colors = listOf(Color.White, Color(0xFFF5F5F5))
                                )
                                isSelected -> Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF7965b3),
                                        Color(0xFF6650a4)
                                    )
                                )
                                isUnlocked -> Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFE8DEF8),
                                        Color(0xFFD0BCFF)
                                    )
                                )
                                else -> Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFBDBDBD),
                                        Color(0xFF9E9E9E)
                                    )
                                )
                            }
                        )
                        .border(
                            width = 3.dp,
                            color = when {
                                isCurrent -> Color(0xFFFFD700)
                                isSelected -> Color(0xFF6650a4)
                                isCompleted -> Color(0xFF4CAF50)
                                else -> Color.White.copy(alpha = 0.5f)
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        // Completed level - show thumbnail
                        isCompleted && thumbnail != null -> {
                            Image(
                                bitmap = thumbnail.asImageBitmap(),
                                contentDescription = "Level $level Thumbnail",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        // Current level - show pin/star icon
                        isCurrent -> {
                            Text(
                                text = "⭐",
                                fontSize = 32.sp
                            )
                        }
                        // Unlocked level - show number
                        isUnlocked -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = level.toString(),
                                    fontSize = if (isCurrent) 24.sp else 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFF6650a4)
                                )
                                Text(
                                    text = getDifficultyForLevel(level).displayName,
                                    fontSize = 8.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color(0xFF6650a4).copy(alpha = 0.7f)
                                )
                            }
                        }
                        // Locked level
                        else -> {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }

        // Gradient path line connecting to next node
        if (isNextNode) {
            val pathHeight = 40.dp
            val nextHorizontalOffset = when {
                (level + 1) % 4 == 1 -> (-60).dp
                (level + 1) % 4 == 2 -> 0.dp
                (level + 1) % 4 == 3 -> 60.dp
                else -> 0.dp
            }

            Canvas(
                modifier = Modifier
                    .width(120.dp)
                    .height(pathHeight)
                    .offset(
                        x = (horizontalOffset + nextHorizontalOffset) / 2,
                        y = 0.dp
                    )
            ) {
                val startX = size.width / 2 - with(density) { (nextHorizontalOffset - horizontalOffset).toPx() / 2 }
                val endX = size.width / 2 + with(density) { (nextHorizontalOffset - horizontalOffset).toPx() / 2 }

                // Draw gradient line
                drawLine(
                    brush = Brush.verticalGradient(
                        colors = if (level < (level + 1)) {
                            listOf(
                                Color(0xFF6650a4).copy(alpha = 0.6f),
                                Color(0xFFD0BCFF).copy(alpha = 0.4f)
                            )
                        } else {
                            listOf(
                                Color(0xFFBDBDBD).copy(alpha = 0.4f),
                                Color(0xFFE0E0E0).copy(alpha = 0.3f)
                            )
                        }
                    ),
                    start = Offset(startX, 0f),
                    end = Offset(endX, size.height),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
                )
            }
        }
    }
}

@Composable
fun LevelMapSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var musicEnabled by remember { mutableStateOf(GamePreferences.isMusicEnabled(context)) }
    var soundEnabled by remember { mutableStateOf(GamePreferences.isSoundEnabled(context)) }
    var vibrationEnabled by remember { mutableStateOf(GamePreferences.isVibrationEnabled(context)) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Settings",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("✕", fontSize = 24.sp, color = Color.Gray)
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
                        Text(
                            text = if (musicEnabled) "🎵" else "🔇",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Music",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            soundEnabled = !soundEnabled
                            GamePreferences.setSoundEnabled(context, soundEnabled)
                        }
                    ) {
                        Text(
                            text = if (soundEnabled) "🔊" else "🔈",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Sound",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            vibrationEnabled = !vibrationEnabled
                            GamePreferences.setVibrationEnabled(context, vibrationEnabled)
                        }
                    ) {
                        Text(
                            text = if (vibrationEnabled) "📳" else "📴",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Vibration",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}