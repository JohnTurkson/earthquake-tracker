package com.johnturkson.sandbox.data.database

import androidx.room.TypeConverter
import java.time.format.DateTimeFormatterBuilder
import kotlin.time.Instant
import kotlin.time.toJavaInstant

class EarthquakeInstantConverters {
    @TypeConverter fun fromTimestamp(value: String?): Instant? = value?.let(Instant::parse)

    @TypeConverter
    fun toTimestamp(value: Instant?): String? = value?.let {
        require(it >= Minimum && it < Maximum)
        Formatter.format(it.toJavaInstant())
    }

    private companion object {
        val Minimum = Instant.parse("0000-01-01T00:00:00Z")
        val Maximum = Instant.parse("+10000-01-01T00:00:00Z")
        val Formatter = DateTimeFormatterBuilder().appendInstant(9).toFormatter()
    }
}
