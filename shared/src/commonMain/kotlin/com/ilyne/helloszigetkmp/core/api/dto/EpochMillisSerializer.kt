package com.ilyne.helloszigetkmp.core.api.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Instant

/**
 * Deserializes an ISO-8601 date-time string (e.g. "2026-08-08T18:00:00Z") from the API into epoch milliseconds, and serializes back the
 * same way.
 */
object EpochMillisSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("EpochMillis", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Long = Instant.parse(decoder.decodeString()).toEpochMilliseconds()

    override fun serialize(
        encoder: Encoder,
        value: Long,
    ) {
        encoder.encodeString(Instant.fromEpochMilliseconds(value).toString())
    }
}
