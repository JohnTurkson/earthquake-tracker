package com.johnturkson.sandbox.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class EarthquakeLocationRepository
@Inject
constructor(private val dataStore: DataStore<Preferences>) {
    val lastKnownLocation: Flow<EarthquakeLocation?> =
        dataStore.data
            .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
            .map { preferences ->
                val latitude = preferences[Latitude] ?: return@map null
                val longitude = preferences[Longitude] ?: return@map null
                if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return@map null
                EarthquakeLocation(latitude, longitude, preferences[Timestamp])
            }

    suspend fun updateLastKnownLocation(location: EarthquakeLocation) {
        require(location.latitude in -90.0..90.0 && location.longitude in -180.0..180.0)
        dataStore.edit { preferences ->
            val previousTimestamp = preferences[Timestamp]
            val timestampMillis = location.timestampMillis
            if (
                previousTimestamp != null &&
                    timestampMillis != null &&
                    timestampMillis < previousTimestamp
            ) {
                return@edit
            }
            preferences[Latitude] = location.latitude
            preferences[Longitude] = location.longitude
            if (timestampMillis != null) preferences[Timestamp] = timestampMillis
            else preferences.remove(Timestamp)
        }
    }

    private companion object {
        val Latitude = doublePreferencesKey("last_known_user_latitude")
        val Longitude = doublePreferencesKey("last_known_user_longitude")
        val Timestamp = longPreferencesKey("last_known_user_timestamp_millis")
    }
}
