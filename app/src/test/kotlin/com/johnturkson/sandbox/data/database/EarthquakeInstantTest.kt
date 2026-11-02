package com.johnturkson.sandbox.data.database

import com.johnturkson.sandbox.data.network.EarthquakeEpochMillisecondsSerializer
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Instant
import kotlinx.serialization.json.Json
import org.junit.Test

class EarthquakeInstantTest {
    private val converters = EarthquakeInstantConverters()

    @Test
    fun databaseTimestampsNormalizeOffsetsAndPreserveNanoseconds() {
        val instant = Instant.parse("2026-11-01T02:00:00.123456789+02:00")
        val stored = converters.toTimestamp(instant)
        assertEquals("2026-11-01T00:00:00.123456789Z", stored)
        assertEquals(instant, converters.fromTimestamp(stored))
        assertNull(converters.toTimestamp(null))
        assertNull(converters.fromTimestamp(null))
    }

    @Test
    fun textOrderingMatchesChronologyAcrossFractionAndEpochBoundaries() {
        val instants =
            listOf(
                Instant.parse("0000-01-01T00:00:00Z"),
                Instant.parse("1969-12-31T23:59:59.999999999Z"),
                Instant.parse("1970-01-01T00:00:00Z"),
                Instant.parse("1970-01-01T00:00:00Z") + 1.nanoseconds,
                Instant.parse("1970-01-01T00:00:00.1Z"),
                Instant.parse("9999-12-31T23:59:59.999999999Z"),
            )
        assertEquals(
            instants,
            instants
                .reversed()
                .map { converters.toTimestamp(it)!! }
                .sorted()
                .map { converters.fromTimestamp(it) },
        )
        assertFailsWith<IllegalArgumentException> {
            converters.toTimestamp(Instant.parse("-0001-12-31T23:59:59Z"))
        }
        assertFailsWith<IllegalArgumentException> {
            converters.toTimestamp(Instant.parse("+10000-01-01T00:00:00Z"))
        }
    }

    @Test
    fun numericWireTimestampsDecodeWithoutLosingMilliseconds() {
        for (millis in listOf(-1L, 0L, 1700000000123L)) {
            val instant = Instant.fromEpochMilliseconds(millis)
            assertEquals(
                instant,
                Json.decodeFromString(EarthquakeEpochMillisecondsSerializer, millis.toString()),
            )
            assertEquals(
                millis.toString(),
                Json.encodeToString(EarthquakeEpochMillisecondsSerializer, instant),
            )
        }
    }

    @Test
    fun defaultSerializationUsesIsoStringsAndPreservesNanoseconds() {
        val instant = Instant.parse("2026-11-01T00:00:00.123456789Z")
        val serialized = Json.encodeToString(instant)
        assertEquals("\"2026-11-01T00:00:00.123456789Z\"", serialized)
        assertEquals(instant, Json.decodeFromString<Instant>(serialized))
    }
}
