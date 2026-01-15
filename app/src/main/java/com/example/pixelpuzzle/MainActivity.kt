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
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
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
                onBackToMap = {
                    navController.navigate(Screen.LevelMap.route) {
                        popUpTo(Screen.LevelMap.route) { inclusive = true }
                    }
                }
            )
        }
    }
}

@Composable
fun GameScreen(
    level: Int,
    onBackToMap: () -> Unit,
    vm: PuzzleViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val showGameSettings = remember { mutableStateOf(false) }
    val currentPoints = remember { mutableStateOf(GamePreferences.getTotalPoints(context)) }
    val difficulty = getDifficultyForLevel(level)

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
                vibrator.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(15)
            }
        }
    }

    LaunchedEffect(level) {
        vm.loadNewGame(context, level)
    }

    LaunchedEffect(state.isSolved) {
        if (state.isSolved) {
            // Save the solved puzzle thumbnail
            vm.getBitmap()?.let { bitmap ->
                GamePreferences.saveLevelThumbnail(context, level, bitmap)
            }

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
                // Left side - Gold coin with points
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = "🪙",
                        fontSize = 32.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${currentPoints.value}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                }

                // Center - Level number only
                Text(
                    text = "LEVEL $level",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                // Right side - Settings
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

            // Fixed size board container - fills available space
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 16.dp),
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

                            // Fixed size for solved image with dark border
                            Box(
                                modifier = Modifier
                                    .width(370.dp)
                                    .height(580.dp)
                                    .graphicsLayer {
                                        this.scaleX = scale
                                        this.scaleY = scale
                                    }
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White) // White border (same as pieces)
                                    .padding(1.dp) // Border thickness (same as pieces)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF2E7D32))
                            ) {
                                SolvedPuzzleWithGrid(bitmap = bitmap, rows = state.rows, cols = state.cols)
                            }
                        } else {
                            // Fixed size for puzzle board
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
                                    onDragStart = { triggerVibration() }
                                )
                            }
                        }
                    }
                }
            }

            // Fixed space for button (reduced padding)
            Box(
                modifier = Modifier.height(64.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state.isSolved) {
                    Button(
                        onClick = onBackToMap,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Next", fontSize = 18.sp)
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

    LaunchedEffect(Unit) {
        delay(10000)
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

    val rows = state.rows
    val cols = state.cols

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
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .onGloballyPositioned { gridSize = it.size }
    ) {
        if (gridSize != IntSize.Zero) {
            val cellWidth = gridSize.width / cols
            val cellHeight = gridSize.height / rows

            // Draw background grid cells (dark green blocks touching each other)
            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(col * cellWidth, row * cellHeight) }
                            .size(
                                with(density) { cellWidth.toDp() },
                                with(density) { cellHeight.toDp() }
                            )
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF2E7D32))
                    )
                }
            }

            // Draw puzzle pieces
            state.pieces.forEachIndexed { index, piece ->
                val isPartOfDraggingUnit = draggingUnitId == piece.unitId
                val baseOffset = IntOffset(
                    (piece.currentPos % cols) * cellWidth,
                    (piece.currentPos / cols) * cellHeight
                )
                val targetOffset = if (isPartOfDraggingUnit && isDragging) baseOffset + dragOffset else baseOffset

                val animatedOffset by animateIntOffsetAsState(
                    targetValue = targetOffset,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "offset"
                )

                val displayOffset = if (isPartOfDraggingUnit && isDragging) baseOffset + dragOffset else animatedOffset
                val isVisible = index < visiblePiecesCount
                val scale by animateFloatAsState(
                    targetValue = if (isVisible) 1f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
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

                // Check for merged neighbors
                val hasRight = state.pieces.any {
                    it.unitId == piece.unitId &&
                            it.originalCol == piece.originalCol + 1 &&
                            it.originalRow == piece.originalRow &&
                            it.currentPos == piece.currentPos + 1
                }
                val hasBottom = state.pieces.any {
                    it.unitId == piece.unitId &&
                            it.originalRow == piece.originalRow + 1 &&
                            it.originalCol == piece.originalCol &&
                            it.currentPos == piece.currentPos + cols
                }
                val hasLeft = state.pieces.any {
                    it.unitId == piece.unitId &&
                            it.originalCol == piece.originalCol - 1 &&
                            it.originalRow == piece.originalRow &&
                            it.currentPos == piece.currentPos - 1
                }
                val hasTop = state.pieces.any {
                    it.unitId == piece.unitId &&
                            it.originalRow == piece.originalRow - 1 &&
                            it.originalCol == piece.originalCol &&
                            it.currentPos == piece.currentPos - cols
                }

                val cornerRadius = 6.dp
                val shape = remember(hasTop, hasRight, hasBottom, hasLeft) {
                    RoundedCornerShape(
                        topStart = if (hasTop || hasLeft) 0.dp else cornerRadius,
                        topEnd = if (hasTop || hasRight) 0.dp else cornerRadius,
                        bottomStart = if (hasBottom || hasLeft) 0.dp else cornerRadius,
                        bottomEnd = if (hasBottom || hasRight) 0.dp else cornerRadius
                    )
                }

                val outerPadding = 0.3.dp
                val borderWidth = 1.dp

                Box(
                    modifier = Modifier
                        .offset { displayOffset }
                        .graphicsLayer {
                            this.scaleX = scale
                            this.scaleY = scale
                            this.alpha = alpha
                            this.rotationY = rotationY
                            this.cameraDistance = 12f * this.density
                        }
                        .zIndex(if (isPartOfDraggingUnit) 1f else 0f)
                        .size(
                            with(density) { cellWidth.toDp() },
                            with(density) { cellHeight.toDp() }
                        )
                        .padding(
                            end = if (!hasRight) outerPadding else 0.dp,
                            bottom = if (!hasBottom) outerPadding else 0.dp,
                            start = if (!hasLeft) outerPadding else 0.dp,
                            top = if (!hasTop) outerPadding else 0.dp
                        )
                        .shadow(if (isPartOfDraggingUnit) 12.dp else 0.dp, shape)
                        .clip(shape)
                        .background(Color.White) // White border
                        .padding(
                            end = if (!hasRight) borderWidth else 0.dp,
                            bottom = if (!hasBottom) borderWidth else 0.dp,
                            start = if (!hasLeft) borderWidth else 0.dp,
                            top = if (!hasTop) borderWidth else 0.dp
                        )
                        .clip(shape)
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
                                        val totalDelta = (deltaY * cols) + deltaX
                                        onUnitMove(unitId, totalDelta)
                                        draggingUnitId = null
                                        dragOffset = IntOffset.Zero
                                        isDragging = false
                                    },
                                    onDragCancel = {
                                        draggingUnitId = null
                                        dragOffset = IntOffset.Zero
                                        isDragging = false
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragOffset += IntOffset(amount.x.roundToInt(), amount.y.roundToInt())
                                    }
                                )
                            }
                        }
                ) {
                    if (rotationY > 90f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF2196F3))
                                .graphicsLayer { this.rotationY = 180f },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "?", color = Color.White, fontSize = 32.sp)
                        }
                    } else {
                        PuzzlePieceImage(piece, bitmap, state)
                    }
                }
            }
        }
    }
}

@Composable
fun PuzzlePieceImage(piece: PuzzlePiece, fullBitmap: Bitmap, gameState: GameState) {
    val pw = fullBitmap.width / gameState.cols
    val ph = fullBitmap.height / gameState.rows
    val cropped = remember(piece.id, fullBitmap, gameState.rows, gameState.cols) {
        try {
            Bitmap.createBitmap(
                fullBitmap,
                piece.originalCol * pw,
                piece.originalRow * ph,
                pw,
                ph
            ).asImageBitmap()
        } catch (e: Exception) {
            Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).asImageBitmap()
        }
    }
    Image(
        bitmap = cropped,
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun SolvedPuzzleWithGrid(bitmap: Bitmap, rows: Int, cols: Int) {
    // Display the complete solved image without any grid lines
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "Solved Puzzle",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.FillBounds
    )
}