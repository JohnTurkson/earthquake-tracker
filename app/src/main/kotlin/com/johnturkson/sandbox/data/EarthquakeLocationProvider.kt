package com.johnturkson.sandbox.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class EarthquakeLocationProvider
@Inject
constructor(
    private val client: FusedLocationProviderClient,
    @ApplicationContext private val context: Context,
) {
    val locations: Flow<EarthquakeLocation> = callbackFlow {
        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            close()
            return@callbackFlow
        }
        val callback =
            object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: return
                    trySend(
                        EarthquakeLocation(location.latitude, location.longitude, location.time)
                    )
                }
            }
        val request =
            LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 60_000)
                .setMinUpdateIntervalMillis(15_000)
                .setMinUpdateDistanceMeters(1_000f)
                .build()
        try {
            client.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    trySend(
                        EarthquakeLocation(location.latitude, location.longitude, location.time)
                    )
                }
            }
            client
                .requestLocationUpdates(request, callback, Looper.getMainLooper())
                .addOnFailureListener { close() }
        } catch (_: SecurityException) {
            close()
        }
        awaitClose { client.removeLocationUpdates(callback) }
    }
}
