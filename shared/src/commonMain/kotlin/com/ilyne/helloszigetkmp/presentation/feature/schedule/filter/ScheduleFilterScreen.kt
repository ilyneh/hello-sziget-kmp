package com.ilyne.helloszigetkmp.presentation.feature.schedule.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilyne.helloszigetkmp.presentation.component.SubHeader
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun ScheduleFilterScreen(
    initialFilter: ScheduleFilter,
    onSave: (ScheduleFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = koinViewModel<ScheduleFilterViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onIntent(FilterIntent.Initialize(initialFilter))
        viewModel.effects.collect { effect ->
            when (effect) {
                is FilterEffect.UpdateFilter -> onSave(effect.filter)
            }
        }
    }

    ScheduleFilterContent(
        uiState = uiState,
        toggleFavoritesOnly = { viewModel.onIntent(FilterIntent.ToggleFavoritesOnly(it)) },
        toggleFriendsGoing = { viewModel.onIntent(FilterIntent.ToggleFriendsGoing(it)) },
        toggleHideEmptyStages = { viewModel.onIntent(FilterIntent.ToggleHideEmptyStages(it)) },
        saveFilter = { viewModel.onIntent(FilterIntent.Save) },
        modifier = modifier
    )
}

@Composable
private fun ScheduleFilterContent(
    uiState: ScheduleFilterUiState,
    toggleFavoritesOnly: (Boolean) -> Unit,
    toggleFriendsGoing: (Boolean) -> Unit,
    toggleHideEmptyStages: (Boolean) -> Unit,
    saveFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        SubHeader(text = "Filters")

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Favorites Only",
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = uiState.showFavoritesOnly,
                onCheckedChange = { toggleFavoritesOnly(it) }
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Friends Going",
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = uiState.showFriendsGoing,
                onCheckedChange = { toggleFriendsGoing(it) }
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hide Stages with Empty Timeslots",
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = uiState.hideEmptyStages,
                onCheckedChange = { toggleHideEmptyStages(it) }
            )
        }

        Button(
            onClick = { saveFilter() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Save"
            )
        }
    }
}


@Preview
@Composable
fun ScheduleFilterContentPreview() {
    AppTheme {
        ScheduleFilterContent(
            uiState = ScheduleFilterUiState(),
            toggleFavoritesOnly = {},
            toggleFriendsGoing = {},
            toggleHideEmptyStages = {},
            saveFilter = {},
        )
    }
}
