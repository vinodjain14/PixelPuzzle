package com.example.pixelpuzzle

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pixelpuzzle.ui.theme.NeonColors
import com.example.pixelpuzzle.ui.theme.GameColors
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Enhanced GameScreen with Phase B improvements:
 * - Squishy tactile buttons
 * - Kinetic level complete text
 * - Micro-interactions on all icons
 */
@Composable
fun GameScreenPhaseB(
    level: Int,
    onBackToMap: () -> Unit,
    vm: PuzzleViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val mergeEvent by vm.mergeEvent.collectAsState()
    val context = LocalContext.current
    val activity = context as? androidx.activity.ComponentActivity
    val showGameSettings = remember { mutableStateOf(false) }
    val currentPoints = remember { mutableStateOf(GamePreferences.getTotalPoints(context)) }

    // Track coin change for flip animation
    var previousPoints by remember { mutableStateOf(currentPoints.value) }
    var coinFlipTrigger by remember { mutableStateOf(false) }

    // Managers
    val soundManager = remember { SoundManager.getInstance(context) }
    val vibrationManager = remember { VibrationManager.getInstance(context) }

    // Animation triggers
    var showShimmer by remember { mutableStateOf(false) }
    var showRipple by remember { mutableStateOf(false) }
    var rippleCenter by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var settingsGearTrigger by remember { mutableStateOf(false) }

    // Handle merge events
    LaunchedEffect(mergeEvent) {
        mergeEvent?.let { event ->
            when (event.type) {
                MergeEventType.MERGE -> {
                    vibrationManager.vibrate(VibrationPattern.MERGE)
                    soundManager.playSound(SoundEffect.MERGE)
                    showShimmer = true
                    showRipple = true
                    delay(800)
                    showShimmer = false
                    showRipple = false
                }
                MergeEventType.COMPLETE -> {
                    vibrationManager.vibrate(VibrationPattern.COMPLETE)
                    soundManager.playSound(SoundEffect.COMPLETE)
                }
                MergeEventType.ERROR -> {
                    vibrationManager.vibrate(VibrationPattern.ERROR)
                    soundManager.playSound(SoundEffect.ERROR)
                }
            }
            vm.clearMergeEvent()
        }
    }

    LaunchedEffect(level) {
        vm.loadNewGame(context, level)
    }

    LaunchedEffect(state.isSolved) {
        if (state.isSolved) {
            vm.getBitmap()?.let { bitmap ->
                GamePreferences.saveLevelThumbnail(context, level, bitmap)
            }

            GamePreferences.addPoints(context, 10)
            GamePreferences.unlockNextLevel(context)

            // Trigger coin flip
            previousPoints = currentPoints.value
            currentPoints.value = GamePreferences.getTotalPoints(context)
            coinFlipTrigger = !coinFlipTrigger

            soundManager.playSound(SoundEffect.COIN)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Dynamic background
        DynamicGradientBackground(
            type = BackgroundType.GAME_PLAY,
            speed = 0.3f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with glass panel
            FrostedPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Flipping coin with amount
                    FlippingCoin(
                        amount = currentPoints.value,
                        trigger = coinFlipTrigger
                    )

                    // Level number with kinetic effect
                    WaveText(
                        text = "LEVEL $level",
                        fontSize = 22.sp,
                        color = NeonColors.ElectricTeal
                    )

                    // Settings with spinning gear
                    SquishyIconButton(
                        onClick = {
                            settingsGearTrigger = !settingsGearTrigger
                            showGameSettings.value = true
                        },
                        backgroundColor = NeonColors.ElectricTeal.copy(alpha = 0.2f)
                    ) {
                        SpinningGear(
                            tint = NeonColors.ElectricTeal,
                            size = 24.dp,
                            trigger = settingsGearTrigger
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Game board
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = NeonColors.ElectricTeal)
                } else {
                    vm.getBitmap()?.let { bitmap ->
                        if (state.isSolved) {
                            val scale by animateFloatAsState(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "solvedScale"
                            )

                            Box(
                                modifier = Modifier
                                    .width(370.dp)
                                    .height(580.dp)
                                    .graphicsLayer {
                                        this.scaleX = scale
                                        this.scaleY = scale
                                    }
                            ) {
                                // Solved puzzle with glass effect
                                GlassCard(
                                    backgroundColor = GameColors.LevelCompleted.copy(alpha = 0.1f),
                                    borderColor = GameColors.LevelCompleted,
                                    cornerRadius = 6.dp
                                ) {
                                    SolvedPuzzleWithGrid(
                                        bitmap = bitmap,
                                        rows = state.rows,
                                        cols = state.cols
                                    )
                                }

                                StarBurstEffect(trigger = state.isSolved)
                            }

                            // Kinetic level complete text
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .offset(y = (-100).dp),
                                contentAlignment = Alignment.Center
                            ) {
                                LevelCompleteText(
                                    visible = state.isSolved,
                                    text = "LEVEL COMPLETE!"
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .width(370.dp)
                                    .height(580.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                DraggablePuzzleGrid(
                                    state = state,
                                    bitmap = bitmap,
                                    onUnitMove = vm::onUnitMoveCompleted,
                                    onDragStart = {
                                        vibrationManager.vibrate(VibrationPattern.LIGHT_TAP)
                                    },
                                    onRippleTrigger = { center ->
                                        rippleCenter = center
                                    }
                                )

                                ShimmerEffect(
                                    trigger = showShimmer,
                                    modifier = Modifier.matchParentSize()
                                )

                                if (showRipple) {
                                    RippleEffect(
                                        trigger = showRipple,
                                        center = rippleCenter,
                                        modifier = Modifier.matchParentSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Squishy next button
            Box(
                modifier = Modifier.height(64.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state.isSolved) {
                    SquishyButton(
                        onClick = {
                            vibrationManager.vibrate(VibrationPattern.MEDIUM_TAP)
                            soundManager.playSound(SoundEffect.UNLOCK)
                            onBackToMap()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = listOf(NeonColors.CyberGreen, NeonColors.ElectricTeal)
                    ) {
                        Text(
                            "Next Level",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        if (showGameSettings.value) {
            GameSettingsDialogTactile(
                onDismiss = { showGameSettings.value = false },
                onRestart = {
                    showGameSettings.value = false
                    vm.restartCurrentGame()
                },
                onHome = {
                    showGameSettings.value = false
                    onBackToMap()
                }
            )
        }
    }
}

@Composable
fun GameSettingsDialogTactile(
    onDismiss: () -> Unit,
    onRestart: () -> Unit,
    onHome: () -> Unit
) {
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
                    text = "Game Menu",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonColors.ElectricTeal
                )

                SquishyIconButton(
                    onClick = onDismiss,
                    backgroundColor = Color.Transparent
                ) {
                    Text("✕", fontSize = 24.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Squishy buttons
            SquishyButton(
                onClick = onRestart,
                modifier = Modifier.fillMaxWidth(),
                colors = listOf(NeonColors.NeonOrange, NeonColors.NeonPink)
            ) {
                Text("🔄 Restart Level", fontSize = 18.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            SquishyButton(
                onClick = onHome,
                modifier = Modifier.fillMaxWidth(),
                colors = listOf(NeonColors.NeonPurple, NeonColors.ElectricBlue)
            ) {
                Text("🏠 Home", fontSize = 18.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

            Spacer(modifier = Modifier.height(24.dp))

            // Squishy toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SquishyToggle(
                    checked = musicEnabled,
                    onCheckedChange = {
                        musicEnabled = it
                        GamePreferences.setMusicEnabled(context, it)
                    },
                    label = "Music"
                )

                SquishyToggle(
                    checked = soundEnabled,
                    onCheckedChange = {
                        soundEnabled = it
                        GamePreferences.setSoundEnabled(context, it)
                    },
                    label = "Sound"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SquishyToggle(
                checked = vibrationEnabled,
                onCheckedChange = {
                    vibrationEnabled = it
                    GamePreferences.setVibrationEnabled(context, it)
                },
                label = "Vibration"
            )
        }
    }
}