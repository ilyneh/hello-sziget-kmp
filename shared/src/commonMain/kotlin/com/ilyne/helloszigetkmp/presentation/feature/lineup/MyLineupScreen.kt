package com.ilyne.helloszigetkmp.presentation.feature.lineup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilyne.helloszigetkmp.presentation.component.FavoriteIconButton
import com.ilyne.helloszigetkmp.presentation.component.SubtitleText
import com.ilyne.helloszigetkmp.presentation.component.header.MainHeader
import com.ilyne.helloszigetkmp.presentation.component.pulltorefresh.PullToRefreshContent
import com.ilyne.helloszigetkmp.presentation.component.status.ErrorState
import com.ilyne.helloszigetkmp.presentation.component.status.LoadingBox
import com.ilyne.helloszigetkmp.presentation.feature.LocalBottomBarPadding
import com.ilyne.helloszigetkmp.presentation.feature.contentBottomInset
import com.ilyne.helloszigetkmp.presentation.feature.schedule.component.list.SetTimeListHeader
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import com.ilyne.helloszigetkmp.util.datetime.formatTime
import org.koin.compose.viewmodel.koinViewModel
import kotlin.collections.component1
import kotlin.collections.component2

@Composable
fun MyLineupScreen(modifier: Modifier = Modifier, onArtistClick: (String) -> Unit = {}) {
    val viewModel = koinViewModel<MyLineupViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PullToRefreshContent(
        isRefreshing = uiState.status == MyLineupUiState.Status.Loading,
        onRefresh = { viewModel.refresh() },
        modifier = modifier
    ) {
        MyLineupScreenContent(
            uiState = uiState,
            viewModel = viewModel,
            onArtistClick = onArtistClick,
            modifier = modifier
        )
    }
}


@Composable
private fun MyLineupScreenContent(
    uiState: MyLineupUiState,
    viewModel: MyLineupViewModel,
    onArtistClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        MainHeader(text = "My Lineup")

        when (val status = uiState.status) {
            MyLineupUiState.Status.Loading -> LoadingBox()

            is MyLineupUiState.Status.Error -> ErrorState(
                message = status.message ?: "Something went wrong",
                onRetry = { viewModel.refresh() },
            )

            else -> {
                if (uiState.favoritesGroupedByDay.isEmpty()) {
                    EmptyView(modifier = Modifier.fillMaxSize())
                } else {
                    MyLineupScreenList(
                        uiState = uiState,
                        onArtistClick = onArtistClick,
                        onRemoveFavorite = { viewModel.removeFavorite(artistId = it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MyLineupScreenList(
    uiState: MyLineupUiState,
    onArtistClick: (String) -> Unit,
    onRemoveFavorite: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = LocalBottomBarPadding.contentBottomInset,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val itemShape = RoundedCornerShape(size = 8.dp)

        uiState.favoritesGroupedByDay.forEach { (day, setTimes) ->
            stickyHeader {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    SetTimeListHeader(text = day)
                }
            }

            items(setTimes, key = { it.id }) { setTime ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    // 1. The Shadow Background Layer
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(x = 6.dp, y = 6.dp) // The 3D offset effect
                            .background(color = AppTheme.colors.redOrange, shape = itemShape)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = AppTheme.colors.warmOrange, shape = itemShape)
                            .clickable { onArtistClick(setTime.artistId) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = setTime.artistName,
                                fontWeight = FontWeight.ExtraBold,
                                color = AppTheme.colors.onAccent
                            )

                            StageSetTimeRow(
                                stageName = setTime.stageName ?: "TBA",
                                startTime = formatTime(setTime.startTime),
                                endTime = formatTime(setTime.endTime),
                            )
                        }

                        FavoriteIconButton(
                            enabled = true,
                            onClick = { onRemoveFavorite(setTime.artistId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StageSetTimeRow(
    stageName: String,
    startTime: String,
    endTime: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth()
    ) {
        SubtitleText(
            text = stageName,
            color = AppTheme.colors.onAccent,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .weight(weight = 2f, fill = false)
        )

        SubtitleText(
            text = "·",
            color = AppTheme.colors.onAccent,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .weight(weight = 1f, fill = false)
        )

        SubtitleText(
            text = "$startTime - $endTime",
            color = AppTheme.colors.onAccent,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.wrapContentWidth()
        )
    }
}

@Composable
private fun EmptyView(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("♡", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("No favorites yet", style = MaterialTheme.typography.titleMedium)
            Text(
                "Tap the heart on any artist to add them",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
