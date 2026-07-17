package com.ilyne.helloszigetkmp.presentation.feature.schedule.filter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilyne.helloszigetkmp.domain.model.GenreGroup
import com.ilyne.helloszigetkmp.domain.model.PerformanceType
import com.ilyne.helloszigetkmp.presentation.component.actionbutton.ActionButton
import com.ilyne.helloszigetkmp.presentation.component.header.ModalHeader
import com.ilyne.helloszigetkmp.presentation.component.header.SubHeader2
import com.ilyne.helloszigetkmp.presentation.component.performancetype.GenreCheckboxUiState
import com.ilyne.helloszigetkmp.presentation.component.performancetype.PerformanceTypeSection
import com.ilyne.helloszigetkmp.presentation.component.sheet.AppModalBottomSheet
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ScheduleFilterScreen(
    initialFilter: ScheduleFilter,
    onSave: (ScheduleFilter) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<ScheduleFilterViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnSave by rememberUpdatedState(onSave)

    LaunchedEffect(Unit) {
        viewModel.onIntent(FilterIntent.Initialize(initialFilter))
        viewModel.effects.collect { effect ->
            when (effect) {
                is FilterEffect.UpdateFilter -> currentOnSave(effect.filter)
            }
        }
    }

    AppModalBottomSheet(
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        ScheduleFilterContent(
            uiState = uiState,
            toggleFavoritesOnly = { viewModel.onIntent(FilterIntent.ToggleFavoritesOnly(it)) },
            toggleFriendsGoing = { viewModel.onIntent(FilterIntent.ToggleFriendsGoing(it)) },
            toggleHideEmptyStages = { viewModel.onIntent(FilterIntent.ToggleHideEmptyStages(it)) },
            toggleShowExtraDays = { viewModel.onIntent(FilterIntent.ToggleShowExtraDays(it)) },
            togglePerformanceType = { type -> viewModel.onIntent(FilterIntent.TogglePerformanceType(type)) },
            toggleGenreGroup = { group, checked -> viewModel.onIntent(FilterIntent.ToggleGenreGroup(group, checked)) },
            toggleGenreDropdown = { type -> viewModel.onIntent(FilterIntent.ToggleGenreDropdown(type)) },
            saveFilter = { viewModel.onIntent(FilterIntent.Save) },
        )
    }
}

@Composable
private fun ScheduleFilterContent(
    uiState: ScheduleFilterUiState,
    toggleFavoritesOnly: (Boolean) -> Unit,
    toggleFriendsGoing: (Boolean) -> Unit,
    toggleHideEmptyStages: (Boolean) -> Unit,
    toggleShowExtraDays: (Boolean) -> Unit,
    togglePerformanceType: (PerformanceType) -> Unit,
    toggleGenreGroup: (GenreGroup, Boolean) -> Unit,
    toggleGenreDropdown: (PerformanceType) -> Unit,
    saveFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        ModalHeader(text = "Filters")

        FilterSwitchRow(
            text = "Favorites",
            checked = uiState.showFavoritesOnly,
            onCheckedChange = { toggleFavoritesOnly(it) },
        )

        FilterSwitchRow(
            text = "Friends Going",
            checked = uiState.showFriendsGoing,
            onCheckedChange = { toggleFriendsGoing(it) },
        )

        FilterSwitchRow(
            text = "Hide Stages with Empty Timeslots",
            checked = uiState.hideEmptyStages,
            onCheckedChange = { toggleHideEmptyStages(it) },
        )

        FilterSwitchRow(
            text = "Show Extra Days",
            checked = uiState.showExtraDays,
            onCheckedChange = { toggleShowExtraDays(it) },
        )

        SubHeader2(text = "Performance Types")

        uiState.performanceTypes.forEach { performanceType ->
            PerformanceTypeSection(
                type = performanceType.type,
                checkState = performanceType.checkState,
                isExpanded = performanceType.isExpanded,
                genres = performanceType.genres.map { genre -> GenreCheckboxUiState(genre.group, genre.isChecked) },
                onToggleType = { togglePerformanceType(performanceType.type) },
                onToggleDropdown = { toggleGenreDropdown(performanceType.type) },
                onToggleGenre = { group, checked -> toggleGenreGroup(group, checked) },
            )
        }

        ActionButton(
            onClick = { saveFilter() },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text(
                text = "Save",
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Preview
@Composable
private fun ScheduleFilterContentPreview() {
    AppTheme {
        ScheduleFilterContent(
            uiState = ScheduleFilterUiState(),
            toggleFavoritesOnly = {},
            toggleFriendsGoing = {},
            toggleHideEmptyStages = {},
            toggleShowExtraDays = {},
            togglePerformanceType = {},
            toggleGenreGroup = { _, _ -> },
            toggleGenreDropdown = {},
            saveFilter = {},
        )
    }
}

@Composable
private fun FilterSwitchRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
