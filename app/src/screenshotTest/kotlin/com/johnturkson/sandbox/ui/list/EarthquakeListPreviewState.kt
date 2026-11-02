package com.johnturkson.sandbox.ui.list

import com.johnturkson.sandbox.ui.event.EarthquakeMapItem
import com.johnturkson.sandbox.ui.map.EarthquakeMapFixtures
import com.johnturkson.sandbox.ui.monitoring.EarthquakeMonitoringState
import com.johnturkson.sandbox.ui.navigation.EarthquakeSelectionResult

data class EarthquakeListPreviewState(
    override val mapItems: List<EarthquakeMapItem> = EarthquakeMapFixtures.items,
    override val selectedMapItemId: String? = null,
    override val monitoring: EarthquakeMonitoringState = EarthquakeMonitoringState(),
    override val scrollRequest: EarthquakeSelectionResult? = null,
) : EarthquakeListState
