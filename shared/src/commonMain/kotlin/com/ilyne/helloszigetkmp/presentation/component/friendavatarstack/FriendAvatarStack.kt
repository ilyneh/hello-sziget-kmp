package com.ilyne.helloszigetkmp.presentation.component.friendavatarstack

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilyne.helloszigetkmp.presentation.component.avatar.InitialsAvatar
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
               text = "+${friends.size - maxNumAvatars}",
               textColor = MaterialTheme.colorScheme.onSurfaceVariant,
               avatarColor = MaterialTheme.colorScheme.surfaceVariant,
           )
        }
    }
}

/**
 * Same circular avatar styling as [FriendAvatarStack], but laid out as a plain, non-overlapping
 * row (positive spacing instead of [FriendAvatarStack]'s negative "stacked" spacing) - for call
 * sites that want each avatar fully visible rather than a compact overlapping cluster.
 */
@Composable
fun FriendAvatarRow(
    friends: List<FriendAvatarStackData>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        friends.forEach { friend ->
            key(friend.id) {
                FriendAvatar(friend = friend, avatarColor = avatarColorFor(friend))
            }
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
        text = friend.name.firstOrNull()?.uppercase() ?: "-",
        imageUrl = friend.imageUrl,
        avatarColor = avatarColor,
        modifier = modifier,
    )
}

@Composable
private fun Avatar(
    text: String,
    imageUrl: String? = null,
    textColor: Color = MaterialTheme.colorScheme.onPrimary,
    avatarColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
) {
    InitialsAvatar(
        imageUrl = imageUrl,
        shape = CircleShape,
        modifier = modifier.size(28.dp),
        backgroundColor = avatarColor,
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
        )
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

@Preview
@Composable
private fun PreviewFriendAvatarRow() {
    AppTheme {
        FriendAvatarRow(
            friends = listOf(
                FriendAvatarStackData(id = "1", name = "Zack"),
                FriendAvatarStackData(id = "2", name = "owen"),
                FriendAvatarStackData(id = "3", name = "Zaira")
            )
        )
    }
}
