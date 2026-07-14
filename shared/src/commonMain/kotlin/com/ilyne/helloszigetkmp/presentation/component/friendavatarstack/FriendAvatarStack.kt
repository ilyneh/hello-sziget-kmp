package com.ilyne.helloszigetkmp.presentation.component.friendavatarstack

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import com.ilyne.helloszigetkmp.presentation.theme.SzigetPalette


data class FriendAvatarStackData(
    val id: String,
    val name: String,
    val imageUrl: String? = null
)

// Lightened by ~15% (blended toward white) so brand swatches stay legible as small filled
// circles behind avatar initials, rather than reading as fully-saturated blocks of color.
private fun Color.lightened(factor: Float): Color = Color(
    red = red + (1f - red) * factor,
    green = green + (1f - green) * factor,
    blue = blue + (1f - blue) * factor,
    alpha = alpha,
)

private val avatarPalette = listOf(
    SzigetPalette.PrimaryBlue,
    SzigetPalette.Magenta,
    SzigetPalette.Red,
    SzigetPalette.HotPink,
    SzigetPalette.WarmOrange,
    SzigetPalette.TealGreen,
    SzigetPalette.DarkTeal,
).map { it.lightened(0.15f) }

// Deterministic hash-into-palette, mirroring StageColors.stageColor's convention for picking
// a stable color from a palette based on an identifier.
private fun avatarColorFor(friend: FriendAvatarStackData): Color =
    avatarPalette[friend.name.hashCode().mod(avatarPalette.size)]

@Composable
fun FriendAvatarStack(
    friends: List<FriendAvatarStackData>,
    maxNumAvatars: Int = 2,
    modifier: Modifier = Modifier
) {

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy((-6).dp)
    ) {
        friends.take(maxNumAvatars).forEach { friend ->
            key(friend.id) {
                FriendAvatar(friend = friend, avatarColor = avatarColorFor(friend))
            }
        }

        if (friends.size > maxNumAvatars) {
           Avatar(
               id = "overflow",
               text = "+${friends.size - maxNumAvatars}",
               textColor = MaterialTheme.colorScheme.onSurfaceVariant,
               avatarColor = MaterialTheme.colorScheme.surfaceVariant,
           )
        }
    }
}

@Composable
private fun FriendAvatar(
    friend: FriendAvatarStackData,
    avatarColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
) {
    Avatar(
        id = friend.id,
        text = friend.name.firstOrNull()?.uppercase() ?: "-",
        imageUrl = friend.imageUrl,
        avatarColor = avatarColor,
        modifier = modifier,
    )
}

@Composable
private fun Avatar(
    id: String,
    text: String,
    imageUrl: String? = null,
    textColor: Color = MaterialTheme.colorScheme.onPrimary,
    avatarColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(28.dp)
            .background(color = avatarColor, shape = CircleShape)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl != null) {
            val platformContext = LocalPlatformContext.current
            // Hold onto a single ImageRequest instance per URL, so Coil's internal
            // painter/state machine isn't restarted by a fresh-but-equal request object
            // on every recomposition.
            val request = remember(imageUrl, platformContext) {
                ImageRequest.Builder(platformContext)
                    .data(imageUrl)
                    .build()
            }
            // Keep showing the last successfully-loaded painter whenever the live state
            // isn't Success (spurious recomposition, cache re-validation, or the URL
            // itself changing between refreshes for the same person), instead of letting
            // the avatar blank out. Deliberately NOT keyed on `imageUrl` -- it needs to
            // survive the URL changing across refreshes; it only resets when this slot
            // starts representing a different person, which the caller guarantees via
            // key(id) around this composable.
            var lastSuccessPainter by remember(id) { mutableStateOf<Painter?>(null) }
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
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
        } else {
            Text(
                text = text,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
            )
        }
    }
}

@Preview
@Composable
private fun PreviewFriendAvatarStack() {
    AppTheme {
        FriendAvatarStack(
            friends = listOf(
                FriendAvatarStackData(id = "1", name = "Zack"),
                FriendAvatarStackData(id = "2", name = "owen"),
                FriendAvatarStackData(id = "3", name = "Zaira")
            )
        )
    }
}
