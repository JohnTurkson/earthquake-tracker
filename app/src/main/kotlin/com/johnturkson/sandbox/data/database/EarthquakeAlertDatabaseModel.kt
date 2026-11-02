package com.johnturkson.sandbox.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Instant

@Entity(tableName = "earthquake_alerts")
data class EarthquakeAlertDatabaseModel(@PrimaryKey val eventId: String, val deliveredAt: Instant)
