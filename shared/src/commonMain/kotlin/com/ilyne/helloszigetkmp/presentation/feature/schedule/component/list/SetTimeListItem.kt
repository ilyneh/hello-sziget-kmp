package com.ilyne.helloszigetkmp.presentation.feature.schedule.component.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import com.ilyne.helloszigetkmp.presentation.component.HeartIcon
import com.ilyne.helloszigetkmp.presentation.component.friendavatarstack.FriendAvatarStack
import com.ilyne.helloszigetkmp.presentation.component.friendavatarstack.FriendAvatarStackData
import com.ilyne.helloszigetkmp.presentation.component.pill.TextPill
import com.ilyne.helloszigetkmp.presentation.component.pill.TextPillDefaults
import com.ilyne.helloszigetkmp.presentation.feature.schedule.ScheduleUiState
import com.ilyne.helloszigetkmp.presentation.feature.schedule.component.color.stageColor
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import com.ilyne.helloszigetkmp.presentation.theme.SzigetPalette
import com.ilyne.helloszigetkmp.util.datetime.formatTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours


@Composable
fun SetTimeListItem(
    setTime: ScheduleUiState.SetTime,
    onToggleFavorite: (artistId: String?, current: Boolean) -> Unit,
    onArtistClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {

    val stageColor = stageColor(stageId = setTime.stageId)
    Row(
        modifier = modifier
            .background(color = SzigetPalette.LightBlue)
            .clickable(enabled = setTime.artistId != null) {
                setTime.artistId?.let(onArtistClick)
            }
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
                color = MaterialTheme.colorScheme.primary,
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
                        name = friend.name,
                        imageUrl = friend.imageUrl,
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
            HeartIcon(
                enabled = setTime.artist?.isFavorited ?: false,
                modifier = Modifier.padding(8.dp)
            )
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
        color = MaterialTheme.colorScheme.primary,
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
    TextPill(
        text = text,
        modifier = modifier,
        style = TextPillDefaults.style.copy(
            fontSize = 16.sp,
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
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
                    imageUrl = null,
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
