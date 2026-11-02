package com.johnturkson.sandbox.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "earthquake_refresh")
data class EarthquakeRefreshDatabaseModel(@PrimaryKey val id: Int = 1, val updatedMillis: Long)
