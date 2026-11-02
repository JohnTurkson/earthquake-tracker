package com.johnturkson.sandbox.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer
import com.johnturkson.sandbox.data.EarthquakeRepository
import com.johnturkson.sandbox.ui.EarthquakeListViewModel.Factory
import com.johnturkson.sandbox.work.EarthquakeMonitor
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = Factory::class)
class EarthquakeListViewModel
@AssistedInject
constructor(
    @Assisted val navKey: Destination.EarthquakeListRoute,
    savedStateHandle: SavedStateHandle,
    repository: EarthquakeRepository,
    monitoringObserver: EarthquakeMonitoringObserver,
    private val monitor: EarthquakeMonitor,
) : ViewModel(), EarthquakeListState {
    override var mapItems: List<EarthquakeMapItem> by mutableStateOf(emptyList())
        private set

    override var monitoring by
        mutableStateOf(
            EarthquakeMonitoringState(status = EarthquakeMonitoringState.Status.Checking)
        )
        private set

    override var selectedMapItemId: String? by
        savedStateHandle
            .saved(serializer = MutableStateSerializer<String?>()) { mutableStateOf(null) }
            .getValue(this, this::selectedMapItemId)
        private set

    override var scrollRequest: EarthquakeSelectionResult? by
        savedStateHandle
            .saved(serializer = MutableStateSerializer<EarthquakeSelectionResult?>()) {
                mutableStateOf(null)
            }
            .getValue(this, this::scrollRequest)
        private set

    private var selectionRevision: Long by
        savedStateHandle
            .saved(serializer = MutableStateSerializer<Long>()) { mutableStateOf(0L) }
            .getValue(this, this::selectionRevision)

    init {
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
    }

    fun applySelection(result: EarthquakeSelectionResult, scroll: Boolean = true) {
        if (result.revision <= selectionRevision) return
        selectionRevision = result.revision
        selectedMapItemId = result.earthquakeId
        scrollRequest = if (scroll && result.earthquakeId != null) result else null
    }

    fun consumeScrollRequest(request: EarthquakeSelectionResult) {
        if (scrollRequest == request) scrollRequest = null
    }

    fun refresh() = monitor.refresh()

    @AssistedFactory
    interface Factory {
        fun create(navKey: Destination.EarthquakeListRoute): EarthquakeListViewModel
    }
}
