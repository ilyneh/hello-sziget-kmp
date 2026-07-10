package com.ilyne.helloszigetkmp.presentation.feature.lineup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilyne.helloszigetkmp.presentation.component.HeartIcon
import com.ilyne.helloszigetkmp.presentation.component.header.MainHeader
import com.ilyne.helloszigetkmp.presentation.feature.schedule.component.list.SetTimeListHeader
import com.ilyne.helloszigetkmp.presentation.theme.SzigetPalette
import com.ilyne.helloszigetkmp.util.datetime.formatTime
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MyLineupScreen(modifier: Modifier = Modifier, onArtistClick: (String) -> Unit = {}) {
    val viewModel = koinViewModel<MyLineupViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        MainHeader(text = "My Lineup")

        when {
            uiState.status == MyLineupUiState.Status.LOADING -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            uiState.favoritesGroupedByDay.isEmpty() -> EmptyView(modifier = Modifier.fillMaxSize())

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = MaterialTheme.colorScheme.surface),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val itemShape = RoundedCornerShape(size = 8.dp)

                    uiState.favoritesGroupedByDay.forEach { (day, setTimes) ->
                        stickyHeader {
                            Box(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                SetTimeListHeader(text = day,)
                            }
                        }

                        items(setTimes, key = { it.id }) { setTime ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                // 1. The Shadow Background Layer
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .offset(x = 6.dp, y = 6.dp) // The 3D offset effect
                                        .background(color = SzigetPalette.RedOrange, shape = itemShape)
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(color = SzigetPalette.WarmOrange, shape = itemShape)
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
                                            color = Color.White
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            SubtitleText(
                                                text = setTime.stageName ?: "TBA",
                                                modifier = Modifier
                                                    .weight(weight = 2f, fill = false)
                                            )

                                            SubtitleText(
                                                text = "·",
                                                modifier = Modifier
                                                    .padding(horizontal = 4.dp)
                                                    .weight(weight = 1f, fill = false)
                                            )

                                            SubtitleText(
                                                text = "${formatTime(setTime.startTime)} - ${formatTime(setTime.endTime)}",
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }

                                    IconButton(onClick = { viewModel.removeFavorite(setTime.artistId) }) {
                                        HeartIcon(
                                            enabled = true,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
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
        color = Color.White,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 1.2.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
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
