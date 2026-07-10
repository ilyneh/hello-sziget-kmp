package com.ilyne.helloszigetkmp.presentation.feature.schedule.filter

import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json

private const val KEY = "ScheduleFilterStorage"

class ScheduleFilterStorage(private val settings: Settings) {

    fun save(filter: ScheduleFilter) {
        settings.putString(KEY, Json.encodeToString(filter))
    }

    fun read(): ScheduleFilter? {
        return settings.getStringOrNull(KEY)?.let { Json.decodeFromString(it) }
    }
}
