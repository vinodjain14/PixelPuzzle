package com.example.pixelpuzzle

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFFBB86FC))) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                    PuzzleApp()
                }
            }
        }
    }
}

@Composable
fun PuzzleApp(vm: PuzzleViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) { vm.loadNewGame(context) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Pixel Puzzle", fontSize = 32.sp, color = Color.Black, modifier = Modifier.padding(top = 16.dp))
            Text("Slide & Merge Units", color = Color.Gray, modifier = Modifier.padding(bottom = 32.dp))

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                } else {
                    vm.getBitmap()?.let { bitmap ->
                        if (state.isSolved) {
                            // Show complete original image when solved with scale animation
                            val scale by animateFloatAsState(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "solvedScale"
                            )

                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Solved Puzzle",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
                                    .graphicsLayer {
                                        this.scaleX = scale
                                        this.scaleY = scale
                                    }
                                    .shadow(16.dp, RoundedCornerShape(16.dp))
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF1E1E1E)),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            DraggablePuzzleGrid(state, bitmap, vm::onUnitMoveCompleted)
                        }
                    }
                }
            }

            Button(
                onClick = { vm.loadNewGame(context) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("New Puzzle")
            }
        }

        // Confetti celebration when solved
        if (state.isSolved) {
            ConfettiCelebration()
        }
    }
}

@Composable
fun ConfettiCelebration() {
    // Create 30 random confetti particles
    val particles = remember {
        List(30) {
            ConfettiParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * -0.5f,
                color = listOf(
                    Color(0xFFFF6B6B),
                    Color(0xFF4ECDC4),
                    Color(0xFFFFE66D),
                    Color(0xFF95E1D3),
                    Color(0xFFF38181),
                    Color(0xFFAA96DA),
                    Color(0xFFFF9FF3),
                    Color(0xFF54A0FF)
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
                androidx.compose.animation.core.animate(
                    initialValue = 0f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = (2000..3500).random()),
                        repeatMode = RepeatMode.Restart
                    )
                ) { value, _ ->
                    offsetY = value
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.TopStart)
                .offset {
                    IntOffset(
                        x = (particle.x * 1000).toInt(),
                        y = (offsetY * 1800).toInt()
                    )
                }
                .size(particle.size)
                .clip(CircleShape)
                .background(particle.color)
        )
    }
}

data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val color: Color,
    val size: androidx.compose.ui.unit.Dp
)

@Composable
fun DraggablePuzzleGrid(
    state: GameState,
    bitmap: Bitmap,
    onUnitMove: (Int, Int) -> Unit
) {
    var gridSize by remember { mutableStateOf(IntSize.Zero) }
    var draggingUnitId by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(IntOffset.Zero) }
    var isDragging by remember { mutableStateOf(false) }

    // Animation state for piece loading
    var visiblePiecesCount by remember { mutableStateOf(0) }
    var allPiecesLoaded by remember { mutableStateOf(false) }
    var shouldFlip by remember { mutableStateOf(false) }
    var animationComplete by remember { mutableStateOf(false) }

    // Trigger staggered animation when pieces change
    LaunchedEffect(state.pieces.size) {
        if (state.pieces.isNotEmpty() && !animationComplete) {
            visiblePiecesCount = 0
            allPiecesLoaded = false
            shouldFlip = false
            animationComplete = false

            // Show pieces one by one (flipped/hidden)
            for (i in state.pieces.indices) {
                delay(80) // Delay between each piece appearing
                visiblePiecesCount = i + 1
            }

            // All pieces are now visible (but still flipped)
            allPiecesLoaded = true

            // Wait a moment then flip all pieces at once
            delay(300)
            shouldFlip = true

            // Wait for flip animation to complete
            delay(600)
            animationComplete = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF4CAF50)) // Green background
            .padding(2.dp) // Padding around the entire grid
            .onGloballyPositioned { gridSize = it.size }
    ) {
        if (gridSize != IntSize.Zero) {
            val cellWidth = gridSize.width / 3
            val cellHeight = gridSize.height / 3

            // Draw the 9 green grid blocks with rounded corners
            for (row in 0..2) {
                for (col in 0..2) {
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = col * cellWidth,
                                    y = row * cellHeight
                                )
                            }
                            .size(
                                width = (cellWidth / LocalContext.current.resources.displayMetrics.density).dp,
                                height = (cellHeight / LocalContext.current.resources.displayMetrics.density).dp
                            )
                            .padding(1.dp) // Reduced gap between blocks
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF388E3C)) // Darker green for blocks
                    )
                }
            }

            // Draw the puzzle pieces on top
            state.pieces.forEachIndexed { index, piece ->
                val isPartOfDraggingUnit = draggingUnitId == piece.unitId
                val baseOffset = IntOffset(
                    x = (piece.currentPos % 3) * cellWidth,
                    y = (piece.currentPos / 3) * cellHeight
                )

                // Only animate when NOT actively dragging
                val targetOffset = if (isPartOfDraggingUnit && isDragging) {
                    baseOffset + dragOffset
                } else {
                    baseOffset
                }

                val animatedOffset by animateIntOffsetAsState(
                    targetValue = targetOffset,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "offset"
                )

                // Use the immediate offset during dragging, animated otherwise
                val displayOffset = if (isPartOfDraggingUnit && isDragging) {
                    baseOffset + dragOffset
                } else {
                    animatedOffset
                }

                // Loading animation
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

                // Flip animation - all pieces flip together
                val rotationY by animateFloatAsState(
                    targetValue = if (shouldFlip) 0f else 180f,
                    animationSpec = tween(durationMillis = 500),
                    label = "flip"
                )

                // Check which sides are connected to other pieces in the same unit
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
                            it.currentPos == piece.currentPos + 3
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
                            it.currentPos == piece.currentPos - 3
                }

                // Create a shape with selective rounded corners
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
                        .offset { displayOffset }
                        .graphicsLayer {
                            this.scaleX = scale
                            this.scaleY = scale
                            this.alpha = alpha
                            this.rotationY = rotationY
                            this.cameraDistance = 12f * density
                        }
                        .zIndex(if (isPartOfDraggingUnit) 1f else 0f)
                        .size(
                            width = (cellWidth / LocalContext.current.resources.displayMetrics.density).dp,
                            height = (cellHeight / LocalContext.current.resources.displayMetrics.density).dp
                        )
                        .then(
                            // Only add padding to edges that are NOT connected - this creates seamless merges
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
                        .pointerInput(piece.unitId, animationComplete) {
                            // Only allow dragging after animation is complete
                            if (animationComplete) {
                                detectDragGestures(
                                    onDragStart = {
                                        draggingUnitId = piece.unitId
                                        isDragging = true
                                    },
                                    onDragEnd = {
                                        val unitId = draggingUnitId ?: return@detectDragGestures

                                        val deltaX = (dragOffset.x.toFloat() / cellWidth).roundToInt()
                                        val deltaY = (dragOffset.y.toFloat() / cellHeight).roundToInt()
                                        val totalDelta = (deltaY * 3) + deltaX

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
                    // Show back side or front side based on rotation
                    if (rotationY > 90f) {
                        // Back side - show a card back design
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF2196F3)) // Blue card back
                                .graphicsLayer {
                                    this.rotationY = 180f // Flip the back to appear correctly
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "?",
                                color = Color.White,
                                fontSize = 32.sp
                            )
                        }
                    } else {
                        // Front side - show actual puzzle piece
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