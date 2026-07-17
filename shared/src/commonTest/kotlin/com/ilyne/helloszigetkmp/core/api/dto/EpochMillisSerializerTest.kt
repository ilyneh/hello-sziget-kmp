package com.ilyne.helloszigetkmp.core.api.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class EpochMillisSerializerTest {
    @Serializable
    private data class Wrapper(
        @Serializable(with = EpochMillisSerializer::class) val timestamp: Long,
    )

    @Test
    fun deserialize_isoString_parsesToEpochMillis() {
        val json = Json.decodeFromString<Wrapper>("""{"timestamp":"2026-08-08T18:00:00Z"}""")

        assertEquals(Instant.parse("2026-08-08T18:00:00Z").toEpochMilliseconds(), json.timestamp)
    }

    @Test
    fun serialize_epochMillis_producesIsoString() {
        val millis = Instant.parse("2026-08-08T18:00:00Z").toEpochMilliseconds()

        val encoded = Json.encodeToString(Wrapper(millis))

        assertEquals("""{"timestamp":"2026-08-08T18:00:00Z"}""", encoded)
    }

    @Test
    fun roundTrip_deserializeThenSerialize_producesSameString() {
        val original = """{"timestamp":"2026-08-06T22:30:00Z"}"""

        val decoded = Json.decodeFromString<Wrapper>(original)
        val reEncoded = Json.encodeToString(decoded)

        assertEquals(original, reEncoded)
    }
}
