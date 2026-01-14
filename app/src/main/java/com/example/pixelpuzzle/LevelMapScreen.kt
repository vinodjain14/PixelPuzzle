package com.example.pixelpuzzle

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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlin.math.sin

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

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadBackgroundImage(context)
        unlockedLevels.value = GamePreferences.getUnlockedLevels(context)
        selectedLevel.value = unlockedLevels.value
        totalPoints.value = GamePreferences.getTotalPoints(context)

        // Scroll to current level
        val currentLevelIndex = unlockedLevels.value - 1
        if (currentLevelIndex > 0) {
            listState.scrollToItem(maxOf(0, currentLevelIndex - 2))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
    ) {
        // Background Image with overlay
        mapState.backgroundBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Background",
                modifier = Modifier
                    .fillMaxSize()
                    .blur(8.dp),
                contentScale = ContentScale.Crop
            )

            // Dark overlay for better readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Black.copy(alpha = 0.85f),
                                Color.Black.copy(alpha = 0.9f)
                            )
                        )
                    )
            )
        }

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
                        text = "Pixel Puzzle",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Points: ${totalPoints.value}",
                        fontSize = 14.sp,
                        color = Color(0xFFFFD700)
                    )
                }

                IconButton(onClick = { showSettings.value = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Level Path with scrolling
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(LevelPaths.TOTAL_LEVELS) { index ->
                    val level = index + 1
                    val isUnlocked = level <= unlockedLevels.value
                    val isCurrent = level == unlockedLevels.value
                    val isSelected = level == selectedLevel.value

                    LevelPathNode(
                        level = level,
                        isUnlocked = isUnlocked,
                        isCurrent = isCurrent,
                        isSelected = isSelected,
                        onClick = {
                            if (isUnlocked) {
                                selectedLevel.value = level
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Play Button
            Button(
                onClick = { onLevelClick(selectedLevel.value) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6650a4),
                    disabledContainerColor = Color.Gray
                ),
                enabled = selectedLevel.value <= unlockedLevels.value
            ) {
                Text(
                    text = "Play Level ${selectedLevel.value}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (showSettings.value) {
            LevelMapSettingsDialog(onDismiss = { showSettings.value = false })
        }
    }
}

@Composable
fun LevelPathNode(
    level: Int,
    isUnlocked: Boolean,
    isCurrent: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalArrangement = if (level % 2 == 0) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Connecting path line
        if (level > 1) {
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Level Node
        Box(
            modifier = Modifier
                .size(if (isCurrent) 80.dp else 64.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isCurrent -> Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFD700),
                                Color(0xFFFFA500)
                            )
                        )
                        isSelected -> Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF6650a4),
                                Color(0xFF4A3F7A)
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
                                Color(0xFF4A4A4A),
                                Color(0xFF2A2A2A)
                            )
                        )
                    }
                )
                .border(
                    width = if (isSelected || isCurrent) 4.dp else 2.dp,
                    color = if (isCurrent) Color(0xFFFFD700) else if (isSelected) Color.White else Color.Transparent,
                    shape = CircleShape
                )
                .clickable(enabled = isUnlocked, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (isCurrent) {
                // Current level - Show pin icon
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Current Level",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            } else if (isUnlocked) {
                // Unlocked level - Show number
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = level.toString(),
                        fontSize = if (isCurrent) 28.sp else 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else Color(0xFF6650a4)
                    )
                    Text(
                        text = getDifficultyForLevel(level).displayName,
                        fontSize = 8.sp,
                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color(0xFF6650a4).copy(alpha = 0.8f)
                    )
                }
            } else {
                // Locked level
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color.Gray,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Level info label
        if (isCurrent) {
            Spacer(modifier = Modifier.width(16.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFD700).copy(alpha = 0.9f),
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    text = "Current Level",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
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