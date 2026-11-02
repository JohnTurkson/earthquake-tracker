package com.johnturkson.sandbox.ui

data class EarthquakeListPreviewState(
    override val mapItems: List<EarthquakeMapItem> = EarthquakeMapFixtures.items,
    override val selectedMapItemId: String? = null,
    override val monitoring: EarthquakeMonitoringState = EarthquakeMonitoringState(),
    override val scrollRequest: EarthquakeSelectionResult? = null,
) : EarthquakeListState
