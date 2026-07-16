package com.ilyne.helloszigetkmp.presentation.component.performancetype

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ilyne.helloszigetkmp.domain.model.GenreGroup
import com.ilyne.helloszigetkmp.domain.model.PerformanceType
import com.ilyne.helloszigetkmp.domain.model.displayName
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import hello_sziget_kmp.shared.generated.resources.Res
import hello_sziget_kmp.shared.generated.resources.ic_carat_right
import org.jetbrains.compose.resources.painterResource

data class GenreCheckboxUiState(
    val group: GenreGroup,
    val isChecked: Boolean,
)

@Composable
fun PerformanceTypeSection(
    type: PerformanceType,
    checkState: ToggleableState,
    isExpanded: Boolean,
    genres: List<GenreCheckboxUiState>,
    onToggleType: () -> Unit,
    onToggleDropdown: () -> Unit,
    onToggleGenre: (GenreGroup, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TriStateCheckbox(
                state = checkState,
                onClick = onToggleType,
            )
            Text(
                text = type.displayName(),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onToggleDropdown) {
                Icon(
                    painter = painterResource(Res.drawable.ic_carat_right),
                    contentDescription = if (isExpanded) "Collapse genres" else "Expand genres",
                    modifier = Modifier.graphicsLayer { rotationZ = if (isExpanded) 90f else 0f },
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .padding(start = 32.dp),
            ) {
                genres.forEach { genre ->
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
private fun PerformanceTypeSectionPreview() {
    AppTheme {
        PerformanceTypeSection(
            type = PerformanceType.MUSIC,
            checkState = ToggleableState.Indeterminate,
            isExpanded = true,
            genres = listOf(
                GenreCheckboxUiState(group = GenreGroup.ROCK, isChecked = true),
                GenreCheckboxUiState(group = GenreGroup.POP, isChecked = false),
            ),
            onToggleType = {},
            onToggleDropdown = {},
            onToggleGenre = { _, _ -> },
        )
    }
}
