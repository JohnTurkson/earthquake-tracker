package com.johnturkson.sandbox.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.johnturkson.sandbox.data.EarthquakeNotifications
import com.johnturkson.sandbox.data.EarthquakeSettingsRepository
import com.johnturkson.sandbox.ui.EarthquakeNotificationsViewModel.Factory
import com.johnturkson.sandbox.work.EarthquakeWorkScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = Factory::class)
class EarthquakeNotificationsViewModel
@AssistedInject
constructor(
    @Assisted val navKey: Destination.NotificationsRoute,
    private val settingsRepository: EarthquakeSettingsRepository,
    private val notificationManager: EarthquakeNotifications,
    private val workScheduler: EarthquakeWorkScheduler,
    monitoringObserver: EarthquakeMonitoringObserver,
) : ViewModel(), EarthquakeNotificationsState {
    private var settingsLoaded = false
    private var capabilitiesLoaded = false
    override var notifications by mutableStateOf(EarthquakeNotificationState())
        private set

    override var monitoring by mutableStateOf(EarthquakeMonitoringState(status = EarthquakeMonitoringState.Status.Checking))
        private set

    init {
        viewModelScope.launch { monitoringObserver.states.collect { monitoring = it } }
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                settingsLoaded = true
                notifications =
                    notifications.copy(
                        isChecking = !capabilitiesLoaded,
                        alertsEnabled = settings.alertsEnabled,
                        minimumMagnitude = settings.minimumMagnitude,
                        hasRequestedPermission = settings.hasRequestedNotifications,
                    )
            }
        }
    }

    fun setMonitoring(enabled: Boolean) {
        if (enabled) workScheduler.startMonitoring() else workScheduler.stopMonitoring()
    }

    fun refreshNotificationCapabilities() {
        val capabilities = notificationManager.capabilities()
        capabilitiesLoaded = true
        notifications =
            notifications.copy(
                isChecking = !settingsLoaded,
                permissionGranted = capabilities.permissionGranted,
                appEnabled = capabilities.appEnabled,
                monitoringChannelEnabled = capabilities.monitoringEnabled,
                alertsChannelEnabled = capabilities.alertsEnabled,
            )
    }

    fun setAlertsEnabled(enabled: Boolean) = saveSettings {
        settingsRepository.setAlertsEnabled(enabled)
    }

    fun setMinimumMagnitude(magnitude: Int) = saveSettings {
        settingsRepository.setMagnitude(magnitude)
    }

    fun recordNotificationPermissionRequest() = saveSettings {
        settingsRepository.recordPermissionRequest()
    }

    fun previewNotification() {
        if (notifications.canPostAlerts) notificationManager.previewAlert()
        refreshNotificationCapabilities()
    }

    private fun saveSettings(update: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                update()
                notifications = notifications.copy(settingsError = false)
            } catch (_: IOException) {
                notifications = notifications.copy(settingsError = true)
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(navKey: Destination.NotificationsRoute): EarthquakeNotificationsViewModel
    }
}
