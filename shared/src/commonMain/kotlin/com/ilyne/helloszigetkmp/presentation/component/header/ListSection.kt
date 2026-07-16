package com.ilyne.helloszigetkmp.presentation.component.header

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A titled, vertically-stacked list section: a [SectionHeader] followed by a [Column] of
 * [items], each keyed by [itemKey] and rendered via [itemContent].
 */
@Composable
fun <T> ListSection(
    title: String,
    items: List<T>,
    itemKey: (T) -> Any,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit,
) {
    SectionHeader(
        modifier = Modifier.padding(top = 8.dp),
        text = title
    )
    Column(
        modifier = modifier.fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            key(itemKey(item)) {
                itemContent(item)
            }
        }
    }
}
