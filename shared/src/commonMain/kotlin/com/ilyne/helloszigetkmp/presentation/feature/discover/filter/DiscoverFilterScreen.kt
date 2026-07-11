package com.ilyne.helloszigetkmp.presentation.feature.discover.filter

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
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
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
import com.ilyne.helloszigetkmp.presentation.component.header.ModalHeader
import com.ilyne.helloszigetkmp.presentation.component.header.SubHeader2
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import hello_sziget_kmp.shared.generated.resources.Res
import hello_sziget_kmp.shared.generated.resources.ic_carat_right
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverFilterScreen(
    initialFilter: DiscoverFilter,
    onSave: (DiscoverFilter) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = koinViewModel<DiscoverFilterViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.onIntent(DiscoverFilterIntent.Initialize(initialFilter))
        viewModel.effects.collect { effect ->
            when (effect) {
                is DiscoverFilterEffect.UpdateFilter -> onSave(effect.filter)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ModalHeader(text = "Filters")
        SubHeader2(text = "Performance Types")

        uiState.performanceTypes.forEach { performanceType ->
            DiscoverPerformanceTypeSection(
                uiState = performanceType,
                onToggleType = { togglePerformanceType(performanceType.type) },
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
private fun DiscoverPerformanceTypeSection(
    uiState: DiscoverPerformanceTypeUiState,
    onToggleType: () -> Unit,
    onToggleDropdown: () -> Unit,
    onToggleGenre: (GenreGroup, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TriStateCheckbox(
                state = uiState.checkState,
                onClick = onToggleType,
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
