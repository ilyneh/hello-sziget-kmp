package com.ilyne.helloszigetkmp.presentation.feature.artistdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilyne.helloszigetkmp.domain.model.Artist
import com.ilyne.helloszigetkmp.domain.model.User
import com.ilyne.helloszigetkmp.presentation.component.FavoriteIconButton
import com.ilyne.helloszigetkmp.presentation.component.friendavatarstack.FriendAvatarRow
import com.ilyne.helloszigetkmp.presentation.component.friendavatarstack.FriendAvatarStackData
import com.ilyne.helloszigetkmp.presentation.component.header.ModalHeader
import com.ilyne.helloszigetkmp.presentation.component.pill.TextPill
import com.ilyne.helloszigetkmp.presentation.component.sheet.AppModalBottomSheet
import com.ilyne.helloszigetkmp.presentation.component.status.ErrorState
import com.ilyne.helloszigetkmp.presentation.component.status.LoadingBox
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import com.ilyne.helloszigetkmp.util.text.htmlToAnnotatedString
import hello_sziget_kmp.shared.generated.resources.Res
import hello_sziget_kmp.shared.generated.resources.ic_cancel
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ArtistDetailScreen(
    artistId: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<ArtistDetailViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(artistId) {
        viewModel.load(artistId)
    }

    AppModalBottomSheet(
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        ArtistDetailContent(
            uiState = uiState,
            onDismiss = onDismiss,
            onFavoriteToggle = viewModel::toggleFavorite,
        )
    }
}

@Composable
private fun ArtistDetailContent(
    uiState: ArtistDetailUiState,
    onDismiss: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ModalHeader(
            text = uiState.artist?.name ?: "Artist",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (uiState.artist != null) {
                    FavoriteIconButton(
                        enabled = uiState.artist.isFavorited,
                        onClick = onFavoriteToggle,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_cancel),
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }

        when (val status = uiState.status) {
            is ArtistDetailUiState.Status.Loading -> {
                LoadingBox(modifier = Modifier.fillMaxWidth().height(120.dp))
            }

            is ArtistDetailUiState.Status.Error -> {
                ErrorState(
                    message = status.message ?: "Something went wrong",
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            }

            is ArtistDetailUiState.Status.Success -> {
                val artist = uiState.artist
                if (artist == null) {
                    Text(
                        text = "Artist not found",
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                    ) {
                        if (!artist.tags.isNullOrEmpty()) {
                            GenreChips(
                                genres = artist.tags,
                                modifier = Modifier.padding(bottom = 16.dp),
                            )
                        }

                        if (uiState.friendsFavorited.isNotEmpty()) {
                            FriendAvatarRow(
                                friends = uiState.friendsFavorited.map { it.toAvatarData() },
                                modifier = Modifier.padding(bottom = 16.dp),
                            )
                        }

                        Text(
                            text = artist.bio
                                ?.htmlToAnnotatedString()
                                ?: AnnotatedString("No bio available yet."),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 24.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreChips(
    genres: List<String>,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        genres.forEach { genre ->
            TextPill(
                text = genre,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

private fun User.toAvatarData() = FriendAvatarStackData(id = id, name = name, imageUrl = imageUrl)

@Preview
@Composable
private fun ArtistDetailContentPreview() {
    AppTheme {
        ArtistDetailContent(
            uiState = ArtistDetailUiState(
                artist = Artist(
                    id = "1",
                    name = "Skrillex",
                    bio = "<p>An artist known for <b>headlining sets</b> and genre-bending production.</p>" +
                        "<ul><li>Multiple festival closes</li><li>Genre-bending style</li></ul>",
                    imageUrl = null,
                    isFavorited = true,
                    tags = listOf("Electronic", "Dubstep", "Bass"),
                ),
                friendsFavorited = listOf(
                    User(id = "1", name = "Zack", imageUrl = null),
                    User(id = "2", name = "owen", imageUrl = null),
                    User(id = "3", name = "Zaira", imageUrl = null),
                ),
                status = ArtistDetailUiState.Status.Success,
            ),
            onDismiss = {},
            onFavoriteToggle = {},
        )
    }
}
