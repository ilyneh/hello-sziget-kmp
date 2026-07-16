package com.ilyne.helloszigetkmp.util.datetime

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals

class FestivalDateUtilsTest {
    private val budapest = TimeZone.of("Europe/Budapest")

    private fun millisAt(isoLocalDateTime: String): Long = LocalDateTime.parse(isoLocalDateTime).toInstant(budapest).toEpochMilliseconds()

    @Test
    fun justBeforeCutoff_559am_countsAsPreviousDay() {
        val festivalDate = millisAt("2026-08-08T05:59:00").toFestivalDate()

        assertEquals(LocalDate(2026, 8, 7), festivalDate)
    }

    @Test
    fun exactlyAtCutoff_600am_countsAsCurrentDay() {
        val festivalDate = millisAt("2026-08-08T06:00:00").toFestivalDate()

        assertEquals(LocalDate(2026, 8, 8), festivalDate)
    }

    @Test
    fun justAfterCutoff_601am_countsAsCurrentDay() {
        val festivalDate = millisAt("2026-08-08T06:01:00").toFestivalDate()

        assertEquals(LocalDate(2026, 8, 8), festivalDate)
    }

    @Test
    fun midnight_countsAsPreviousDay() {
        val festivalDate = millisAt("2026-08-08T00:00:00").toFestivalDate()

        assertEquals(LocalDate(2026, 8, 7), festivalDate)
    }

    @Test
    fun midday_countsAsCurrentDay() {
        val festivalDate = millisAt("2026-08-08T14:00:00").toFestivalDate()

        assertEquals(LocalDate(2026, 8, 8), festivalDate)
    }
}
