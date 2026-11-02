package com.johnturkson.sandbox.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EarthquakeConnectivity @Inject constructor(@ApplicationContext context: Context) :
    EarthquakeConnection {
    private val manager = context.getSystemService(ConnectivityManager::class.java)

    override fun isOnline(): Boolean =
        manager
            .getNetworkCapabilities(manager.activeNetwork)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
}
