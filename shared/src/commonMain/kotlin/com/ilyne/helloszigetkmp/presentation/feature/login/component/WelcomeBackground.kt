package com.ilyne.helloszigetkmp.presentation.feature.login.component

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
import hello_sziget_kmp.shared.generated.resources.Res
import hello_sziget_kmp.shared.generated.resources.login_badge_artists
import hello_sziget_kmp.shared.generated.resources.login_badge_days
import hello_sziget_kmp.shared.generated.resources.login_badge_stages
import hello_sziget_kmp.shared.generated.resources.login_brand_name
import hello_sziget_kmp.shared.generated.resources.login_date_location
import hello_sziget_kmp.shared.generated.resources.login_slogan
import hello_sziget_kmp.shared.generated.resources.login_year
import org.jetbrains.compose.resources.stringResource

/**
 * Screen-specific overlay tones for the welcome hero. This screen intentionally renders a
 * fixed dark gradient regardless of the app's light/dark theme, so these colors live here
 * rather than in [com.ilyne.helloszigetkmp.presentation.theme.AppColorScheme] or
 * [com.ilyne.helloszigetkmp.presentation.theme.SzigetPalette] — they're exclusive to this screen.
 */
private object WelcomeColors {
    val GradientStart = Color(0xFFFF4958)
    val GradientMid = Color(0xFFB83DD8)
    val GradientEnd = Color(0xFF3B00DD)
    val AmbientCircleLarge = Color.White.copy(alpha = 0.06f)
    val AmbientCircleSmall = Color.White.copy(alpha = 0.04f)
    val AmbientGlow = Color(0xFFFFDC00).copy(alpha = 0.22f)
    val PillBackground = Color.White.copy(alpha = 0.18f)
    val PillBorder = Color.White.copy(alpha = 0.28f)
    val HeadlineShadow = Color.Black.copy(alpha = 0.25f)
    val YearLabelText = Color.White.copy(alpha = 0.82f)
    val SubtitleText = Color.White.copy(alpha = 0.55f)
    val BadgeBackground = Color.White.copy(alpha = 0.14f)
    val BadgeBorder = Color.White.copy(alpha = 0.2f)
    val TextPrimary = Color.White
}

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
                            WelcomeColors.GradientStart,
                            WelcomeColors.GradientMid,
                            WelcomeColors.GradientEnd,
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
                    .background(WelcomeColors.AmbientCircleLarge, shape = CircleShape),
            )

            // Middle-left background circle
            Box(
                modifier = Modifier
                    .offset(x = (-80).dp, y = 350.dp)
                    .size(200.dp)
                    .align(Alignment.TopStart)
                    .background(WelcomeColors.AmbientCircleSmall, shape = CircleShape),
            )

            // Lower-right ambient circle (yellow tone)
            Box(
                modifier = Modifier
                    .offset(x = 30.dp, y = 195.dp)
                    .size(86.dp)
                    .align(Alignment.CenterEnd)
                    .background(WelcomeColors.AmbientGlow, shape = CircleShape),
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
                            .background(WelcomeColors.PillBackground, shape = CircleShape)
                            .border(1.dp, WelcomeColors.PillBorder, shape = CircleShape)
                            .padding(horizontal = 18.dp, vertical = 5.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.login_date_location),
                            color = WelcomeColors.TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 2.sp,
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Big Headline Text with Soft drop-shadow
                    Text(
                        text = stringResource(Res.string.login_brand_name),
                        color = WelcomeColors.TextPrimary,
                        fontSize = 96.sp,
                        fontWeight = FontWeight.ExtraBold,
                        style = TextStyle(
                            shadow = Shadow(
                                color = WelcomeColors.HeadlineShadow,
                                offset = Offset(0f, 6f),
                                blurRadius = 40f,
                            ),
                        ),
                        lineHeight = 72.sp,
                        textAlign = TextAlign.Center,
                    )

                    // Year Label
                    Text(
                        text = stringResource(Res.string.login_year),
                        color = WelcomeColors.YearLabelText,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 8.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )

                    // Subtitle Slogan
                    Text(
                        text = stringResource(Res.string.login_slogan),
                        color = WelcomeColors.SubtitleText,
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
                        FestivalBadge(text = stringResource(Res.string.login_badge_artists))
                        FestivalBadge(text = stringResource(Res.string.login_badge_days))
                        FestivalBadge(text = stringResource(Res.string.login_badge_stages))
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
            .background(WelcomeColors.BadgeBackground, shape = CircleShape)
            .border(1.dp, WelcomeColors.BadgeBorder, shape = CircleShape)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            color = WelcomeColors.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
