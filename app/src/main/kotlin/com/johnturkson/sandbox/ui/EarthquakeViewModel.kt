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
import com.johnturkson.sandbox.ui.EarthquakeViewModel.Factory
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
    @Assisted override val navKey: Destination.EarthquakeRoute,
    private val savedStateHandle: SavedStateHandle,
    private val locationRepository: EarthquakeLocationRepository,
    private val locationProvider: EarthquakeLocationProvider,
) : ViewModel(), EarthquakeState {
    private var locationUpdates: Job? = null
    override var initialMapLocation: EarthquakeLocation? by mutableStateOf(null)
        private set

    override var userLocation: EarthquakeLocation? by mutableStateOf(null)
        private set

    init {
        viewModelScope.launch {
            initialMapLocation =
                locationRepository.lastKnownLocation.first() ?: EarthquakeLocation.Default
        }
    }

    override val mapItems: List<EarthquakeMapItem> = EarthquakeMapFixtures.items
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

    override var isListPanelVisible: Boolean by
        savedStateHandle
            .saved(serializer = MutableStateSerializer<Boolean>()) { mutableStateOf(true) }
            .getValue(this, this::isListPanelVisible)
        private set

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

    @AssistedFactory
    interface Factory {
        fun create(navKey: Destination.EarthquakeRoute): EarthquakeViewModel
    }
}
