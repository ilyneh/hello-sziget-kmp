package com.ilyne.helloszigetkmp.presentation.feature.discover.filter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
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
import hello_sziget_kmp.shared.generated.resources.Res
import hello_sziget_kmp.shared.generated.resources.common_filters
import hello_sziget_kmp.shared.generated.resources.common_performance_types
import hello_sziget_kmp.shared.generated.resources.common_save
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DiscoverFilterScreen(
    initialFilter: DiscoverFilter,
    onSave: (DiscoverFilter) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<DiscoverFilterViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnSave by rememberUpdatedState(onSave)

    LaunchedEffect(Unit) {
        viewModel.onIntent(DiscoverFilterIntent.Initialize(initialFilter))
        viewModel.effects.collect { effect ->
            when (effect) {
                is DiscoverFilterEffect.UpdateFilter -> currentOnSave(effect.filter)
            }
        }
    }

    AppModalBottomSheet(
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        DiscoverFilterContent(
            uiState = uiState,
            togglePerformanceType = { type -> viewModel.onIntent(DiscoverFilterIntent.TogglePerformanceType(type)) },
            toggleGenreGroup = { group, checked -> viewModel.onIntent(DiscoverFilterIntent.ToggleGenreGroup(group, checked)) },
            toggleGenreDropdown = { type -> viewModel.onIntent(DiscoverFilterIntent.ToggleGenreDropdown(type)) },
            saveFilter = { viewModel.onIntent(DiscoverFilterIntent.Save) },
        )
    }
}

@Composable
private fun DiscoverFilterContent(
    uiState: DiscoverFilterUiState,
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
        ModalHeader(text = stringResource(Res.string.common_filters))
        SubHeader2(text = stringResource(Res.string.common_performance_types))

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
                text = stringResource(Res.string.common_save),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Preview
@Composable
private fun DiscoverFilterContentPreview() {
    AppTheme {
        DiscoverFilterContent(
            uiState = DiscoverFilterUiState(),
            togglePerformanceType = {},
            toggleGenreGroup = { _, _ -> },
            toggleGenreDropdown = {},
            saveFilter = {},
        )
    }
}
