package com.ilyne.helloszigetkmp.presentation.feature.schedule

import com.ilyne.helloszigetkmp.core.settings.SettingsStore
import com.russhwolf.settings.Settings

private const val KEY = "ScheduleViewModeStorage"

class ScheduleViewModeStorage(
    settings: Settings,
) {
    private val store = SettingsStore<ViewMode>(
        settings = settings,
        key = KEY,
        encode = { it.name },
        decode = { name -> ViewMode.entries.firstOrNull { it.name == name } },
    )

    fun save(viewMode: ViewMode) = store.save(viewMode)

    fun read(): ViewMode? = store.read()
}
