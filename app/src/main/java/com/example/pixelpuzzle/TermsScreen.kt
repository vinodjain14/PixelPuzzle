package com.example.pixelpuzzle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TermsScreen(onAccept: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "VNAA's Gaming",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6650a4)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Pixel Puzzle",
                fontSize = 24.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(40.dp))

            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = "Terms & Conditions",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(
                        text = "By playing this game, you agree to the following terms and conditions:",
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = "• This game is provided for entertainment purposes only.\n" +
                                "• All game data is stored locally on your device.\n" +
                                "• We do not collect any personal information.\n" +
                                "• The game content is the property of VNAA's Gaming.\n" +
                                "• Please play responsibly.",
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    Text(
                        text = "How to Play",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(
                        text = "1. Slide & Merge: Drag puzzle pieces to slide them across the board.\n\n" +
                                "2. Connect Pieces: When correct pieces touch in the right position, they automatically merge.\n\n" +
                                "3. Complete the Image: Keep merging pieces until you reconstruct the complete image.\n\n" +
                                "4. Unlock Levels: Complete each level to unlock the next one.\n\n" +
                                "5. Earn Points: Get 10 points for every puzzle you solve!",
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    GamePreferences.setTermsAccepted(context)
                    onAccept()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6650a4))
            ) {
                Text("Accept & Continue", fontSize = 18.sp)
            }
        }
    }
}
