package com.johnturkson.sandbox.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Instant

@Entity(tableName = "earthquake_refresh")
data class EarthquakeRefreshDatabaseModel(@PrimaryKey val id: Int = 1, val updatedAt: Instant)
