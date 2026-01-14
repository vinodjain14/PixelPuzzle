package com.example.pixelpuzzle

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFFBB86FC))) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                    PixelPuzzleApp()
                }
            }
        }
    }
}

@Composable
fun PixelPuzzleApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val hasAcceptedTerms = remember { GamePreferences.hasAcceptedTerms(context) }

    val startDestination = if (hasAcceptedTerms) Screen.LevelMap.route else Screen.Terms.route

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Terms.route) {
            TermsScreen(onAccept = {
                navController.navigate(Screen.LevelMap.route) {
                    popUpTo(Screen.Terms.route) { inclusive = true }
                }
            })
        }

        composable(Screen.LevelMap.route) {
            LevelMapScreen(onLevelClick = { level ->
                navController.navigate(Screen.Game.createRoute(level))
            })
        }

        composable(
            route = Screen.Game.route,
            arguments = listOf(navArgument("level") { type = NavType.IntType })
        ) { backStackEntry ->
            val level = backStackEntry.arguments?.getInt("level") ?: 1
            GameScreen(
                level = level,
                onHome = {
                    navController.navigate(Screen.LevelMap.route) {
                        popUpTo(Screen.LevelMap.route) { inclusive = true }
                    }
                },
                onNextLevel = {
                    val nextLevel = level + 1
                    if (nextLevel <= 20) {
                        navController.navigate(Screen.Game.createRoute(nextLevel)) {
                            popUpTo(Screen.Game.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.LevelMap.route) {
                            popUpTo(Screen.LevelMap.route) { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun GameScreen(
    level: Int,
    onHome: () -> Unit,
    onNextLevel: () -> Unit,
    vm: PuzzleViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    var boardSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val showGameSettings = remember { mutableStateOf(false) }
    val currentPoints = remember { mutableStateOf(GamePreferences.getTotalPoints(context)) }

    fun triggerVibration() {
        if (GamePreferences.isVibrationEnabled(context)) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        }
    }

    LaunchedEffect(level) {
        vm.loadNewGame(context)
    }

    LaunchedEffect(state.isSolved) {
        if (state.isSolved) {
            GamePreferences.addPoints(context, 10)
            GamePreferences.unlockNextLevel(context)
            currentPoints.value = GamePreferences.getTotalPoints(context)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Points: ${currentPoints.value}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6650a4)
                )

                Text(
                    text = "LEVEL $level",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                IconButton(onClick = { showGameSettings.value = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFF6650a4),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 72.dp), // Reserve space for button
                contentAlignment = Alignment.Center
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                                    .size(
                                        width = with(density) { boardSize.width.toDp() },
                                        height = with(density) { boardSize.height.toDp() }
                                    )
                                    .graphicsLayer {
                                        this.scaleX = scale
                                        this.scaleY = scale
                                    }
                                    .background(Color.Black)
                                    .padding(4.dp)
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Solved Puzzle",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.FillBounds
                                )
                            }
                        } else {
                            DraggablePuzzleGrid(
                                state = state,
                                bitmap = bitmap,
                                onUnitMove = vm::onUnitMoveCompleted,
                                onSizeChanged = { size ->
                                    if (boardSize != size) {
                                        boardSize = size
                                        val widthDp = with(density) { size.width.toDp() }
                                        val heightDp = with(density) { size.height.toDp() }
                                        DebugConfig.d("PuzzleBoard", "Board Size: ${size.width}px × ${size.height}px (${widthDp} × ${heightDp})")
                                    }
                                },
                                onDragStart = { triggerVibration() }
                            )
                        }
                    }
                }
            }

            // Fixed space for button (always present, visible or not)
            Box(
                modifier = Modifier.height(72.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state.isSolved) {
                    Button(
                        onClick = onNextLevel,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(if (level < 20) "Next Level" else "Back to Map", fontSize = 18.sp)
                    }
                }
            }
        }

        if (state.isSolved) {
            ConfettiCelebration()
        }

        if (showGameSettings.value) {
            GameSettingsDialog(
                onDismiss = { showGameSettings.value = false },
                onRestart = {
                    showGameSettings.value = false
                    vm.loadNewGame(context)
                },
                onHome = {
                    showGameSettings.value = false
                    onHome()
                }
            )
        }
    }
}

@Composable
fun GameSettingsDialog(onDismiss: () -> Unit, onRestart: () -> Unit, onHome: () -> Unit) {
    val context = LocalContext.current
    var musicEnabled by remember { mutableStateOf(GamePreferences.isMusicEnabled(context)) }
    var soundEnabled by remember { mutableStateOf(GamePreferences.isSoundEnabled(context)) }
    var vibrationEnabled by remember { mutableStateOf(GamePreferences.isVibrationEnabled(context)) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                        text = "Game Menu",
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

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6650a4))
                ) {
                    Text("🔄 Restart Level", fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onHome,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6650a4))
                ) {
                    Text("🏠 Home", fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                HorizontalDivider()

                Spacer(modifier = Modifier.height(24.dp))

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
                            fontSize = 40.sp
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
                            fontSize = 40.sp
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
                            fontSize = 40.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Vibration",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConfettiCelebration() {
    var showConfetti by remember { mutableStateOf(true) }

    // Hide confetti after 10 seconds
    LaunchedEffect(Unit) {
        delay(10000) // 10 seconds
        showConfetti = false
    }

    if (showConfetti) {
        val particles = remember {
            List(30) {
                ConfettiParticle(
                    x = Random.nextFloat(),
                    y = Random.nextFloat() * -0.5f,
                    color = listOf(
                        Color(0xFFFF6B6B), Color(0xFF4ECDC4), Color(0xFFFFE66D),
                        Color(0xFF95E1D3), Color(0xFFF38181), Color(0xFFAA96DA),
                        Color(0xFFFF9FF3), Color(0xFF54A0FF)
                    ).random(),
                    size = (8..20).random().dp
                )
            }
        }

        particles.forEach { particle ->
            var offsetY by remember { mutableStateOf(0f) }

            LaunchedEffect(Unit) {
                while (true) {
                    offsetY = 0f
                    animate(
                        initialValue = 0f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = (2000..3500).random()),
                            repeatMode = RepeatMode.Restart
                        )
                    ) { value, _ -> offsetY = value }
                }
            }

            Box(
                modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.TopStart)
                    .offset { IntOffset((particle.x * 1000).toInt(), (offsetY * 1800).toInt()) }
                    .size(particle.size).clip(CircleShape).background(particle.color)
            )
        }
    }
}

data class ConfettiParticle(val x: Float, val y: Float, val color: Color, val size: androidx.compose.ui.unit.Dp)

@Composable
fun DraggablePuzzleGrid(
    state: GameState,
    bitmap: Bitmap,
    onUnitMove: (Int, Int) -> Unit,
    onSizeChanged: (IntSize) -> Unit = {},
    onDragStart: () -> Unit = {}
) {
    var gridSize by remember { mutableStateOf(IntSize.Zero) }
    var draggingUnitId by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(IntOffset.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    var visiblePiecesCount by remember { mutableStateOf(0) }
    var shouldFlip by remember { mutableStateOf(false) }
    var animationComplete by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    LaunchedEffect(state.pieces.size) {
        if (state.pieces.isNotEmpty() && !animationComplete) {
            visiblePiecesCount = 0
            shouldFlip = false
            animationComplete = false
            for (i in state.pieces.indices) {
                delay(80)
                visiblePiecesCount = i + 1
            }
            delay(300)
            shouldFlip = true
            delay(600)
            animationComplete = true
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF4CAF50)).padding(4.dp)
            .onGloballyPositioned { gridSize = it.size; onSizeChanged(it.size) }
    ) {
        if (gridSize != IntSize.Zero) {
            val cellWidth = gridSize.width / 3
            val cellHeight = gridSize.height / 3

            for (row in 0..2) {
                for (col in 0..2) {
                    Box(
                        modifier = Modifier.offset { IntOffset(col * cellWidth, row * cellHeight) }
                            .size(with(density) { cellWidth.toDp() }, with(density) { cellHeight.toDp() })
                            .padding(1.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF388E3C))
                    )
                }
            }

            state.pieces.forEachIndexed { index, piece ->
                val isPartOfDraggingUnit = draggingUnitId == piece.unitId
                val baseOffset = IntOffset((piece.currentPos % 3) * cellWidth, (piece.currentPos / 3) * cellHeight)
                val targetOffset = if (isPartOfDraggingUnit && isDragging) baseOffset + dragOffset else baseOffset

                val animatedOffset by animateIntOffsetAsState(
                    targetValue = targetOffset,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                    label = "offset"
                )

                val displayOffset = if (isPartOfDraggingUnit && isDragging) baseOffset + dragOffset else animatedOffset
                val isVisible = index < visiblePiecesCount
                val scale by animateFloatAsState(
                    targetValue = if (isVisible) 1f else 0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    label = "scale"
                )
                val alpha by animateFloatAsState(
                    targetValue = if (isVisible) 1f else 0f,
                    animationSpec = tween(durationMillis = 200),
                    label = "alpha"
                )
                val rotationY by animateFloatAsState(
                    targetValue = if (shouldFlip) 0f else 180f,
                    animationSpec = tween(durationMillis = 500),
                    label = "flip"
                )

                val hasRight = state.pieces.any { it.unitId == piece.unitId && it.originalCol == piece.originalCol + 1 && it.originalRow == piece.originalRow && it.currentPos == piece.currentPos + 1 }
                val hasBottom = state.pieces.any { it.unitId == piece.unitId && it.originalRow == piece.originalRow + 1 && it.originalCol == piece.originalCol && it.currentPos == piece.currentPos + 3 }
                val hasLeft = state.pieces.any { it.unitId == piece.unitId && it.originalCol == piece.originalCol - 1 && it.originalRow == piece.originalRow && it.currentPos == piece.currentPos - 1 }
                val hasTop = state.pieces.any { it.unitId == piece.unitId && it.originalRow == piece.originalRow - 1 && it.originalCol == piece.originalCol && it.currentPos == piece.currentPos - 3 }

                val cornerRadius = 8.dp
                val shape = remember(hasTop, hasRight, hasBottom, hasLeft) {
                    RoundedCornerShape(
                        topStart = if (hasTop || hasLeft) 0.dp else cornerRadius,
                        topEnd = if (hasTop || hasRight) 0.dp else cornerRadius,
                        bottomStart = if (hasBottom || hasLeft) 0.dp else cornerRadius,
                        bottomEnd = if (hasBottom || hasRight) 0.dp else cornerRadius
                    )
                }

                val gapSize = 1.dp

                Box(
                    modifier = Modifier.offset { displayOffset }
                        .graphicsLayer { this.scaleX = scale; this.scaleY = scale; this.alpha = alpha; this.rotationY = rotationY; this.cameraDistance = 12f * this.density }
                        .zIndex(if (isPartOfDraggingUnit) 1f else 0f)
                        .size(with(density) { cellWidth.toDp() }, with(density) { cellHeight.toDp() })
                        .padding(end = if (!hasRight) gapSize else 0.dp, bottom = if (!hasBottom) gapSize else 0.dp, start = if (!hasLeft) gapSize else 0.dp, top = if (!hasTop) gapSize else 0.dp)
                        .shadow(if (isPartOfDraggingUnit) 12.dp else 0.dp, shape).clip(shape)
                        .pointerInput(piece.unitId, animationComplete) {
                            if (animationComplete) {
                                detectDragGestures(
                                    onDragStart = {
                                        draggingUnitId = piece.unitId
                                        isDragging = true
                                        onDragStart()
                                    },
                                    onDragEnd = {
                                        val unitId = draggingUnitId ?: return@detectDragGestures
                                        val deltaX = (dragOffset.x.toFloat() / cellWidth).roundToInt()
                                        val deltaY = (dragOffset.y.toFloat() / cellHeight).roundToInt()
                                        val totalDelta = (deltaY * 3) + deltaX
                                        onUnitMove(unitId, totalDelta)
                                        draggingUnitId = null; dragOffset = IntOffset.Zero; isDragging = false
                                    },
                                    onDragCancel = { draggingUnitId = null; dragOffset = IntOffset.Zero; isDragging = false },
                                    onDrag = { change, amount -> change.consume(); dragOffset += IntOffset(amount.x.roundToInt(), amount.y.roundToInt()) }
                                )
                            }
                        }
                ) {
                    if (rotationY > 90f) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color(0xFF2196F3))
                                .graphicsLayer { this.rotationY = 180f },
                            contentAlignment = Alignment.Center
                        ) { Text(text = "?", color = Color.White, fontSize = 32.sp) }
                    } else {
                        PuzzlePieceImage(piece, bitmap)
                    }
                }
            }
        }
    }
}

@Composable
fun PuzzlePieceImage(piece: PuzzlePiece, fullBitmap: Bitmap) {
    val pw = fullBitmap.width / 3
    val ph = fullBitmap.height / 3
    val cropped = remember(piece.id, fullBitmap) {
        try {
            Bitmap.createBitmap(fullBitmap, piece.originalCol * pw, piece.originalRow * ph, pw, ph).asImageBitmap()
        } catch (e: Exception) {
            Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).asImageBitmap()
        }
    }
    Image(bitmap = cropped, contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
}