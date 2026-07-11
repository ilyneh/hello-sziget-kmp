package com.ilyne.helloszigetkmp.presentation.feature.schedule.component.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilyne.helloszigetkmp.domain.model.User
import com.ilyne.helloszigetkmp.presentation.component.friendavatarstack.FriendAvatarStack
import com.ilyne.helloszigetkmp.presentation.component.friendavatarstack.FriendAvatarStackData
import com.ilyne.helloszigetkmp.presentation.feature.schedule.ScheduleUiState
import com.ilyne.helloszigetkmp.presentation.feature.schedule.component.color.artistColor
import com.ilyne.helloszigetkmp.util.datetime.formatTime


enum class SetTimeCardViewMode {
    TIMELINE, SWIMLANE
}

@Composable
fun SetTimeCard(
    setTime: ScheduleUiState.SetTime,
    viewMode: SetTimeCardViewMode = SetTimeCardViewMode.TIMELINE,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val color = setTime.artist?.let(::artistColor) ?: Color.Gray
    val backgroundAlpha = if (setTime.isInThePast) 0.55f else 1f
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clipToBounds()
            .background(color.copy(alpha = backgroundAlpha))
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {

        val friendsFavorited = setTime.artistFriendsFavorited?.friendsFavorited
        val hasFriendsFavorited = friendsFavorited?.isNotEmpty() == true

        Column {
            Text(
                text = setTime.artist?.name ?: "Unknown",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color.White,
                lineHeight = 18.sp,
            )
            Text(
                text = formatTime(setTime.startTime),
                fontSize = 11.sp,
                color = Color.White,
            )

            if (hasFriendsFavorited && viewMode == SetTimeCardViewMode.TIMELINE) {
                Spacer(modifier = Modifier.height(4.dp))
                FriendsAvatar(friends = friendsFavorited)
            }
        }

        if (hasFriendsFavorited && viewMode == SetTimeCardViewMode.SWIMLANE) {
            FriendsAvatar(
                friends = friendsFavorited,
                modifier = Modifier
                    .align(Alignment.BottomEnd),
            )
        }

    }
}

@Composable
private fun FriendsAvatar(
    friends: List<User>,
    modifier: Modifier = Modifier
) {
    FriendAvatarStack(
        friends = friends.map { friend ->
            FriendAvatarStackData(
                name = friend.name,
                imageUrl = friend.imageUrl,
            )
        },
        maxNumAvatars = 3,
        modifier = modifier
    )
}
