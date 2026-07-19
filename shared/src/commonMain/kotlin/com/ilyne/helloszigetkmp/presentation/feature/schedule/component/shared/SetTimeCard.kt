package com.ilyne.helloszigetkmp.presentation.feature.schedule.component.shared

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilyne.helloszigetkmp.domain.model.User
import com.ilyne.helloszigetkmp.presentation.component.HeartIcon
import com.ilyne.helloszigetkmp.presentation.component.friendavatarstack.FriendAvatarStack
import com.ilyne.helloszigetkmp.presentation.component.friendavatarstack.FriendAvatarStackData
import com.ilyne.helloszigetkmp.presentation.feature.schedule.ScheduleUiState
import com.ilyne.helloszigetkmp.presentation.feature.schedule.component.color.artistColor
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import com.ilyne.helloszigetkmp.util.datetime.formatTime
import hello_sziget_kmp.shared.generated.resources.Res
import hello_sziget_kmp.shared.generated.resources.common_unknown
import org.jetbrains.compose.resources.stringResource

enum class SetTimeCardViewMode {
    TIMELINE,
    SWIMLANE,
}

private val FAVORITE_HEART_SIZE = 16.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SetTimeCard(
    setTime: ScheduleUiState.SetTime,
    modifier: Modifier = Modifier,
    viewMode: SetTimeCardViewMode = SetTimeCardViewMode.TIMELINE,
    onClick: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
) {
    val color = setTime.artist?.let(::artistColor) ?: AppTheme.colors.fallbackGray
    val backgroundAlpha = if (setTime.isInThePast) 0.55f else 1f
    val isFavorited = setTime.artist?.isFavorited == true
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clipToBounds()
            .background(color.copy(alpha = backgroundAlpha))
            .combinedClickable(onClick = onClick, onLongClick = onToggleFavorite)
            .padding(8.dp),
    ) {
        val friendsFavorited = setTime.artistFriendsFavorited?.friendsFavorited
        val hasFriendsFavorited = friendsFavorited?.isNotEmpty() == true

        Column {
            Text(
                text = setTime.artist?.name ?: stringResource(Res.string.common_unknown),
                modifier = Modifier.padding(end = if (isFavorited) FAVORITE_HEART_SIZE + 4.dp else 0.dp),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = AppTheme.colors.onAccent,
                lineHeight = 18.sp,
            )
            Text(
                text = formatTime(setTime.startTime),
                fontSize = 11.sp,
                color = AppTheme.colors.onAccent,
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

        if (isFavorited) {
            HeartIcon(
                enabled = true,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(FAVORITE_HEART_SIZE),
            )
        }
    }
}

@Composable
private fun FriendsAvatar(
    friends: List<User>,
    modifier: Modifier = Modifier,
) {
    FriendAvatarStack(
        friends = friends.map { friend ->
            FriendAvatarStackData(
                id = friend.id,
                name = friend.name,
                imageUrl = friend.imageUrl,
            )
        },
        maxNumAvatars = 3,
        modifier = modifier,
    )
}
