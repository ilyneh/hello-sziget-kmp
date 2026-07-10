package com.ilyne.helloszigetkmp.presentation.feature.schedule.filter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilyne.helloszigetkmp.domain.model.GenreGroup
import com.ilyne.helloszigetkmp.domain.model.PerformanceType
import com.ilyne.helloszigetkmp.domain.model.displayName
import com.ilyne.helloszigetkmp.presentation.component.actionbutton.ActionButton
import com.ilyne.helloszigetkmp.presentation.component.header.SubHeader
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import hello_sziget_kmp.shared.generated.resources.Res
import hello_sziget_kmp.shared.generated.resources.ic_carat_right
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleFilterScreen(
    initialFilter: ScheduleFilter,
    onSave: (ScheduleFilter) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = koinViewModel<ScheduleFilterViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.onIntent(FilterIntent.Initialize(initialFilter))
        viewModel.effects.collect { effect ->
            when (effect) {
                is FilterEffect.UpdateFilter -> onSave(effect.filter)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        ScheduleFilterContent(
            uiState = uiState,
            toggleFavoritesOnly = { viewModel.onIntent(FilterIntent.ToggleFavoritesOnly(it)) },
            toggleFriendsGoing = { viewModel.onIntent(FilterIntent.ToggleFriendsGoing(it)) },
            toggleHideEmptyStages = { viewModel.onIntent(FilterIntent.ToggleHideEmptyStages(it)) },
            toggleShowExtraDays = { viewModel.onIntent(FilterIntent.ToggleShowExtraDays(it)) },
            togglePerformanceType = { type, checked -> viewModel.onIntent(FilterIntent.TogglePerformanceType(type, checked)) },
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
    togglePerformanceType: (PerformanceType, Boolean) -> Unit,
    toggleGenreGroup: (GenreGroup, Boolean) -> Unit,
    toggleGenreDropdown: (PerformanceType) -> Unit,
    saveFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        SubHeader(text = "Filters")

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Favorites",
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

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Show Extra Days",
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = uiState.showExtraDays,
                onCheckedChange = { toggleShowExtraDays(it) }
            )
        }

        SubHeader(text = "Performance Types")

        uiState.performanceTypes.forEach { performanceType ->
            PerformanceTypeSection(
                uiState = performanceType,
                onToggleType = { checked -> togglePerformanceType(performanceType.type, checked) },
                onToggleDropdown = { toggleGenreDropdown(performanceType.type) },
                onToggleGenre = { group, checked -> toggleGenreGroup(group, checked) },
            )
        }

        ActionButton(
            onClick = { saveFilter() },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text(
                text = "Save",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PerformanceTypeSection(
    uiState: PerformanceTypeUiState,
    onToggleType: (Boolean) -> Unit,
    onToggleDropdown: () -> Unit,
    onToggleGenre: (GenreGroup, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = uiState.isChecked,
                onCheckedChange = onToggleType,
            )
            Text(
                text = uiState.type.displayName(),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onToggleDropdown) {
                Icon(
                    painter = painterResource(Res.drawable.ic_carat_right),
                    contentDescription = if (uiState.isExpanded) "Collapse genres" else "Expand genres",
                    modifier = Modifier.graphicsLayer { rotationZ = if (uiState.isExpanded) 90f else 0f },
                )
            }
        }

        AnimatedVisibility(visible = uiState.isExpanded) {
            Column(
                modifier = Modifier
                    .padding(start = 32.dp)
            ) {
                uiState.genres.forEach { genre ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = genre.isChecked,
                            onCheckedChange = { checked -> onToggleGenre(genre.group, checked) },
                        )
                        Text(text = genre.group.displayName())
                    }
                }
            }
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
            toggleShowExtraDays = {},
            togglePerformanceType = { _, _ -> },
            toggleGenreGroup = { _, _ -> },
            toggleGenreDropdown = {},
            saveFilter = {},
        )
    }
}
