package com.johnturkson.sandbox.data

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class EarthquakeAlertPolicy {
    fun qualifies(item: EarthquakeDatabaseModel, minimumMagnitude: Int, now: Instant): Boolean =
        item.type == "earthquake" &&
            item.magnitude != null &&
            item.magnitude >= minimumMagnitude &&
            now - item.time in Duration.ZERO..1.minutes
}
