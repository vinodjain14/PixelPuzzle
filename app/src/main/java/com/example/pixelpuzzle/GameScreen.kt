package com.example.pixelpuzzle

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch  // ← ADD THIS

/**
 * Enhanced GameScreen with Phase B improvements:
 * - Squishy tactile buttons
 * - Clean level complete notification (no wobbling)
 * - Micro-interactions on all icons
 * - Static header (no animations)
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

    // ADD THIS - Coroutine scope for launching coroutines
    val coroutineScope = rememberCoroutineScope()

    // Hint dialog states
    val showHintDialog = remember { mutableStateOf(false) }
    val showShopDialog = remember { mutableStateOf(false) }
    var isLoadingAd by remember { mutableStateOf(false) }
    var showAdError by remember { mutableStateOf(false) }
    var adErrorMessage by remember { mutableStateOf("") }

    // Revealed pieces for hint visualization
    var revealedPieceIds by remember { mutableStateOf(setOf<Int>()) }
    var showRevealAnimation by remember { mutableStateOf(false) }

    // Track coin change for flip animation
    var previousPoints by remember { mutableStateOf(currentPoints.value) }
    var coinFlipTrigger by remember { mutableStateOf(false) }

    // Managers
    val soundManager = remember { SoundManager.getInstance(context) }
    val vibrationManager = remember { VibrationManager.getInstance(context) }
    val adManager = remember { AdManager.getInstance(context) }
    val billingManager = remember { BillingManager.getInstance(context) }

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

    // Handle hint selection
    fun handleHintSelected(hintType: HintType) {
        when (hintType) {
            is HintType.FreeHint -> {
                // Check if ad is ready
                if (!adManager.isAdReady()) {
                    isLoadingAd = true
                    adManager.loadRewardedAd(
                        onAdLoaded = {
                            isLoadingAd = false
                            // Show ad
                            activity?.let { act ->
                                adManager.showRewardedAd(
                                    activity = act,
                                    onUserEarnedReward = {
                                        if (HintSystemManager.useFreeHint(context)) {
                                            // FREE HINT: Show Where - Visual guide with arrow
                                            val incorrectPieces = state.pieces.filter { piece ->
                                                val correctPos = piece.originalRow * state.cols + piece.originalCol
                                                piece.currentPos != correctPos
                                            }

                                            if (incorrectPieces.isNotEmpty()) {
                                                val randomPiece = incorrectPieces.random()
                                                revealedPieceIds = setOf(randomPiece.id)
                                                showRevealAnimation = true
                                                vibrationManager.vibrate(VibrationPattern.SUCCESS)
                                                soundManager.playSound(SoundEffect.UNLOCK)

                                                // Clear after 5 seconds
                                                coroutineScope.launch {
                                                    delay(5000)
                                                    showRevealAnimation = false
                                                    revealedPieceIds = emptySet()
                                                }
                                            }
                                        }
                                    },
                                    onAdFailedToShow = { error ->
                                        adErrorMessage = error
                                        showAdError = true
                                    }
                                )
                            }
                        },
                        onAdFailedToLoad = { error ->
                            isLoadingAd = false
                            adErrorMessage = error
                            showAdError = true
                        }
                    )
                } else {
                    // Ad is ready, show it
                    activity?.let { act ->
                        adManager.showRewardedAd(
                            activity = act,
                            onUserEarnedReward = {
                                if (HintSystemManager.useFreeHint(context)) {
                                    val incorrectPieces = state.pieces.filter { piece ->
                                        val correctPos = piece.originalRow * state.cols + piece.originalCol
                                        piece.currentPos != correctPos
                                    }

                                    if (incorrectPieces.isNotEmpty()) {
                                        val randomPiece = incorrectPieces.random()
                                        revealedPieceIds = setOf(randomPiece.id)
                                        showRevealAnimation = true
                                        vibrationManager.vibrate(VibrationPattern.SUCCESS)
                                        soundManager.playSound(SoundEffect.UNLOCK)

                                        coroutineScope.launch {
                                            delay(5000)
                                            showRevealAnimation = false
                                            revealedPieceIds = emptySet()
                                        }
                                    }
                                }
                            },
                            onAdFailedToShow = { error ->
                                adErrorMessage = error
                                showAdError = true
                            }
                        )
                    }
                }
            }
            is HintType.PremiumHint -> {
                val availability = HintSystemManager.getHintAvailability(context)
                if (availability.premiumHintsOwned > 0) {
                    // Use owned hint
                    if (HintSystemManager.usePremiumHint(context)) {
                        // PREMIUM HINT: Auto-solve a 2x2 section
                        val incorrectPieces = state.pieces.filter { piece ->
                            val correctPos = piece.originalRow * state.cols + piece.originalCol
                            piece.currentPos != correctPos
                        }

                        if (incorrectPieces.isNotEmpty()) {
                            val centerPiece = incorrectPieces.random()
                            val revealed = mutableSetOf<Int>()

                            // Get 2x2 area around center piece
                            for (dr in 0..1) {
                                for (dc in 0..1) {
                                    val targetRow = centerPiece.originalRow + dr
                                    val targetCol = centerPiece.originalCol + dc

                                    if (targetRow in 0 until state.rows && targetCol in 0 until state.cols) {
                                        state.pieces.find {
                                            it.originalRow == targetRow && it.originalCol == targetCol
                                        }?.let { revealed.add(it.id) }
                                    }
                                }
                            }

                            revealedPieceIds = revealed
                            showRevealAnimation = true
                            vibrationManager.vibrate(VibrationPattern.SUCCESS)
                            soundManager.playSound(SoundEffect.UNLOCK)

                            // TODO: Actually move pieces to correct positions here
                            // For now, just show where they should go

                            coroutineScope.launch {
                                delay(5000)
                                showRevealAnimation = false
                                revealedPieceIds = emptySet()
                            }
                        }
                    }
                } else {
                    // Purchase with coins
                    if (HintSystemManager.purchasePremiumHint(context)) {
                        currentPoints.value = GamePreferences.getTotalPoints(context)
                        coinFlipTrigger = !coinFlipTrigger

                        val incorrectPieces = state.pieces.filter { piece ->
                            val correctPos = piece.originalRow * state.cols + piece.originalCol
                            piece.currentPos != correctPos
                        }

                        if (incorrectPieces.isNotEmpty()) {
                            val centerPiece = incorrectPieces.random()
                            val revealed = mutableSetOf<Int>()

                            for (dr in 0..1) {
                                for (dc in 0..1) {
                                    val targetRow = centerPiece.originalRow + dr
                                    val targetCol = centerPiece.originalCol + dc

                                    if (targetRow in 0 until state.rows && targetCol in 0 until state.cols) {
                                        state.pieces.find {
                                            it.originalRow == targetRow && it.originalCol == targetCol
                                        }?.let { revealed.add(it.id) }
                                    }
                                }
                            }

                            revealedPieceIds = revealed
                            showRevealAnimation = true
                            vibrationManager.vibrate(VibrationPattern.SUCCESS)
                            soundManager.playSound(SoundEffect.COIN)

                            coroutineScope.launch {
                                delay(5000)
                                showRevealAnimation = false
                                revealedPieceIds = emptySet()
                            }
                        }
                    }
                }
            }
            is HintType.MasterReveal -> {
                val availability = HintSystemManager.getHintAvailability(context)
                if (availability.masterRevealsOwned > 0) {
                    // Use owned reveal
                    if (HintSystemManager.useMasterReveal(context)) {
                        // MASTER REVEAL: Show complete image as transparent overlay
                        // This will show ALL pieces in their correct positions
                        revealedPieceIds = state.pieces.map { it.id }.toSet()
                        showRevealAnimation = true
                        vibrationManager.vibrate(VibrationPattern.SUCCESS)
                        soundManager.playSound(SoundEffect.UNLOCK)

                        coroutineScope.launch {
                            delay(10000) // 10 seconds to study the solution
                            showRevealAnimation = false
                            revealedPieceIds = emptySet()
                        }
                    }
                } else {
                    // Purchase with coins
                    if (HintSystemManager.purchaseMasterReveal(context)) {
                        currentPoints.value = GamePreferences.getTotalPoints(context)
                        coinFlipTrigger = !coinFlipTrigger

                        revealedPieceIds = state.pieces.map { it.id }.toSet()
                        showRevealAnimation = true
                        vibrationManager.vibrate(VibrationPattern.SUCCESS)
                        soundManager.playSound(SoundEffect.COIN)

                        coroutineScope.launch {
                            delay(10000)
                            showRevealAnimation = false
                            revealedPieceIds = emptySet()
                        }
                    }
                }
            }
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
            // Header with glass panel - ALL STATIC, NO ANIMATIONS
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
                    // Flipping coin display with amount
                    FlippingCoin(
                        amount = currentPoints.value,
                        trigger = coinFlipTrigger
                    )

                    // Static level number - no animation
                    Text(
                        text = "LEVEL $level",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonColors.ElectricTeal
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Hint button
                        HintButton(onClick = { showHintDialog.value = true })

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
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Solved Puzzle",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.FillBounds
                                    )
                                }

                                StarBurstEffect(trigger = state.isSolved)
                            }

                            // Clean level complete notification - NO WOBBLING
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .offset(y = (-120).dp),
                                contentAlignment = Alignment.Center
                            ) {
                                LevelCompleteNotification(visible = state.isSolved)
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
                                    onRippleTrigger = { center: androidx.compose.ui.geometry.Offset ->
                                        rippleCenter = center
                                    }
                                )

                                // Hint reveal overlay
                                if (showRevealAnimation && revealedPieceIds.isNotEmpty()) {
                                    HintRevealOverlay(
                                        state = state,
                                        revealedPieceIds = revealedPieceIds,
                                        bitmap = bitmap
                                    )
                                }

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

        if (showHintDialog.value) {
            HintDialog(
                onHintSelected = { hintType ->
                    handleHintSelected(hintType)
                    showHintDialog.value = false
                },
                onDismiss = { showHintDialog.value = false }
            )
        }

        if (showShopDialog.value) {
            HintShopDialog(
                onDismiss = { showShopDialog.value = false },
                onPurchase = { productId ->
                    activity?.let { act ->
                        billingManager.queryProducts { products ->
                            products.find { it.productId == productId }?.let { product ->
                                billingManager.launchPurchaseFlow(act, product)
                            }
                        }
                    }
                }
            )
        }

        if (isLoadingAd) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NeonColors.ElectricTeal)
            }
        }

        if (showAdError) {
            AlertDialog(
                onDismissRequest = { showAdError = false },
                title = { Text("Ad Error") },
                text = { Text(adErrorMessage) },
                confirmButton = {
                    Button(onClick = { showAdError = false }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
fun HintRevealOverlay(
    state: GameState,
    revealedPieceIds: Set<Int>,
    bitmap: Bitmap
) {
    val density = LocalDensity.current
    val infiniteTransition = rememberInfiniteTransition(label = "hint")

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hintGlow"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cellWidth = size.width / state.cols
        val cellHeight = size.height / state.rows

        revealedPieceIds.forEach { pieceId ->
            val piece = state.pieces.find { it.id == pieceId }
            piece?.let {
                val correctPos = it.originalRow * state.cols + it.originalCol
                val correctCol = correctPos % state.cols
                val correctRow = correctPos / state.cols

                // Draw glowing indicator at correct position
                val x = correctCol * cellWidth
                val y = correctRow * cellHeight

                drawRect(
                    color = NeonColors.CyberGreen.copy(alpha = glowAlpha * 0.5f),
                    topLeft = androidx.compose.ui.geometry.Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(cellWidth, cellHeight)
                )

                // Draw border
                drawRect(
                    color = NeonColors.CyberGreen.copy(alpha = glowAlpha),
                    topLeft = androidx.compose.ui.geometry.Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(cellWidth, cellHeight),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                )
            }
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
    val soundManager = remember { SoundManager.getInstance(context) }

    var musicEnabled by remember { mutableStateOf(GamePreferences.isMusicEnabled(context)) }
    var soundEnabled by remember { mutableStateOf(GamePreferences.isSoundEnabled(context)) }
    var vibrationEnabled by remember { mutableStateOf(GamePreferences.isVibrationEnabled(context)) }
    var debugEnabled by remember { mutableStateOf(DebugConfig.isDebugEnabled()) }

    // Hidden developer mode
    var developerModeEnabled by remember { mutableStateOf(GamePreferences.isDeveloperModeEnabled(context)) }
    var tapCount by remember { mutableStateOf(0) }

    // Reset tap count after 2 seconds of no taps
    LaunchedEffect(tapCount) {
        if (tapCount > 0) {
            kotlinx.coroutines.delay(2000)
            tapCount = 0
        }
    }

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
                    color = NeonColors.ElectricTeal,
                    // Hidden tap detector on title
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        tapCount++
                        if (tapCount >= 10) {
                            developerModeEnabled = !developerModeEnabled
                            GamePreferences.setDeveloperModeEnabled(context, developerModeEnabled)
                            soundManager.playSound(if (developerModeEnabled) SoundEffect.UNLOCK else SoundEffect.POP)
                            val vibrationManager = VibrationManager.getInstance(context)
                            vibrationManager.vibrate(if (developerModeEnabled) VibrationPattern.SUCCESS else VibrationPattern.LIGHT_TAP)
                            tapCount = 0
                        }
                    }
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
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

            // Audio & Haptics Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        musicEnabled = !musicEnabled
                        GamePreferences.setMusicEnabled(context, musicEnabled)

                        if (musicEnabled) {
                            soundManager.playBackgroundMusic()
                        } else {
                            soundManager.stopBackgroundMusic()
                        }
                    }
                ) {
                    Text(text = if (musicEnabled) "🎵" else "🔇", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Music", fontSize = 14.sp, color = Color.White)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        soundEnabled = !soundEnabled
                        GamePreferences.setSoundEnabled(context, soundEnabled)

                        if (soundEnabled) {
                            soundManager.playSound(SoundEffect.POP)
                        }
                    }
                ) {
                    Text(text = if (soundEnabled) "🔊" else "🔈", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Sound", fontSize = 14.sp, color = Color.White)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        vibrationEnabled = !vibrationEnabled
                        GamePreferences.setVibrationEnabled(context, vibrationEnabled)

                        if (vibrationEnabled) {
                            val vibrationManager = VibrationManager.getInstance(context)
                            vibrationManager.vibrate(VibrationPattern.LIGHT_TAP)
                        }
                    }
                ) {
                    Text(text = if (vibrationEnabled) "📳" else "📴", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Vibration", fontSize = 14.sp, color = Color.White)
                }
            }

            // Hidden Developer Options (only show when developer mode is enabled)
            if (developerModeEnabled) {
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Developer",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeonColors.ElectricTeal.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            debugEnabled = !debugEnabled
                            DebugConfig.setDebugLogsEnabled(context, debugEnabled)
                            soundManager.playSound(SoundEffect.POP)
                        }
                    ) {
                        Text(text = if (debugEnabled) "🐛" else "🔍", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Debug Logs",
                            fontSize = 14.sp,
                            color = if (debugEnabled) NeonColors.CyberGreen else Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}