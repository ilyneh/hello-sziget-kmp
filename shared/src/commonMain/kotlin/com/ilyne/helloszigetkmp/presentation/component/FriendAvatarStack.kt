package com.ilyne.helloszigetkmp.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme


data class FriendAvatarStackData(
    val name: String,
    val imageUrl: String? = null
)

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
            FriendAvatar(friend = friend)
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

@Composable
private fun FriendAvatar(
    friend: FriendAvatarStackData,
    avatarColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
) {
    Avatar(
        text = friend.name.firstOrNull()?.uppercase() ?: "-",
        avatarColor = avatarColor,
        modifier = modifier,
    )
}

@Composable
private fun Avatar(
    text: String,
    textColor: Color = MaterialTheme.colorScheme.onPrimary,
    avatarColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(28.dp)
            .background(color = avatarColor, shape = CircleShape)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.surface, shape = CircleShape),
        contentAlignment = Alignment.Center,
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
                FriendAvatarStackData(name = "Zack"),
                FriendAvatarStackData(name = "owen"),
                FriendAvatarStackData(name = "Zaira")
            )
        )
    }
}
