package com.johnturkson.sandbox.ui.map

import com.johnturkson.sandbox.data.location.EarthquakeLocation
import com.johnturkson.sandbox.ui.event.EarthquakeMapItem
import com.johnturkson.sandbox.ui.monitoring.EarthquakeMonitoringState
import com.johnturkson.sandbox.ui.navigation.Destination

data class EarthquakePreviewState(
    override val monitoring: EarthquakeMonitoringState = EarthquakeMonitoringState(),
    override val requestedMapFocusId: String? = null,
    override val unavailableNotificationEvent: Boolean = false,
    override val navKey: Destination.EarthquakeMapRoute = Destination.EarthquakeMapRoute,
    override val mapItems: List<EarthquakeMapItem> = EarthquakeMapFixtures.items,
    override val selectedMapItemId: String? = null,
    override val selectedClusterItemIds: List<String> = emptyList(),
    override val initialMapLocation: EarthquakeLocation? = EarthquakeLocation.Default,
    override val userLocation: EarthquakeLocation? = null,
) : EarthquakeState
