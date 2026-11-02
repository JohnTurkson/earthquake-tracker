package com.johnturkson.sandbox.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.johnturkson.sandbox.annotations.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn

@Singleton
class EarthquakeConnectivity
@Inject
constructor(@ApplicationContext context: Context, @IoDispatcher dispatcher: CoroutineDispatcher) :
    EarthquakeConnection {
    private val manager = context.getSystemService(ConnectivityManager::class.java)

    override fun isOnline(): Boolean =
        manager
            .getNetworkCapabilities(manager.activeNetwork)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true

    override val online = callbackFlow {
        val callback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    trySend(isOnline())
                }

                override fun onLost(network: Network) {
                    trySend(isOnline())
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities,
                ) {
                    trySend(isOnline())
                }
            }
        manager.registerDefaultNetworkCallback(callback)
        trySend(isOnline())
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }
        .distinctUntilChanged()
        .stateIn(CoroutineScope(dispatcher + SupervisorJob()), SharingStarted.Eagerly, isOnline())
}
