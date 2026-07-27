package com.ilyne.helloszigetkmp.presentation.feature.schedule.filter

import com.ilyne.helloszigetkmp.core.settings.SettingsStore
import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json

private const val KEY = "ScheduleFilterStorage"

class ScheduleFilterStorage(
    settings: Settings,
) {
    // A stored value that fails to decode (e.g. the persisted shape changed incompatibly across
    // an app update) must not crash ScheduleViewModel's construction - fall back to "no filter"
    // rather than propagating the SerializationException.
    private val store = SettingsStore<ScheduleFilter>(
        settings = settings,
        key = KEY,
        encode = { Json.encodeToString(it) },
        decode = { Json.decodeFromString(it) },
    )

    fun save(filter: ScheduleFilter) = store.save(filter)

    fun read(): ScheduleFilter? = store.read()
}
