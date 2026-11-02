package com.johnturkson.sandbox.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "earthquake_alerts")
data class EarthquakeAlertDatabaseModel(@PrimaryKey val eventId: String, val deliveredMillis: Long)
