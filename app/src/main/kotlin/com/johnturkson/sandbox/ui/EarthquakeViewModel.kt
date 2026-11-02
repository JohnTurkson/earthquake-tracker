package com.johnturkson.sandbox.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer
import androidx.work.WorkInfo
import com.johnturkson.sandbox.data.EarthquakeConnection
import com.johnturkson.sandbox.data.EarthquakeLocation
import com.johnturkson.sandbox.data.EarthquakeLocationProvider
import com.johnturkson.sandbox.data.EarthquakeLocationRepository
import com.johnturkson.sandbox.data.EarthquakeNotifications
import com.johnturkson.sandbox.data.EarthquakeRepository
import com.johnturkson.sandbox.data.EarthquakeSettingsRepository
import com.johnturkson.sandbox.ui.EarthquakeViewModel.Factory
import com.johnturkson.sandbox.work.EarthquakeWorkScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = Factory::class)
class EarthquakeViewModel
@AssistedInject
constructor(
    @Assisted override val navKey: Destination.EarthquakeRoute,
    private val savedStateHandle: SavedStateHandle,
    private val locationRepository: EarthquakeLocationRepository,
    private val locationProvider: EarthquakeLocationProvider,
    private val repository: EarthquakeRepository,
    private val settingsRepository: EarthquakeSettingsRepository,
    private val notificationManager: EarthquakeNotifications,
    private val workScheduler: EarthquakeWorkScheduler,
    private val connectivity: EarthquakeConnection,
) : ViewModel(), EarthquakeState {
    private var settingsLoaded = false
    private var capabilitiesLoaded = false
    private var locationUpdates: Job? = null
    override var initialMapLocation: EarthquakeLocation? by mutableStateOf(null)
        private set

    override var userLocation: EarthquakeLocation? by mutableStateOf(null)
        private set

    override var monitoring by
        mutableStateOf(
            EarthquakeMonitoringState(status = EarthquakeMonitoringState.Status.Checking)
        )
        private set

    override var notifications by mutableStateOf(EarthquakeNotificationState())
        private set

    override var requestedMapFocusId: String? by mutableStateOf(null)
        private set

    override var unavailableNotificationEvent by mutableStateOf(false)
        private set

    override var notificationsVisible: Boolean by
        savedStateHandle
            .saved(serializer = MutableStateSerializer<Boolean>()) { mutableStateOf(false) }
            .getValue(this, this::notificationsVisible)
        private set

    override var mapItems: List<EarthquakeMapItem> by mutableStateOf(emptyList())
        private set

    override var selectedMapItemId: String? by
        savedStateHandle
            .saved(serializer = MutableStateSerializer<String?>()) { mutableStateOf(null) }
            .getValue(this, this::selectedMapItemId)
        private set

    override var selectedView: EarthquakeView by
        savedStateHandle
            .saved(serializer = MutableStateSerializer<EarthquakeView>()) {
                mutableStateOf(EarthquakeView.Map)
            }
            .getValue(this, this::selectedView)
        private set

    override var selectedClusterItemIds: List<String> by
        savedStateHandle
            .saved(serializer = MutableStateSerializer<List<String>>()) {
                mutableStateOf(emptyList())
            }
            .getValue(this, this::selectedClusterItemIds)
        private set

    override var isListPanelVisible: Boolean by
        savedStateHandle
            .saved(serializer = MutableStateSerializer<Boolean>()) { mutableStateOf(true) }
            .getValue(this, this::isListPanelVisible)
        private set

    init {
        viewModelScope.launch {
            initialMapLocation =
                locationRepository.lastKnownLocation.first() ?: EarthquakeLocation.Default
        }
        viewModelScope.launch {
            repository.getEarthquakesFlow().collect { items ->
                val selected = selectedMapItemId
                if (selected != null && items.none { it.id == selected }) {
                    repository.getEarthquakeFlow(selected).first()?.let {
                        selectedMapItemId = it.id
                    }
                }
                mapItems = items.map { item ->
                    val magnitude =
                        item.magnitude?.let { String.format(Locale.ROOT, "%.1f", it) } ?: "?"
                    EarthquakeMapItem(
                        item.id,
                        item.latitude,
                        item.longitude,
                        item.title ?: "M $magnitude – ${item.place ?: "Unknown location"}",
                        eventTime = item.time,
                        magnitude = item.magnitude,
                    )
                }
                monitoring = monitoring.copy(hasStoredData = items.isNotEmpty())
            }
        }
        viewModelScope.launch {
            repository.getRefreshFlow().collect { metadata ->
                monitoring = monitoring.copy(lastUpdated = metadata?.updatedAt)
            }
        }
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
        viewModelScope.launch {
            combine(
                    workScheduler.monitoringWork,
                    workScheduler.refreshWork,
                    connectivity.online,
                ) { monitor, refresh, online ->
                    val status =
                        when (monitor?.state) {
                            WorkInfo.State.ENQUEUED,
                            WorkInfo.State.BLOCKED -> EarthquakeMonitoringState.Status.Starting
                            WorkInfo.State.RUNNING ->
                                if (online) EarthquakeMonitoringState.Status.Running
                                else EarthquakeMonitoringState.Status.Offline
                            WorkInfo.State.FAILED -> EarthquakeMonitoringState.Status.Failed
                            else -> EarthquakeMonitoringState.Status.Stopped
                        }
                    val refreshing =
                        when (refresh?.state) {
                            WorkInfo.State.RUNNING -> EarthquakeMonitoringState.Refresh.Running
                            WorkInfo.State.ENQUEUED,
                            WorkInfo.State.BLOCKED -> EarthquakeMonitoringState.Refresh.Queued
                            WorkInfo.State.FAILED -> EarthquakeMonitoringState.Refresh.Failed
                            else -> EarthquakeMonitoringState.Refresh.Idle
                        }
                    status to refreshing
                }
                .collect { (status, refresh) ->
                    monitoring = monitoring.copy(status = status, refresh = refresh)
                }
        }
        workScheduler.refresh()
    }

    fun updateListPanelVisibility(visible: Boolean) {
        isListPanelVisible = visible
    }

    fun selectView(view: EarthquakeView) {
        selectedView = view
    }

    fun selectMapItem(item: EarthquakeMapItem) {
        selectedMapItemId = item.id
    }

    fun clearMapSelection() {
        selectedMapItemId = null
        dismissMapCluster()
    }

    fun selectMapCluster(items: List<EarthquakeMapItem>) {
        selectedClusterItemIds = items.map { it.id }.distinct()
    }

    fun dismissMapCluster() {
        selectedClusterItemIds = emptyList()
    }

    fun startLocationUpdates() {
        if (locationUpdates?.isActive == true) return
        locationUpdates = viewModelScope.launch {
            locationProvider.locations.collect { location ->
                userLocation = location
                try {
                    locationRepository.updateLastKnownLocation(location)
                } catch (_: IOException) {
                    return@collect
                }
            }
        }
    }

    fun stopLocationUpdates() {
        locationUpdates?.cancel()
        locationUpdates = null
    }

    fun refresh() {
        workScheduler.refresh()
    }

    fun setMonitoring(enabled: Boolean) {
        if (enabled) workScheduler.startMonitoring() else workScheduler.stopMonitoring()
    }

    fun showNotifications(visible: Boolean) {
        notificationsVisible = visible
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

    fun openNotificationEvent(eventId: String) {
        viewModelScope.launch {
            val item = repository.getEarthquakeFlow(eventId).first()
            if (item != null) {
                dismissMapCluster()
                selectedMapItemId = item.id
                requestedMapFocusId = item.id
                selectedView = EarthquakeView.Map
                notificationsVisible = false
                unavailableNotificationEvent = false
            } else unavailableNotificationEvent = true
        }
    }

    fun consumeMapFocus() {
        requestedMapFocusId = null
    }

    fun dismissUnavailableEvent() {
        unavailableNotificationEvent = false
    }

    @AssistedFactory
    interface Factory {
        fun create(navKey: Destination.EarthquakeRoute): EarthquakeViewModel
    }
}
