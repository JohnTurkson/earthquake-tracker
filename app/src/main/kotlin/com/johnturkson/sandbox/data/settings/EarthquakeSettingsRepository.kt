package com.johnturkson.sandbox.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

@Singleton
class EarthquakeSettingsRepository @Inject constructor(private val store: DataStore<Preferences>) {
    private val alerts = booleanPreferencesKey("earthquake_alerts_enabled")
    private val magnitude = intPreferencesKey("earthquake_alert_magnitude")
    private val requested = booleanPreferencesKey("earthquake_notifications_requested")

    val settings =
        store.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }
            .map { preferences ->
                EarthquakeSettings(
                    preferences[alerts] ?: false,
                    preferences[magnitude]?.takeIf { it in 5..7 } ?: 6,
                    preferences[requested] ?: false,
                )
            }

    suspend fun setAlertsEnabled(enabled: Boolean) {
        store.edit { it[alerts] = enabled }
    }

    suspend fun setMagnitude(value: Int) {
        require(value in 5..7)
        store.edit { it[magnitude] = value }
    }

    suspend fun recordPermissionRequest() {
        store.edit { it[requested] = true }
    }
}
