package com.ilyne.helloszigetkmp.core.repository

import com.russhwolf.settings.Settings
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

private const val KEY_PREFIX = "SoftRefreshGate_lastFetchedAt_"

/**
 * Guards a repository's `refresh()` so routine screen loads can skip the network call
 * (and rely on the local Room cache) if data was already fetched within [Duration]
 * [threshold] ago, while an explicit pull-to-refresh (force = true) always re-fetches.
 *
 * One instance per data source, keyed by [key], backed by [Settings] so the last-fetched
 * timestamp survives process death.
 */
class SoftRefreshGate(
    private val settings: Settings,
    key: String,
    private val clock: Clock = Clock.System,
) {
    private val lastFetchedKey = KEY_PREFIX + key

    /**
     * Runs [fetch] only if [force] is true or the last successful fetch is older than
     * [threshold] (or has never happened). On a skipped call, callers should rely on
     * their already-populated local DB cache. Records "now" as the last-fetched time
     * only after [fetch] completes successfully.
     */
    suspend fun refreshIfStale(
        force: Boolean = false,
        threshold: Duration = 3.hours,
        fetch: suspend () -> Unit,
    ) {
        if (!force && !isStale(threshold)) return
        fetch()
        settings.putLong(lastFetchedKey, clock.now().toEpochMilliseconds())
    }

    private fun isStale(threshold: Duration): Boolean {
        val lastFetchedAt = settings.getLongOrNull(lastFetchedKey) ?: return true
        val elapsed = clock.now().toEpochMilliseconds() - lastFetchedAt
        return elapsed >= threshold.inWholeMilliseconds
    }

    companion object {
        /**
         * Clears every gate's last-fetched timestamp. [Settings] survives a Room destructive
         * migration (they're backed by separate storage), so without this a wiped local DB
         * would still look "fresh" to every gate and screens would show empty state for up to
         * their threshold instead of immediately refetching from the backend.
         */
        fun clearAll(settings: Settings) {
            settings.keys.filter { it.startsWith(KEY_PREFIX) }.forEach(settings::remove)
        }
    }
}
