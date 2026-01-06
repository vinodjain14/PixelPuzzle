package com.example.pixelpuzzle

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFFBB86FC))) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("puzzle_prefs", android.content.Context.MODE_PRIVATE) }
    val acceptedTerms = remember { mutableStateOf(prefs.getBoolean("accepted_terms", false)) }

    if (acceptedTerms.value) {
        PuzzleApp()
    } else {
        TermsAndConditionsScreen(
            onAccept = {
                prefs.edit().putBoolean("accepted_terms", true).apply()
                acceptedTerms.value = true
            }
        )
    }
}

@Composable
fun TermsAndConditionsScreen(onAccept: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Anam Gaming",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFBB86FC),
            modifier = Modifier.padding(top = 40.dp, bottom = 8.dp)
        )

        Text(
            "Pixel Puzzle",
            fontSize = 24.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Terms and Conditions",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    """
                    Welcome to Anam Gaming's Pixel Puzzle!
                    
                    By using this app, you agree to the following terms:
                    
                    1. Game Rules
                    • Solve puzzles by sliding and merging image pieces
                    • Complete 25 games per set with increasing difficulty
                    • Earn 20 coins for each solved puzzle
                    
                    2. Difficulty Levels
                    • Game 1: 3x3 (Easy)
                    • Game 2: 3x4 (Medium)
                    • Game 3: 4x4 (Hard)
                    • Game 4: 4x5 (Very Hard)
                    • Game 5: 5x5 (Super Hard)
                    Pattern repeats every 5 games
                    
                    3. Privacy
                    • Your progress is saved locally on your device
                    • We use Unsplash API for images
                    • No personal data is collected or shared
                    
                    4. Content
                    • Images are sourced from Unsplash
                    • Content is suitable for all ages
                    • Random images from various categories
                    
                    5. Fair Play
                    • Play fairly and enjoy the challenge
                    • Your coins and progress are stored locally
                    
                    6. Updates
                    • We may update these terms periodically
                    • Continued use means acceptance of changes
                    
                    Have fun and enjoy the challenge!
                    
                    © 2025 Anam Gaming. All rights reserved.
                    """.trimIndent(),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Color.DarkGray
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onAccept,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC))
        ) {
            Text("Accept & Start Playing", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun PuzzleApp(vm: PuzzleViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.loadSavedProgress(context)
        vm.loadNewGame(context)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with level and coins
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Level ${state.currentLevel}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    val config = LevelConfig.getConfigForLevel(state.currentLevel)
                    Text(
                        "${config.difficulty} (${config.rows}x${config.cols})",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text("${config.category.capitalize()}", fontSize = 12.sp, color = Color(0xFFBB86FC))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFFFFF9C4), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("💰", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${state.totalCoins}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF57C00)
                    )
                }
            }

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                } else {
                    vm.getBitmap()?.let { bitmap ->
                        if (state.isSolved) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Solved Puzzle",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.7f)
                                    .background(Color(0xFF1E1E1E)),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            DraggablePuzzleGrid(state, bitmap) { unitId, deltaPos ->
                                vm.onUnitMoveCompleted(unitId, deltaPos, context)
                            }
                        }
                    }
                }
            }

            if (state.isSolved) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("🎉 Congratulations! 🎉", color = Color(0xFF4CAF50), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("+20 Coins Earned!", color = Color(0xFFF57C00), fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }

            Button(
                onClick = {
                    if (state.isSolved) {
                        vm.nextLevel(context)
                    } else {
                        vm.loadNewGame(context)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (state.isSolved) "Next Level" else "New Puzzle", fontSize = 18.sp)
            }
        }

        // Confetti Animation
        if (state.showConfetti) {
            ConfettiAnimation()
        }
    }
}

@Composable
fun ConfettiAnimation() {
    val confettiCount = 50
    val confettiPieces = remember {
        List(confettiCount) {
            ConfettiPiece(
                color = listOf(
                    Color(0xFFFF6B6B), Color(0xFF4ECDC4),
                    Color(0xFFFFD93D), Color(0xFF95E1D3),
                    Color(0xFFF38181), Color(0xFFAA96DA)
                ).random(),
                startX = Random.nextFloat(),
                delay = Random.nextInt(0, 500)
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        confettiPieces.forEach { piece ->
            AnimatedConfettiPiece(piece)
        }
    }
}

data class ConfettiPiece(
    val color: Color,
    val startX: Float,
    val delay: Int
)

@Composable
fun AnimatedConfettiPiece(piece: ConfettiPiece) {
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")

    val yOffset by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000 + piece.delay, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "yOffset"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .offset(x = (piece.startX * 400).dp, y = yOffset.dp)
            .size(12.dp)
            .background(piece.color, CircleShape)
    )
}

@Composable
fun DraggablePuzzleGrid(
    state: GameState,
    bitmap: Bitmap,
    onUnitMove: (Int, Int) -> Unit
) {
    var gridSize by remember { mutableStateOf(IntSize.Zero) }
    var draggingUnitId by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(IntOffset.Zero) }

    val rows = state.gridRows
    val cols = state.gridCols

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .background(Color(0xFF1E1E1E))
            .onGloballyPositioned { gridSize = it.size }
    ) {
        if (gridSize != IntSize.Zero) {
            val cellWidth = gridSize.width / cols
            val cellHeight = gridSize.height / rows

            state.pieces.forEach { piece ->
                val isPartOfDraggingUnit = draggingUnitId == piece.unitId
                val baseOffset = IntOffset(
                    x = (piece.currentPos % cols) * cellWidth,
                    y = (piece.currentPos / cols) * cellHeight
                )

                val animatedOffset by animateIntOffsetAsState(
                    targetValue = if (isPartOfDraggingUnit) baseOffset + dragOffset else baseOffset,
                    label = "offset"
                )

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

                val cornerRadius = 8.dp
                val shape = remember(hasTop, hasRight, hasBottom, hasLeft) {
                    RoundedCornerShape(
                        topStart = if (hasTop || hasLeft) 0.dp else cornerRadius,
                        topEnd = if (hasTop || hasRight) 0.dp else cornerRadius,
                        bottomStart = if (hasBottom || hasLeft) 0.dp else cornerRadius,
                        bottomEnd = if (hasBottom || hasRight) 0.dp else cornerRadius
                    )
                }

                Box(
                    modifier = Modifier
                        .offset { animatedOffset }
                        .zIndex(if (isPartOfDraggingUnit) 1f else 0f)
                        .size(
                            width = (cellWidth / LocalContext.current.resources.displayMetrics.density).dp,
                            height = (cellHeight / LocalContext.current.resources.displayMetrics.density).dp
                        )
                        .then(
                            if (!hasRight && !hasBottom && !hasLeft && !hasTop) {
                                Modifier.padding(1.dp)
                            } else {
                                Modifier.padding(
                                    end = if (!hasRight) 1.dp else 0.dp,
                                    bottom = if (!hasBottom) 1.dp else 0.dp,
                                    start = if (!hasLeft) 1.dp else 0.dp,
                                    top = if (!hasTop) 1.dp else 0.dp
                                )
                            }
                        )
                        .shadow(if (isPartOfDraggingUnit) 12.dp else 0.dp, shape)
                        .clip(shape)
                        .pointerInput(piece.unitId) {
                            detectDragGestures(
                                onDragStart = { draggingUnitId = piece.unitId },
                                onDragEnd = {
                                    val unitId = draggingUnitId ?: return@detectDragGestures

                                    val deltaX = (dragOffset.x.toFloat() / cellWidth).roundToInt()
                                    val deltaY = (dragOffset.y.toFloat() / cellHeight).roundToInt()
                                    val totalDelta = (deltaY * cols) + deltaX

                                    onUnitMove(unitId, totalDelta)

                                    draggingUnitId = null
                                    dragOffset = IntOffset.Zero
                                },
                                onDragCancel = {
                                    draggingUnitId = null
                                    dragOffset = IntOffset.Zero
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += IntOffset(amount.x.roundToInt(), amount.y.roundToInt())
                                }
                            )
                        }
                ) {
                    PuzzlePieceImage(piece, bitmap, cols, rows)
                }
            }
        }
    }
}

@Composable
fun PuzzlePieceImage(piece: PuzzlePiece, fullBitmap: Bitmap, cols: Int, rows: Int) {
    val pw = fullBitmap.width / cols
    val ph = fullBitmap.height / rows
    val cropped = remember(piece.id, fullBitmap) {
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
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
}