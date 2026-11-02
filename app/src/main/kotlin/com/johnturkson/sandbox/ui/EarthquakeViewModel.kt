package com.johnturkson.sandbox.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer
import com.johnturkson.sandbox.ui.EarthquakeViewModel.Factory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel(assistedFactory = Factory::class)
class EarthquakeViewModel
@AssistedInject
constructor(
    @Assisted override val navKey: Destination.EarthquakeRoute,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel(), EarthquakeState {
    override val mapItems: List<EarthquakeMapItem> = EarthquakeMapFixtures.items
    override var selectedMapItemId: String? by
        savedStateHandle
            .saved(serializer = MutableStateSerializer<String?>()) { mutableStateOf(null) }
            .getValue(this, this::selectedMapItemId)
        private set

    fun selectMapItem(item: EarthquakeMapItem) {
        selectedMapItemId = item.id
    }

    fun clearMapSelection() {
        selectedMapItemId = null
    }

    @AssistedFactory
    interface Factory {
        fun create(navKey: Destination.EarthquakeRoute): EarthquakeViewModel
    }
}
