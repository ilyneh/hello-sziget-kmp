package com.ilyne.helloszigetkmp.core.settings

import com.russhwolf.settings.Settings

/**
 * Shared shape for "encode a value to a String, write it to [Settings] under one key; read it
 * back, decode it" — the pattern hand-rolled by several small persistence classes (TokenStorage,
 * ScheduleFilterStorage, ScheduleViewModeStorage, ...). Those classes keep their own small,
 * feature-specific public API (so call sites elsewhere don't need to change) and delegate the
 * actual Settings read/write/decode-failure-handling to this.
 *
 * [decode] receives the raw stored string, and returning null (or throwing) from it will be
 * treated the same as nothing being stored - the underlying value being unparseable (e.g. its
 * serialized shape changed incompatibly across an app update) must not crash the caller.
 * [onDecodeFailure] additionally runs when [decode] returns null or throws, and is used e.g. by
 * TokenStorage to drop the corrupt entry so it doesn't keep failing to decode on every future
 * read - most callers can leave it as the no-op default.
 */
class SettingsStore<T>(
    private val settings: Settings,
    private val key: String,
    private val encode: (T) -> String,
    private val decode: (String) -> T?,
    private val onDecodeFailure: () -> Unit = {},
) {
    fun save(value: T) {
        settings.putString(key, encode(value))
    }

    fun read(): T? =
        settings.getStringOrNull(key)?.let { raw ->
            runCatching { decode(raw) }.getOrNull().also { decoded ->
                if (decoded == null) onDecodeFailure()
            }
        }

    fun clear() {
        settings.remove(key)
    }
}
