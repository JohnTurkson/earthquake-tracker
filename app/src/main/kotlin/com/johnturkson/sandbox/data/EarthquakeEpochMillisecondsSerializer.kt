package com.johnturkson.sandbox.data

import kotlin.time.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object EarthquakeEpochMillisecondsSerializer : KSerializer<Instant> {
    override val descriptor =
        PrimitiveSerialDescriptor("EarthquakeEpochMilliseconds", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Instant =
        Instant.fromEpochMilliseconds(decoder.decodeLong())

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeLong(value.toEpochMilliseconds())
    }
}
