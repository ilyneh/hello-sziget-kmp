package com.ilyne.helloszigetkmp.core.repository

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * [SoftRefreshGate] gates a repository's refresh() calls behind a last-fetched-at timestamp
 * persisted in [Settings], so routine screen loads can skip the network call while an explicit
 * force refresh always re-fetches. Backed by a controllable [FakeClock] since the production
 * class only hardcoded [Clock.System] before this test added constructor injection.
 */
class SoftRefreshGateTest {
    private val threshold = 3.hours

    @Test
    fun refreshIfStale_neverFetchedBefore_runsFetch() =
        runTest {
            val gate = SoftRefreshGate(MapSettings(), "key", FakeClock(epochMillis = 1_000_000))
            var fetchCount = 0

            gate.refreshIfStale(force = false, threshold = threshold) { fetchCount++ }

            assertEquals(1, fetchCount)
        }

    @Test
    fun refreshIfStale_force_alwaysRunsFetch_regardlessOfStaleness() =
        runTest {
            val clock = FakeClock(epochMillis = 1_000_000)
            val settings = MapSettings()
            val gate = SoftRefreshGate(settings, "key", clock)

            // First fetch marks lastFetchedAt = now, so a normal (non-forced) call right after
            // would be skipped.
            gate.refreshIfStale(force = false, threshold = threshold) { }

            var fetchCount = 0
            gate.refreshIfStale(force = true, threshold = threshold) { fetchCount++ }

            assertEquals(1, fetchCount, "force = true must always invoke fetch even when fresh")
        }

    @Test
    fun refreshIfStale_notForced_freshData_skipsFetch() =
        runTest {
            val clock = FakeClock(epochMillis = 1_000_000)
            val gate = SoftRefreshGate(MapSettings(), "key", clock)

            gate.refreshIfStale(force = false, threshold = threshold) { }

            // Advance well under the threshold.
            clock.epochMillis += 1.hours.inWholeMilliseconds

            var fetchCount = 0
            gate.refreshIfStale(force = false, threshold = threshold) { fetchCount++ }

            assertEquals(0, fetchCount, "fresh data must not trigger a fetch")
        }

    @Test
    fun refreshIfStale_notForced_staleData_runsFetch() =
        runTest {
            val clock = FakeClock(epochMillis = 1_000_000)
            val gate = SoftRefreshGate(MapSettings(), "key", clock)

            gate.refreshIfStale(force = false, threshold = threshold) { }

            // Advance well past the threshold.
            clock.epochMillis += 4.hours.inWholeMilliseconds

            var fetchCount = 0
            gate.refreshIfStale(force = false, threshold = threshold) { fetchCount++ }

            assertEquals(1, fetchCount, "stale data must trigger a fetch")
        }

    @Test
    fun isStale_exactlyAtThreshold_isConsideredStale() =
        runTest {
            val clock = FakeClock(epochMillis = 1_000_000)
            val gate = SoftRefreshGate(MapSettings(), "key", clock)

            gate.refreshIfStale(force = false, threshold = threshold) { }

            // Advance exactly to the threshold boundary (elapsed >= threshold => stale).
            clock.epochMillis += threshold.inWholeMilliseconds

            var fetchCount = 0
            gate.refreshIfStale(force = false, threshold = threshold) { fetchCount++ }

            assertEquals(1, fetchCount, "elapsed == threshold must count as stale")
        }

    @Test
    fun isStale_justUnderThreshold_isNotStale() =
        runTest {
            val clock = FakeClock(epochMillis = 1_000_000)
            val gate = SoftRefreshGate(MapSettings(), "key", clock)

            gate.refreshIfStale(force = false, threshold = threshold) { }

            clock.epochMillis += threshold.inWholeMilliseconds - 1

            var fetchCount = 0
            gate.refreshIfStale(force = false, threshold = threshold) { fetchCount++ }

            assertEquals(0, fetchCount, "elapsed just under threshold must not count as stale")
        }

    @Test
    fun isStale_justOverThreshold_isStale() =
        runTest {
            val clock = FakeClock(epochMillis = 1_000_000)
            val gate = SoftRefreshGate(MapSettings(), "key", clock)

            gate.refreshIfStale(force = false, threshold = threshold) { }

            clock.epochMillis += threshold.inWholeMilliseconds + 1

            var fetchCount = 0
            gate.refreshIfStale(force = false, threshold = threshold) { fetchCount++ }

            assertEquals(1, fetchCount, "elapsed just over threshold must count as stale")
        }

    @Test
    fun refreshIfStale_failingFetch_doesNotPersistLastFetchedAt() =
        runTest {
            val clock = FakeClock(epochMillis = 1_000_000)
            val settings = MapSettings()
            val gate = SoftRefreshGate(settings, "key", clock)

            try {
                gate.refreshIfStale(force = false, threshold = threshold) {
                    throw IllegalStateException("network failure")
                }
            } catch (e: IllegalStateException) {
                // expected
            }

            assertFalse(
                settings.hasKey("SoftRefreshGate_lastFetchedAt_key"),
                "a failing fetch must not persist lastFetchedAt",
            )

            // Since lastFetchedAt was never persisted, the very next non-forced call should
            // still be considered stale and should run fetch again.
            var fetchCount = 0
            gate.refreshIfStale(force = false, threshold = threshold) { fetchCount++ }

            assertEquals(1, fetchCount, "gate must still be stale after a failed fetch")
        }

    @Test
    fun refreshIfStale_successfulFetch_persistsLastFetchedAt() =
        runTest {
            val clock = FakeClock(epochMillis = 1_000_000)
            val settings = MapSettings()
            val gate = SoftRefreshGate(settings, "key", clock)

            gate.refreshIfStale(force = false, threshold = threshold) { }

            assertEquals(
                1_000_000L,
                settings.getLongOrNull("SoftRefreshGate_lastFetchedAt_key"),
            )
        }

    @Test
    fun clearAll_removesOnlySoftRefreshGateKeys() {
        val settings = MapSettings()
        settings.putLong("SoftRefreshGate_lastFetchedAt_ArtistRepository", 123L)
        settings.putLong("SoftRefreshGate_lastFetchedAt_FriendRepository", 456L)
        settings.putString("unrelated_key", "should survive")
        settings.putLong("some_other_prefixed_but_different_thing", 789L)

        SoftRefreshGate.clearAll(settings)

        assertFalse(settings.hasKey("SoftRefreshGate_lastFetchedAt_ArtistRepository"))
        assertFalse(settings.hasKey("SoftRefreshGate_lastFetchedAt_FriendRepository"))
        assertTrue(settings.hasKey("unrelated_key"))
        assertTrue(settings.hasKey("some_other_prefixed_but_different_thing"))
        assertEquals("should survive", settings.getStringOrNull("unrelated_key"))
    }

    private class FakeClock(
        var epochMillis: Long,
    ) : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(epochMillis)
    }
}
