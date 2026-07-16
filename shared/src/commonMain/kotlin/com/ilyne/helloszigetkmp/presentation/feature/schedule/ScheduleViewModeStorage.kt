package com.ilyne.helloszigetkmp.presentation.feature.schedule

import com.russhwolf.settings.Settings

private const val KEY = "ScheduleViewModeStorage"

class ScheduleViewModeStorage(
    private val settings: Settings,
) {
    fun save(viewMode: ViewMode) {
        settings.putString(KEY, viewMode.name)
    }

    fun read(): ViewMode? {
        val name = settings.getStringOrNull(KEY) ?: return null
        return ViewMode.entries.firstOrNull { it.name == name }
    }
}
