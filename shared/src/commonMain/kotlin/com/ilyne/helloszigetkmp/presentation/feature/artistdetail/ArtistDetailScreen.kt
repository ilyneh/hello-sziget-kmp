package com.ilyne.helloszigetkmp.presentation.feature.artistdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilyne.helloszigetkmp.domain.model.Artist
import com.ilyne.helloszigetkmp.domain.model.SetTime
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
import com.ilyne.helloszigetkmp.util.datetime.formatDayAndTime
import com.ilyne.helloszigetkmp.util.text.htmlToAnnotatedString
import hello_sziget_kmp.shared.generated.resources.Res
import hello_sziget_kmp.shared.generated.resources.artist_detail_no_bio
import hello_sziget_kmp.shared.generated.resources.artist_detail_no_name
import hello_sziget_kmp.shared.generated.resources.artist_detail_not_found
import hello_sziget_kmp.shared.generated.resources.common_something_went_wrong
import org.jetbrains.compose.resources.stringResource
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
            onFavoriteToggle = viewModel::toggleFavorite,
        )
    }
}

@Composable
private fun ArtistDetailContent(
    uiState: ArtistDetailUiState,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().testTag("artistdetail_root")) {
        ModalHeader(
            text = uiState.artist?.name ?: stringResource(Res.string.artist_detail_no_name),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("artistdetail_header"),
        ) {
            if (uiState.artist != null) {
                FavoriteIconButton(
                    enabled = uiState.artist.isFavorited,
                    onClick = onFavoriteToggle,
                    modifier = Modifier.testTag("artistdetail_favorite_button"),
                )
            }
        }

        when (val status = uiState.status) {
            is ArtistDetailUiState.Status.Loading -> {
                LoadingBox(modifier = Modifier.fillMaxWidth().height(120.dp).testTag("artistdetail_loading"))
            }

            is ArtistDetailUiState.Status.Error -> {
                ErrorState(
                    message = status.message ?: stringResource(Res.string.common_something_went_wrong),
                    modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("artistdetail_error"),
                )
            }

            is ArtistDetailUiState.Status.Success -> {
                val artist = uiState.artist
                if (artist == null) {
                    Text(
                        text = stringResource(Res.string.artist_detail_not_found),
                        modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("artistdetail_not_found"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                    ) {
                        val chips = artist.toChips(uiState.nextSetTime)
                        if (chips.isNotEmpty()) {
                            GenreChips(
                                chips = chips,
                                modifier = Modifier.padding(bottom = 16.dp).testTag("artistdetail_genre_chips"),
                            )
                        }

                        if (uiState.friendsFavorited.isNotEmpty()) {
                            FriendAvatarRow(
                                friends = uiState.friendsFavorited.map { it.toAvatarData() },
                                modifier = Modifier.padding(bottom = 16.dp).testTag("artistdetail_friends_favorited"),
                            )
                        }

                        Text(
                            text = artist.bio
                                ?.htmlToAnnotatedString()
                                ?: AnnotatedString(stringResource(Res.string.artist_detail_no_bio)),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 24.dp).testTag("artistdetail_bio"),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreChips(
    chips: List<String>,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEach { chip ->
            TextPill(
                text = chip,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

private val includedTagPrefixes = listOf("genre-", "tag-")

private fun formatTag(rawTag: String): String {
    val stripped = when {
        rawTag.startsWith("genre-") -> rawTag.removePrefix("genre-")
        rawTag.startsWith("tag-") -> rawTag.removePrefix("tag-")
        else -> rawTag
    }
    return stripped.replaceFirstChar(Char::uppercaseChar)
}

private fun Artist.toChips(nextSetTime: SetTime?): List<String> =
    buildList {
        nextSetTime?.let { add(formatDayAndTime(it.startTime, it.endTime)) }
        tags
            ?.filter { tag -> includedTagPrefixes.any { tag.startsWith(it) } }
            ?.map(::formatTag)
            ?.let { addAll(it) }
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
                    tags = listOf(
                        "tag-music",
                        "genre-electronic",
                        "day-saturday",
                        "country-be",
                        "genre-hip-hop",
                        "country-hu",
                        "genre-rap",
                    ),
                ),
                friendsFavorited = listOf(
                    User(id = "1", name = "Zack", imageUrl = null),
                    User(id = "2", name = "owen", imageUrl = null),
                    User(id = "3", name = "Zaira", imageUrl = null),
                ),
                nextSetTime = SetTime(
                    id = "st-1",
                    artistId = "1",
                    stageId = null,
                    startTime = 1_723_708_800_000L,
                    endTime = 1_723_712_400_000L,
                    hideEndTime = false,
                ),
                status = ArtistDetailUiState.Status.Success,
            ),
            onFavoriteToggle = {},
        )
    }
}
