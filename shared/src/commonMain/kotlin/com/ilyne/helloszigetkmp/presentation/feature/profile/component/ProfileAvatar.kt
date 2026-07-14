package com.ilyne.helloszigetkmp.presentation.feature.profile.component

import androidx.compose.foundation.Image
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
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
            // Hold onto a single ImageRequest instance per URL, so Coil's internal
            // painter/state machine isn't restarted by a fresh-but-equal request object
            // on every recomposition (see history for why this alone wasn't sufficient).
            val request = remember(imageUrl, platformContext) {
                ImageRequest.Builder(platformContext)
                    .data(imageUrl)
                    .build()
            }
            // Regardless of *why* the painter's state cycles away from Success (spurious
            // recomposition, a real cache re-validation, platform-specific dispatch
            // timing -- or the URL itself changing slightly between refreshes, e.g. a
            // rotating signed/cache-busted URL for what is otherwise the same photo),
            // never let the box go blank for a friend whose photo we've already
            // successfully rendered once: keep drawing the last successfully-loaded
            // painter until a *new* one succeeds. Deliberately NOT keyed on `imageUrl` --
            // this needs to survive the URL itself changing across refreshes, since that's
            // exactly the case that was defeating it; it only needs to reset when this
            // avatar slot starts representing a different person, which the caller already
            // guarantees via key(friend.id) around this composable.
            var lastSuccessPainter by remember { mutableStateOf<Painter?>(null) }
            val painter = rememberAsyncImagePainter(
                model = request,
                onSuccess = { lastSuccessPainter = it.painter },
            )
            val state by painter.state.collectAsState()
            val displayPainter = if (state is AsyncImagePainter.State.Success) painter else lastSuccessPainter ?: painter
            Image(
                painter = displayPainter,
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
