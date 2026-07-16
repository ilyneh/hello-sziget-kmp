package com.ilyne.helloszigetkmp.presentation.feature.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ilyne.helloszigetkmp.domain.model.Artist
import com.ilyne.helloszigetkmp.presentation.component.FavoriteIconButton
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme


@Composable
fun ArtistCard(
    artist: Artist,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit = {},
) {
    val coral = AppTheme.colors.coral
    val gradientEnd = MaterialTheme.colorScheme.primary
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = coral)
                .padding(6.dp)
        ) {
            AsyncImage(
                model = artist.imageUrl,
                contentDescription = "${artist.name} image",
                contentScale = ContentScale.Crop,
                alignment = Alignment.BottomCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(size = 8.dp))
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, gradientEnd),
                                startY = size.height * 0.5f, // Adjust where the shadow begins fading in
                                endY = size.height        // Ends perfectly at the bottom edge
                            )
                        )
                    }
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = artist.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = coral,
                    maxLines = 2,
                    lineHeight = 14.sp,
                    overflow = TextOverflow.Ellipsis,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 10.sp,        // Minimum allowable size
                        maxFontSize = 14.sp,        // Maximum allowable size
                        stepSize = 0.5.sp             // Granularity of adjustment
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            FavoriteIconButton(
                enabled = artist.isFavorited,
                onClick = onFavoriteToggle,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}
