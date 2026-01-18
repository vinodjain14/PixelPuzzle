package com.example.pixelpuzzle

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pixelpuzzle.ui.theme.GlassUI
import com.example.pixelpuzzle.ui.theme.NeonColors
import com.example.pixelpuzzle.ui.theme.TextColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
fun TermsScreen(onAccept: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var currentTutorialStep by remember { mutableStateOf(0) }
    var showFullTerms by remember { mutableStateOf(false) }

    // Auto-advance tutorial
    LaunchedEffect(Unit) {
        while (currentTutorialStep < 4) {
            delay(3000)
            currentTutorialStep = (currentTutorialStep + 1) % 5
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Dynamic background
        DynamicGradientBackground(
            type = BackgroundType.MENU,
            speed = 0.4f
        )

        // Particle overlay
        ParticleBackground(
            particleCount = 12,
            colors = listOf(NeonColors.ElectricTeal, NeonColors.NeonPurple, NeonColors.NeonPink)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Static game title
            Text(
                text = "Pixel Puzzle",
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NeonColors.ElectricTeal,
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Main content card with glassmorphism
            GlassCard(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                backgroundColor = GlassUI.CardDark,
                borderColor = NeonColors.ElectricTeal.copy(alpha = 0.3f),
                cornerRadius = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(scrollState)
                ) {
                    // Quick Summary Section
                    Text(
                        text = "Quick Summary",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonColors.ElectricTeal
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Visual summary cards
                    VisualSummaryCard(
                        icon = "🏠",
                        title = "Local Storage",
                        description = "All data stays on your device"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    VisualSummaryCard(
                        icon = "🛡️",
                        title = "Privacy First",
                        description = "Zero personal info collected"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    VisualSummaryCard(
                        icon = "🎮",
                        title = "Pure Fun",
                        description = "Made for entertainment only"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    VisualSummaryCard(
                        icon = "🎨",
                        title = "Original Content",
                        description = "By VNAA's Gaming"
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Expandable full terms
                    ExpandableSection(
                        title = "View Full Terms & Conditions",
                        isExpanded = showFullTerms,
                        onToggle = { showFullTerms = !showFullTerms }
                    ) {
                        Text(
                            text = "By playing this game, you agree to the following terms and conditions:\n\n" +
                                    "• This game is provided for entertainment purposes only.\n" +
                                    "• All game data is stored locally on your device.\n" +
                                    "• We do not collect any personal information.\n" +
                                    "• The game content is the property of VNAA's Gaming.\n" +
                                    "• Please play responsibly.",
                            fontSize = 14.sp,
                            color = TextColors.Secondary,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    HorizontalDivider(color = GlassUI.BorderMedium)

                    Spacer(modifier = Modifier.height(32.dp))

                    // Interactive How to Play
                    Text(
                        text = "How to Play",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonColors.ElectricTeal
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tutorial carousel with manual swipe
                    SwipeableTutorialCarousel(
                        currentStep = currentTutorialStep,
                        onStepChange = { currentTutorialStep = it }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Quick tips
                    QuickTipsSection()
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Squishy accept button
            SquishyButton(
                onClick = {
                    GamePreferences.setTermsAccepted(context)
                    onAccept()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = listOf(NeonColors.CyberGreen, NeonColors.ElectricTeal)
            ) {
                Text(
                    "Accept & Start Playing",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun AnimatedTitle() {
    val infiniteTransition = rememberInfiniteTransition(label = "title")

    val color1 by infiniteTransition.animateColor(
        initialValue = NeonColors.ElectricTeal,
        targetValue = NeonColors.NeonPurple,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "titleColor"
    )

    Text(
        text = "VNAA's Gaming",
        fontSize = 40.sp,
        fontWeight = FontWeight.ExtraBold,
        color = color1,
        style = MaterialTheme.typography.headlineLarge
    )
}

@Composable
fun VisualSummaryCard(
    icon: String,
    title: String,
    description: String
) {
    GlassCard(
        backgroundColor = GlassUI.CardLight,
        borderColor = GlassUI.BorderSubtle,
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                NeonColors.ElectricTeal.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 32.sp
                )
            }

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextColors.Primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = TextColors.Secondary
                )
            }
        }
    }
}

@Composable
fun ExpandableSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onToggle)
                .background(GlassUI.CardLight)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextColors.Primary
            )

            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = NeonColors.ElectricTeal,
                modifier = Modifier.size(24.dp)
            )
        }

        // Expandable content
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun SwipeableTutorialCarousel(
    currentStep: Int,
    onStepChange: (Int) -> Unit
) {
    val tutorialSteps = listOf(
        TutorialStep(
            emoji = "👆",
            title = "Slide & Merge",
            description = "Drag puzzle pieces across the board"
        ),
        TutorialStep(
            emoji = "🧩",
            title = "Connect Pieces",
            description = "Pieces auto-merge when correctly positioned"
        ),
        TutorialStep(
            emoji = "🖼️",
            title = "Complete Image",
            description = "Keep merging until the image is whole"
        ),
        TutorialStep(
            emoji = "🔓",
            title = "Unlock Levels",
            description = "Complete levels to unlock new ones"
        ),
        TutorialStep(
            emoji = "🪙",
            title = "Earn Rewards",
            description = "Get 10 coins for every puzzle solved!"
        )
    )

    var dragOffset by remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragOffset.absoluteValue > 100) {
                                if (dragOffset > 0 && currentStep > 0) {
                                    onStepChange(currentStep - 1)
                                } else if (dragOffset < 0 && currentStep < tutorialSteps.size - 1) {
                                    onStepChange(currentStep + 1)
                                }
                            }
                            dragOffset = 0f
                        },
                        onDragCancel = {
                            dragOffset = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragOffset += dragAmount
                        }
                    )
                }
        ) {
            val step = tutorialSteps[currentStep]

            // Animated card
            val scale by animateFloatAsState(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "tutorialScale"
            )

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationX = dragOffset * 0.3f
                    },
                backgroundColor = NeonColors.ElectricTeal.copy(alpha = 0.15f),
                borderColor = NeonColors.ElectricTeal.copy(alpha = 0.4f),
                cornerRadius = 20.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Large emoji
                    Text(
                        text = step.emoji,
                        fontSize = 72.sp,
                        modifier = Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = step.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonColors.ElectricTeal,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = step.description,
                        fontSize = 16.sp,
                        color = TextColors.Secondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Step indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(tutorialSteps.size) { index ->
                StepIndicator(
                    isActive = index == currentStep,
                    onClick = { onStepChange(index) }
                )
                if (index < tutorialSteps.size - 1) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Swipe hint
        Text(
            text = "← Swipe to navigate →",
            fontSize = 12.sp,
            color = TextColors.Tertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun TutorialCarousel(currentStep: Int) {
    val tutorialSteps = listOf(
        TutorialStep(
            emoji = "👆",
            title = "Slide & Merge",
            description = "Drag puzzle pieces across the board"
        ),
        TutorialStep(
            emoji = "🧩",
            title = "Connect Pieces",
            description = "Pieces auto-merge when correctly positioned"
        ),
        TutorialStep(
            emoji = "🖼️",
            title = "Complete Image",
            description = "Keep merging until the image is whole"
        ),
        TutorialStep(
            emoji = "🔓",
            title = "Unlock Levels",
            description = "Complete levels to unlock new ones"
        ),
        TutorialStep(
            emoji = "🪙",
            title = "Earn Rewards",
            description = "Get 10 coins for every puzzle solved!"
        )
    )

    val step = tutorialSteps[currentStep]

    // Animated card
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tutorialScale"
    )

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = NeonColors.ElectricTeal.copy(alpha = 0.15f),
        borderColor = NeonColors.ElectricTeal.copy(alpha = 0.4f),
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large emoji
            Text(
                text = step.emoji,
                fontSize = 72.sp,
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = step.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = NeonColors.ElectricTeal,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = step.description,
                fontSize = 16.sp,
                color = TextColors.Secondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun StepIndicator(
    isActive: Boolean,
    onClick: () -> Unit
) {
    val color by animateColorAsState(
        targetValue = if (isActive) NeonColors.ElectricTeal else GlassUI.BorderMedium,
        label = "indicatorColor"
    )

    val size by animateDpAsState(
        targetValue = if (isActive) 12.dp else 8.dp,
        label = "indicatorSize"
    )

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick)
    )
}

@Composable
fun QuickTipsSection() {
    Column {
        Text(
            text = "💡 Pro Tips",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = NeonColors.GlowYellow
        )

        Spacer(modifier = Modifier.height(12.dp))

        TipItem(
            icon = "⚡",
            text = "Pieces can push other pieces out of the way"
        )

        Spacer(modifier = Modifier.height(8.dp))

        TipItem(
            icon = "🎯",
            text = "Look for corner pieces first - they're easiest"
        )

        Spacer(modifier = Modifier.height(8.dp))

        TipItem(
            icon = "✨",
            text = "Merged pieces move together as one unit"
        )
    }
}

@Composable
fun TipItem(icon: String, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = icon,
            fontSize = 20.sp
        )
        Text(
            text = text,
            fontSize = 14.sp,
            color = TextColors.Secondary,
            lineHeight = 20.sp
        )
    }
}

data class TutorialStep(
    val emoji: String,
    val title: String,
    val description: String
)