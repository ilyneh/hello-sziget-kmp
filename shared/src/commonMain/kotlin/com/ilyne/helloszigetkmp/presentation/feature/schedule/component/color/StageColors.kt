package com.ilyne.helloszigetkmp.presentation.feature.schedule.component.color

import androidx.compose.ui.graphics.Color
import com.ilyne.helloszigetkmp.presentation.theme.SzigetPalette
import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json

private val stagePalette = listOf(
    SzigetPalette.HotPink,
    SzigetPalette.PrimaryBlue,
    SzigetPalette.Magenta,
    SzigetPalette.TealGreen,
    SzigetPalette.Red,
    SzigetPalette.RedOrange,
    SzigetPalette.DarkTeal,
)

private const val STAGE_COLOR_ASSIGNMENTS_KEY = "StageColorAssignments"

/**
 * Assigns each stage a color by first-seen position rather than hashCode, so distinct
 * stages don't collide onto the same palette entry once the stage count exceeds the
 * palette size. Assignments are persisted so a stage keeps its color across app launches
 * instead of it depending on hash distribution or the current filter.
 */
private object StageColorAssignments {
    private val settings = Settings()
    private val assignments: MutableMap<String, Int> = loadAssignments().toMutableMap()

    fun indexFor(stageId: String): Int {
        // Re-mod stored indices against the current palette size — if the palette is ever
        // resized, old persisted indices (from a larger palette) would otherwise be out of
        // bounds here.
        assignments[stageId]?.let { return it.mod(stagePalette.size) }

        val nextIndex = assignments.size.mod(stagePalette.size)
        assignments[stageId] = nextIndex
        persist()
        return nextIndex
    }

    private fun loadAssignments(): Map<String, Int> =
        settings
            .getStringOrNull(STAGE_COLOR_ASSIGNMENTS_KEY)
            ?.let { Json.decodeFromString<Map<String, Int>>(it) }
            ?: emptyMap()

    private fun persist() {
        settings.putString(STAGE_COLOR_ASSIGNMENTS_KEY, Json.encodeToString(assignments))
    }
}

fun stageColor(stageId: String?): Color =
    if (stageId != null) {
        stagePalette[StageColorAssignments.indexFor(stageId)]
    } else {
        SzigetPalette.UnknownStageGray
    }
