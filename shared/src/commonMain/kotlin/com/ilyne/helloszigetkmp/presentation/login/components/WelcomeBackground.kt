package com.ilyne.helloszigetkmp.presentation.login.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WelcomeBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFF4958), // rgb(255, 73, 88)
                            Color(0xFFB83DD8), // rgb(184, 61, 216)
                            Color(0xFF3B00DD), // rgb(59, 0, 221)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                    ),
                ),
        ) {
            // --- FLOATING AMBIENT BACKGROUND CIRCLES ---
            // Top-right background circle
            Box(
                modifier = Modifier
                    .offset(x = 60.dp, y = (-80).dp)
                    .size(260.dp)
                    .align(Alignment.TopEnd)
                    .background(Color.White.copy(alpha = 0.06f), shape = CircleShape),
            )

            // Middle-left background circle
            Box(
                modifier = Modifier
                    .offset(x = (-80).dp, y = 350.dp)
                    .size(200.dp)
                    .align(Alignment.TopStart)
                    .background(Color.White.copy(alpha = 0.04f), shape = CircleShape),
            )

            // Lower-right ambient circle (yellow tone)
            Box(
                modifier = Modifier
                    .offset(x = 30.dp, y = 195.dp)
                    .size(86.dp)
                    .align(Alignment.CenterEnd)
                    .background(Color(0xFFFFDC00).copy(alpha = 0.22f), shape = CircleShape),
            )

            // --- MAIN SCREEN CONTENT SCROLL ---
            Column(
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Mock Top Bar Spacer (Accounts for system status bar or spacing)
                Spacer(modifier = Modifier.height(54.dp))

                // Central Branding Elements
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // Pill Label: Date and Location
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.18f), shape = CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.28f), shape = CircleShape)
                            .padding(horizontal = 18.dp, vertical = 5.dp),
                    ) {
                        Text(
                            text = "AUG 6–11 · BUDAPEST",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 2.sp,
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Big Headline Text with Soft drop-shadow
                    Text(
                        text = "SZIGET",
                        color = Color.White,
                        fontSize = 96.sp,
                        fontWeight = FontWeight.ExtraBold,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.25f),
                                offset = Offset(0f, 6f),
                                blurRadius = 40f,
                            ),
                        ),
                        lineHeight = 72.sp,
                        textAlign = TextAlign.Center,
                    )

                    // Year Label
                    Text(
                        text = "2026",
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 8.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )

                    // Subtitle Slogan
                    Text(
                        text = "Island of Freedom",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 3.sp,
                        modifier = Modifier.padding(top = 10.dp),
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Row of Tag Badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FestivalBadge(text = "🎸 400+ Artists")
                        FestivalBadge(text = "🌍 6 Days")
                        FestivalBadge(text = "🎪 7 Stages")
                    }
                }
            }
        }
    }
}

@Composable
fun FestivalBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.14f), shape = CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.2f), shape = CircleShape)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
