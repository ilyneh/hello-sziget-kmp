package com.ilyne.helloszigetkmp.presentation.schedule.components.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilyne.helloszigetkmp.domain.model.Artist
import com.ilyne.helloszigetkmp.presentation.component.friendavatarstack.FriendAvatarStack
import com.ilyne.helloszigetkmp.presentation.component.friendavatarstack.FriendAvatarStackData
import com.ilyne.helloszigetkmp.presentation.feature.schedule.ScheduleUiState
import com.ilyne.helloszigetkmp.presentation.feature.schedule.component.color.stageColor
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import com.ilyne.helloszigetkmp.util.datetime.formatTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours


@Composable
fun SetTimeListItem(
    setTime: ScheduleUiState.SetTime,
    onToggleFavorite: (artistId: String?, current: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {

    val stageColor = stageColor(stageId = setTime.stageId)
    Row(
        modifier = modifier
            .background(color = MaterialTheme.colorScheme.surface)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color = stageColor, shape = CircleShape),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {

            Text(
                text = setTime.artist?.name.orEmpty(),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 1.2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                SubtitleText(
                    text = setTime.stage?.name ?: "TBA",
                    modifier = Modifier
                        .weight(weight = 2f, fill = false)
                )

                SubtitleText(
                    text = "·",
                    modifier = Modifier
                        .padding(start = 4.dp, end = 4.dp)
                        .weight(weight = 1f, fill = false)
                )

                SubtitleText(
                    text = "${formatTime(setTime.startTime)} - ${formatTime(setTime.endTime)}",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        val friendsFavorited = setTime.artistFriendsFavorited?.friendsFavorited
        if (friendsFavorited != null) {
            FriendAvatarStack(
                friends = friendsFavorited.map { friend ->
                    FriendAvatarStackData(
                        name = friend.name
                    )
                },
            )
        }

        IconButton(
            onClick = {
                onToggleFavorite(
                    setTime.artistId,
                    setTime.artist?.isFavorited ?: false
                )
            },
        ) {
            Text(if (setTime.artist?.isFavorited ?: false) "♥" else "♡", fontSize = 20.sp)
        }
    }
}

@Composable
private fun SubtitleText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 1.2.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}


@Composable
fun SetTimeListHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        fontWeight = FontWeight.SemiBold,
        lineHeight = 1.2.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Preview
@Composable
fun SetTimeListItemPreview() {
    AppTheme {
        SetTimeListItem(
            setTime = ScheduleUiState.SetTime(
                id = "1",
                startTime = Clock.System.now().toEpochMilliseconds(),
                endTime = Clock.System.now().plus(duration = 1.hours).toEpochMilliseconds(),
                hideEndTime = false,
                isInThePast = false,
                startHourFraction = 0.0,
                endHourFraction = 0.0,
                artist = Artist(
                    id = "2",
                    name = "Skrillex Skrillex Skrillex Skrillex Skrillex",
                    bio = null,
                    isFavorited = true,
                    tags = null
                )
            ),
            onToggleFavorite = { _, _ -> }
        )
    }
}

@Preview
@Composable
fun SetTimeListHeaderPreview() {
    AppTheme {
        SetTimeListHeader(text = "17:30")
    }
}
