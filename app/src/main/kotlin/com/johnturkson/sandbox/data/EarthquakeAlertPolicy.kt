package com.johnturkson.sandbox.data

class EarthquakeAlertPolicy {
    fun qualifies(item: EarthquakeDatabaseModel, minimumMagnitude: Int, nowMillis: Long): Boolean =
        item.type == "earthquake" &&
            item.magnitude != null &&
            item.magnitude >= minimumMagnitude &&
            nowMillis - item.time in 0..60_000L
}
