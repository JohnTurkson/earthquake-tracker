package com.johnturkson.sandbox.data.settings

data class EarthquakeSettings(
    val alertsEnabled: Boolean = false,
    val minimumMagnitude: Int = 6,
    val hasRequestedNotifications: Boolean = false,
)
