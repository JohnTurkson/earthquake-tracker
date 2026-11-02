package com.johnturkson.sandbox.ui

import com.johnturkson.sandbox.data.EarthquakeLocation

data class EarthquakePreviewState(
    override val monitoring: EarthquakeMonitoringState = EarthquakeMonitoringState(),
    override val notifications: EarthquakeNotificationState =
        EarthquakeNotificationState(isChecking = false),
    override val notificationsVisible: Boolean = false,
    override val requestedMapFocusId: String? = null,
    override val unavailableNotificationEvent: Boolean = false,
    override val navKey: Destination.EarthquakeRoute = Destination.EarthquakeRoute,
    override val mapItems: List<EarthquakeMapItem> = EarthquakeMapFixtures.items,
    override val selectedMapItemId: String? = null,
    override val selectedClusterItemIds: List<String> = emptyList(),
    override val isListPanelVisible: Boolean = true,
    override val selectedView: EarthquakeView = EarthquakeView.Map,
    override val initialMapLocation: EarthquakeLocation? = EarthquakeLocation.Default,
    override val userLocation: EarthquakeLocation? = null,
) : EarthquakeState
