package com.ilyne.helloszigetkmp.presentation.feature.profile.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest


@Composable
fun ProfileAvatar(
    avatarText: String,
    imageUrl: String? = null,
    elevated: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val modifier = if (elevated) modifier.shadow(elevation = 8.dp, shape = RoundedCornerShape(percent = 32)) else modifier
    Box(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(percent = 32))
            .background(MaterialTheme.colorScheme.primary)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
    ) {
        if (imageUrl != null) {
            val platformContext = LocalPlatformContext.current
            // Hold onto a single ImageRequest instance per URL. Passing a raw String
            // `model` makes AsyncImage build a brand new ImageRequest every recomposition;
            // Coil keys its internal painter/state machine off that request instance, so a
            // fresh-but-equal request can still restart the state machine -- briefly
            // clearing the already-cached bitmap -- even though nothing about the image
            // actually changed. That restart is what caused the avatar to flash blank on
            // pull-to-refresh.
            val request = remember(imageUrl, platformContext) {
                ImageRequest.Builder(platformContext)
                    .data(imageUrl)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(4.dp),
                text = avatarText,
                color = Color.White,
                maxLines = 1,
                textAlign = TextAlign.Center,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 16.sp,        // Minimum allowable size
                    maxFontSize = 80.sp,        // Maximum allowable size
                    stepSize = 1.sp             // Granularity of adjustment
                ),
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
