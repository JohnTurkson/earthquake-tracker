package com.johnturkson.sandbox.ui.details

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.johnturkson.sandbox.data.repository.EarthquakeRepository
import com.johnturkson.sandbox.ui.details.EarthquakeDetailsViewModel.Factory
import com.johnturkson.sandbox.ui.event.toMapItem
import com.johnturkson.sandbox.ui.navigation.Destination
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = Factory::class)
class EarthquakeDetailsViewModel
@AssistedInject
constructor(
    @Assisted val navKey: Destination.EarthquakeDetailsRoute,
    repository: EarthquakeRepository,
) : ViewModel(), EarthquakeDetailsState {
    override var isLoading by mutableStateOf(true)
        private set

    override var item: EarthquakeDetailsItem? by mutableStateOf(null)
        private set

    init {
        viewModelScope.launch {
            repository.getEarthquakeFlow(navKey.earthquakeId).collect { record ->
                item = record?.let {
                    EarthquakeDetailsItem(
                        mapItem = it.toMapItem(),
                        depth = it.depth,
                        updated = it.updated,
                        place = it.place,
                        magnitudeType = it.magnitudeType,
                        status = it.status,
                        felt = it.felt,
                        communityIntensity = it.cdi,
                        network = it.network,
                        url = it.url,
                    )
                }
                isLoading = false
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(navKey: Destination.EarthquakeDetailsRoute): EarthquakeDetailsViewModel
    }
}
