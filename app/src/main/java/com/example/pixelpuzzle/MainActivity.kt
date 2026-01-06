package com.example.pixelpuzzle

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt

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
                        // Show complete original image when solved
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Solved Puzzle",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
                                .background(Color(0xFF1E1E1E)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        DraggablePuzzleGrid(state, bitmap, vm::onUnitMoveCompleted)
                    }
                }
            }
        }

        if (state.isSolved) {
            Text("Solved!", color = Color.Green, fontSize = 24.sp, modifier = Modifier.padding(16.dp))
        }

        Button(
            onClick = { vm.loadNewGame(context) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("New Puzzle")
        }
    }
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
            .background(Color(0xFF1E1E1E))
            .onGloballyPositioned { gridSize = it.size }
    ) {
        if (gridSize != IntSize.Zero) {
            val cellWidth = gridSize.width / 3
            val cellHeight = gridSize.height / 3

            state.pieces.forEach { piece ->
                val isPartOfDraggingUnit = draggingUnitId == piece.unitId
                val baseOffset = IntOffset(
                    x = (piece.currentPos % 3) * cellWidth,
                    y = (piece.currentPos / 3) * cellHeight
                )

                val animatedOffset by animateIntOffsetAsState(
                    targetValue = if (isPartOfDraggingUnit) baseOffset + dragOffset else baseOffset,
                    label = "offset"
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
                        .offset { animatedOffset }
                        .zIndex(if (isPartOfDraggingUnit) 1f else 0f)
                        .size(
                            width = (cellWidth / LocalContext.current.resources.displayMetrics.density).dp,
                            height = (cellHeight / LocalContext.current.resources.displayMetrics.density).dp
                        )
                        .then(
                            // Only add padding to edges that are NOT connected
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
                                    val totalDelta = (deltaY * 3) + deltaX

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
                    PuzzlePieceImage(piece, bitmap)
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