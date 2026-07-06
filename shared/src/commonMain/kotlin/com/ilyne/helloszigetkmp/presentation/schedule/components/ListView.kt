package com.ilyne.helloszigetkmp.presentation.schedule.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ilyne.helloszigetkmp.presentation.schedule.ScheduleUiState


// ── List View ─────────────────────────────────────────────────────────────────

@Composable
fun SetTimeListView(setTimes: List<ScheduleUiState.SetTime>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(setTimes, key = { it.id }) { setTime ->
            SetTimeCard(setTime = setTime, modifier = Modifier.fillMaxWidth().height(72.dp))
        }
    }
}
