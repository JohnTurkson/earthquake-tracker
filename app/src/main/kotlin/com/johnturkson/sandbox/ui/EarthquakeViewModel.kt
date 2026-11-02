package com.johnturkson.sandbox.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer
import com.johnturkson.sandbox.data.EarthquakeLocation
import com.johnturkson.sandbox.data.EarthquakeLocationProvider
import com.johnturkson.sandbox.data.EarthquakeLocationRepository
import com.johnturkson.sandbox.data.EarthquakeRepository
import com.johnturkson.sandbox.ui.EarthquakeViewModel.Factory
import com.johnturkson.sandbox.work.EarthquakeWorkScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = Factory::class)
class EarthquakeViewModel
@AssistedInject
constructor(
    @Assisted override val navKey: Destination.EarthquakeMapRoute,
    savedStateHandle: SavedStateHandle,
    private val locationRepository: EarthquakeLocationRepository,
    private val locationProvider: EarthquakeLocationProvider,
    private val repository: EarthquakeRepository,
    monitoringObserver: EarthquakeMonitoringObserver,
    private val workScheduler: EarthquakeWorkScheduler,
) : ViewModel(), EarthquakeState {
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

    override var mapItems: List<EarthquakeMapItem> by mutableStateOf(emptyList())
        private set

    override var unavailableNotificationEvent by mutableStateOf(false)
        private set

    override var requestedMapFocusId: String? by
        savedStateHandle
            .saved(serializer = MutableStateSerializer<String?>()) { mutableStateOf(null) }
            .getValue(this, this::requestedMapFocusId)
        private set

    override var selectedMapItemId: String? by
        savedStateHandle
            .saved(serializer = MutableStateSerializer<String?>()) { mutableStateOf(null) }
            .getValue(this, this::selectedMapItemId)
        private set

    override var selectedClusterItemIds: List<String> by
        savedStateHandle
            .saved(serializer = MutableStateSerializer<List<String>>()) {
                mutableStateOf(emptyList())
            }
            .getValue(this, this::selectedClusterItemIds)
        private set

    private var selectionRevision: Long by
        savedStateHandle
            .saved(serializer = MutableStateSerializer<Long>()) { mutableStateOf(0L) }
            .getValue(this, this::selectionRevision)

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
                mapItems = items.map { it.toMapItem() }
            }
        }
        viewModelScope.launch { monitoringObserver.states.collect { monitoring = it } }
        workScheduler.refresh()
    }

    fun applySelection(result: EarthquakeSelectionResult, focus: Boolean = true) {
        if (result.revision <= selectionRevision) return
        selectionRevision = result.revision
        selectedMapItemId = result.earthquakeId
        if (result.earthquakeId == null) dismissMapCluster()
        requestedMapFocusId = if (focus) result.earthquakeId else null
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

    fun openNotificationEvent(eventId: String, onSelected: (String) -> Unit = {}) {
        viewModelScope.launch {
            val item = repository.getEarthquakeFlow(eventId).first()
            if (item != null) {
                dismissMapCluster()
                selectedMapItemId = item.id
                requestedMapFocusId = item.id
                unavailableNotificationEvent = false
                onSelected(item.id)
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
        fun create(navKey: Destination.EarthquakeMapRoute): EarthquakeViewModel
    }
}
