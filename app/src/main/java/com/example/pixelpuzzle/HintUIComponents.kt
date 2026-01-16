package com.example.pixelpuzzle

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * Hint button in game screen
 */
@Composable
fun HintButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val availability = remember { HintSystemManager.getHintAvailability(context) }

    val infiniteTransition = rememberInfiniteTransition(label = "hint")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hintGlow"
    )

    Surface(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = CircleShape,
        shadowElevation = 8.dp,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFD700).copy(alpha = glowAlpha),
                            Color(0xFFFFA500).copy(alpha = glowAlpha)
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    color = Color.White.copy(alpha = 0.5f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "💡",
                    fontSize = 24.sp
                )
                if (availability.freeHintsRemaining > 0) {
                    Text(
                        text = "${availability.freeHintsRemaining}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Hint selection dialog
 */
@Composable
fun HintDialog(
    onHintSelected: (HintType) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var availability by remember { mutableStateOf(HintSystemManager.getHintAvailability(context)) }
    val costs = remember { HintSystemManager.getHintCosts() }
    val totalCoins = remember { mutableStateOf(GamePreferences.getTotalPoints(context)) }

    // Refresh availability when dialog opens
    LaunchedEffect(Unit) {
        availability = HintSystemManager.getHintAvailability(context)
        totalCoins.value = GamePreferences.getTotalPoints(context)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💡 Need a Hint?",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6650a4)
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("✕", fontSize = 24.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Current coins display
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "🪙", fontSize = 20.sp)
                    Text(
                        text = "${totalCoins.value} Coins",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFD700)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Free Hint Option
                HintOptionCard(
                    icon = "📺",
                    title = "Free Hint",
                    description = "Watch an ad to reveal 1 correct piece",
                    availability = if (availability.freeHintsRemaining > 0) {
                        "Available (${availability.freeHintsRemaining} left today)"
                    } else {
                        "No free hints remaining"
                    },
                    costText = "Watch Ad",
                    isAvailable = availability.freeHintsRemaining > 0,
                    buttonColor = Color(0xFF4CAF50),
                    onClick = {
                        onHintSelected(HintType.FreeHint)
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Premium Hint Option
                HintOptionCard(
                    icon = "✨",
                    title = "Premium Hint",
                    description = "Reveal a 3×3 section (9 pieces)",
                    availability = if (availability.premiumHintsOwned > 0) {
                        "Owned: ${availability.premiumHintsOwned}"
                    } else if (availability.canAffordPremiumHint) {
                        "Available"
                    } else {
                        "Insufficient coins"
                    },
                    costText = if (availability.premiumHintsOwned > 0) {
                        "Use Owned"
                    } else {
                        "${costs.premiumHintCost} Coins"
                    },
                    isAvailable = availability.premiumHintsOwned > 0 || availability.canAffordPremiumHint,
                    buttonColor = Color(0xFF6650a4),
                    onClick = {
                        onHintSelected(HintType.PremiumHint)
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Master Reveal Option
                HintOptionCard(
                    icon = "👑",
                    title = "Master Reveal",
                    description = "Reveal entire quadrant (¼ of puzzle)",
                    availability = if (availability.masterRevealsOwned > 0) {
                        "Owned: ${availability.masterRevealsOwned}"
                    } else if (availability.canAffordMasterReveal) {
                        "Available"
                    } else {
                        "Insufficient coins"
                    },
                    costText = if (availability.masterRevealsOwned > 0) {
                        "Use Owned"
                    } else {
                        "${costs.masterRevealCost} Coins"
                    },
                    isAvailable = availability.masterRevealsOwned > 0 || availability.canAffordMasterReveal,
                    buttonColor = Color(0xFFFFD700),
                    onClick = {
                        onHintSelected(HintType.MasterReveal)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
fun HintOptionCard(
    icon: String,
    title: String,
    description: String,
    availability: String,
    costText: String,
    isAvailable: Boolean,
    buttonColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAvailable) Color(0xFFF5F5F5) else Color(0xFFEEEEEE)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = icon, fontSize = 32.sp)

                    Column {
                        Text(
                            text = title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAvailable) Color.Black else Color.Gray
                        )
                        Text(
                            text = description,
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = availability,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isAvailable) Color(0xFF4CAF50) else Color(0xFFFF6B6B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = isAvailable,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text(costText, fontSize = 16.sp)
            }
        }
    }
}

/**
 * Shop dialog for purchasing hints with real money
 */
@Composable
fun HintShopDialog(
    onDismiss: () -> Unit,
    onPurchase: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🛒 Hint Shop",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6650a4)
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("✕", fontSize = 24.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Coin Packs
                    item {
                        Text(
                            text = "Coin Packs",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    items(getCoinPackages()) { product ->
                        ShopItemCard(
                            product = product,
                            onPurchase = { onPurchase(product.productId) }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    // Hint Packs
                    item {
                        Text(
                            text = "Hint Packs",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    items(getHintPackages()) { product ->
                        ShopItemCard(
                            product = product,
                            onPurchase = { onPurchase(product.productId) }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    // Special Offers
                    item {
                        Text(
                            text = "Special",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    item {
                        ShopItemCard(
                            product = IAPProduct(
                                productId = BillingManager.PRODUCT_REMOVE_ADS,
                                title = "Remove Ads Forever",
                                description = "Enjoy ad-free experience",
                                price = "$2.99",
                                icon = "🚫"
                            ),
                            onPurchase = { onPurchase(BillingManager.PRODUCT_REMOVE_ADS) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShopItemCard(
    product: IAPProduct,
    onPurchase: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = product.icon, fontSize = 32.sp)

                Column {
                    Text(
                        text = product.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = product.description,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }

            Button(
                onClick = onPurchase,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6650a4)
                )
            ) {
                Text(product.price, fontSize = 14.sp)
            }
        }
    }
}

private fun getCoinPackages(): List<IAPProduct> {
    return listOf(
        IAPProduct(
            productId = BillingManager.PRODUCT_50_COINS,
            title = "50 Coins",
            description = "Small coin pack",
            price = "$0.99",
            icon = "🪙"
        ),
        IAPProduct(
            productId = BillingManager.PRODUCT_150_COINS,
            title = "150 Coins",
            description = "Medium coin pack",
            price = "$1.99",
            icon = "💰"
        ),
        IAPProduct(
            productId = BillingManager.PRODUCT_500_COINS,
            title = "500 Coins",
            description = "Large coin pack",
            price = "$4.99",
            icon = "💎"
        ),
        IAPProduct(
            productId = BillingManager.PRODUCT_1000_COINS,
            title = "1000 Coins",
            description = "Mega coin pack (Best Value!)",
            price = "$7.99",
            icon = "👑"
        )
    )
}

private fun getHintPackages(): List<IAPProduct> {
    return listOf(
        IAPProduct(
            productId = BillingManager.PRODUCT_5_PREMIUM_HINTS,
            title = "5 Premium Hints",
            description = "Reveal 3×3 sections",
            price = "$1.99",
            icon = "✨"
        ),
        IAPProduct(
            productId = BillingManager.PRODUCT_10_PREMIUM_HINTS,
            title = "10 Premium Hints",
            description = "Best value for hints!",
            price = "$2.99",
            icon = "⭐"
        ),
        IAPProduct(
            productId = BillingManager.PRODUCT_3_MASTER_REVEALS,
            title = "3 Master Reveals",
            description = "Reveal entire quadrants",
            price = "$3.99",
            icon = "👑"
        )
    )
}