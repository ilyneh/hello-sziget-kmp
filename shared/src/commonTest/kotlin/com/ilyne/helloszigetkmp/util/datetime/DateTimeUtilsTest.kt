package com.ilyne.helloszigetkmp.util.datetime

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [normalizedFestivalHourRange]/[normalizedFestivalHourFractionRange] normalize a set's start/end onto a single monotonic timeline for
 * rendering (Swimlane bar width, Grid height, etc). Each endpoint is normalized independently against the 6am festival-day cutoff, which
 * is correct for sets fully before the cutoff, fully after it, or crossing midnight while staying under the cutoff — but naively doing
 * so breaks for a set that starts before the cutoff and ends at/after it (e.g. 4:30-6:00), since only the start gets pushed past 24.
 */
class DateTimeUtilsTest {
    private val budapest = TimeZone.of("Europe/Budapest")

    private fun millisAt(isoLocalDateTime: String): Long = LocalDateTime.parse(isoLocalDateTime).toInstant(budapest).toEpochMilliseconds()

    @Test
    fun setSpanningTheCutoff_430to600_endsAfterStart() {
        // The reported bug: 4:30-6:00 independently normalizes to start=28.5, end=6.0 (since 6 is not < 6), which is
        // before the start. The paired range must push the end past 24 too so the duration stays a positive 1.5 hours.
        val start = millisAt("2026-08-07T04:30:00")
        val end = millisAt("2026-08-07T06:00:00")

        val (startFraction, endFraction) = normalizedFestivalHourFractionRange(start, end, budapest)

        assertEquals(28.5, startFraction)
        assertEquals(30.0, endFraction)
        assertEquals(1.5, endFraction - startFraction)

        val (startHour, endHour) = normalizedFestivalHourRange(start, end, budapest)
        assertEquals(28, startHour)
        assertEquals(30, endHour)
    }

    @Test
    fun setSpanningTheCutoff_longSetPastCutoff_430to715_endsAfterStart() {
        // A longer early-morning set that keeps running well past the cutoff hour (not just exactly at it).
        val start = millisAt("2026-08-07T04:30:00")
        val end = millisAt("2026-08-07T07:15:00")

        val (startFraction, endFraction) = normalizedFestivalHourFractionRange(start, end, budapest)

        assertEquals(28.5, startFraction)
        assertEquals(31.25, endFraction)
        assertEquals(2.75, endFraction - startFraction)
    }

    @Test
    fun setFullyBeforeCutoff_200to400_isUnaffected() {
        val start = millisAt("2026-08-07T02:00:00")
        val end = millisAt("2026-08-07T04:00:00")

        val (startFraction, endFraction) = normalizedFestivalHourFractionRange(start, end, budapest)

        assertEquals(26.0, startFraction)
        assertEquals(28.0, endFraction)
        assertEquals(2.0, endFraction - startFraction)
    }

    @Test
    fun setFullyAfterCutoff_14to16_isUnaffected() {
        val start = millisAt("2026-08-07T14:00:00")
        val end = millisAt("2026-08-07T16:00:00")

        val (startFraction, endFraction) = normalizedFestivalHourFractionRange(start, end, budapest)

        assertEquals(14.0, startFraction)
        assertEquals(16.0, endFraction)
        assertEquals(2.0, endFraction - startFraction)
    }

    @Test
    fun setCrossingMidnightButStayingBeforeCutoff_23to2_isUnaffected() {
        // Late-evening set that runs past midnight but ends before the cutoff — already handled correctly
        // by independent per-endpoint normalization (start unshifted at 23, end pushed to 26).
        val start = millisAt("2026-08-07T23:00:00")
        val end = millisAt("2026-08-08T02:00:00")

        val (startFraction, endFraction) = normalizedFestivalHourFractionRange(start, end, budapest)

        assertEquals(23.0, startFraction)
        assertEquals(26.0, endFraction)
        assertEquals(3.0, endFraction - startFraction)
    }

    @Test
    fun setShortAndFullyBeforeCutoff_500to545_isUnaffected() {
        val start = millisAt("2026-08-07T05:00:00")
        val end = millisAt("2026-08-07T05:45:00")

        val (startFraction, endFraction) = normalizedFestivalHourFractionRange(start, end, budapest)

        assertEquals(29.0, startFraction)
        assertEquals(29.75, endFraction)
        assertEquals(0.75, endFraction - startFraction)
    }

    @Test
    fun everyCase_endIsAlwaysAfterStart() {
        val cases = listOf(
            "2026-08-07T04:30:00" to "2026-08-07T06:00:00",
            "2026-08-07T04:30:00" to "2026-08-07T07:15:00",
            "2026-08-07T02:00:00" to "2026-08-07T04:00:00",
            "2026-08-07T14:00:00" to "2026-08-07T16:00:00",
            "2026-08-07T23:00:00" to "2026-08-08T02:00:00",
            "2026-08-07T05:00:00" to "2026-08-07T05:45:00",
        )

        for ((startIso, endIso) in cases) {
            val (startFraction, endFraction) = normalizedFestivalHourFractionRange(millisAt(startIso), millisAt(endIso), budapest)
            assertTrue(endFraction > startFraction, "expected end > start for $startIso - $endIso, got $startFraction -> $endFraction")
        }
    }
}
