package com.ilyne.helloszigetkmp.presentation.feature.schedule.filter

import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json

private const val KEY = "ScheduleFilterStorage"

class ScheduleFilterStorage(
    private val settings: Settings,
) {
    fun save(filter: ScheduleFilter) {
        settings.putString(KEY, Json.encodeToString(filter))
    }

    // A stored value that fails to decode (e.g. the persisted shape changed incompatibly across
    // an app update) must not crash ScheduleViewModel's construction - fall back to "no filter"
    // rather than propagating the SerializationException.
    fun read(): ScheduleFilter? =
        settings.getStringOrNull(KEY)?.let {
            runCatching { Json.decodeFromString<ScheduleFilter>(it) }.getOrNull()
        }
}
