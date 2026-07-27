package com.ilyne.helloszigetkmp.presentation.feature.schedule.component.color

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ilyne.helloszigetkmp.presentation.theme.SzigetPalette
import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject

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
 *
 * Injected via Koin (see `di/AppModule.kt`) rather than kept as a bare global singleton, in
 * line with the rest of the app's `Settings`-backed storage classes (e.g.
 * `ScheduleFilterStorage`). The assignment map is read from disk once, at construction time
 * (i.e. whenever Koin first resolves this single), and cached in memory from then on -
 * `indexFor` itself never touches disk on the read path, so it's safe to call from
 * composition. Persisting a newly-seen stage still does a synchronous write, but that only
 * happens the first time a given stage is encountered, not on every recomposition.
 */
class StageColorAssigner(
    private val settings: Settings,
) {
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

@Composable
fun stageColor(stageId: String?): Color =
    if (stageId != null) {
        val stageColorAssigner = koinInject<StageColorAssigner>()
        stagePalette[stageColorAssigner.indexFor(stageId)]
    } else {
        SzigetPalette.UnknownStageGray
    }
