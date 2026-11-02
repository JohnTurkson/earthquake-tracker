package com.johnturkson.sandbox.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PauseCircleOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.johnturkson.sandbox.R
import java.util.Date

@Composable
fun EarthquakeMonitoringCard(
    state: EarthquakeMonitoringState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    lastUpdatedLabel: String? = null,
) {
    val offline =
        state.status == EarthquakeMonitoringState.Status.Offline ||
            state.refresh == EarthquakeMonitoringState.Refresh.Offline
    val failed =
        state.status == EarthquakeMonitoringState.Status.Failed ||
            state.refresh in
                setOf(
                    EarthquakeMonitoringState.Refresh.NetworkError,
                    EarthquakeMonitoringState.Refresh.DataError,
                    EarthquakeMonitoringState.Refresh.StorageError,
                )
    val title =
        when {
            state.status == EarthquakeMonitoringState.Status.Checking ->
                R.string.checking_monitoring
            state.refresh == EarthquakeMonitoringState.Refresh.Queued -> R.string.refresh_queued
            state.isRefreshing -> R.string.refreshing_earthquakes
            offline ->
                if (state.isMonitoring) R.string.monitoring_offline else R.string.refresh_offline
            state.status == EarthquakeMonitoringState.Status.Failed -> R.string.monitoring_failed
            failed -> R.string.refresh_failed
            state.status == EarthquakeMonitoringState.Status.Starting ->
                R.string.monitoring_starting
            state.isMonitoring -> R.string.live_monitoring
            else -> R.string.monitoring_off
        }
    val lastUpdated =
        lastUpdatedLabel
            ?: state.lastUpdated?.let {
                DateFormat.getTimeFormat(LocalContext.current)
                    .format(Date(it.toEpochMilliseconds()))
            }
    val detail =
        when {
            state.refresh == EarthquakeMonitoringState.Refresh.Queued ->
                stringResource(R.string.waiting_to_start)
            state.isRefreshing -> stringResource(R.string.existing_data_visible)
            offline || failed ->
                lastUpdated?.let { stringResource(R.string.saved_data_updated, it) }
                    ?: stringResource(
                        if (state.hasStoredData) R.string.saved_data_available
                        else R.string.no_saved_data
                    )
            state.status == EarthquakeMonitoringState.Status.Starting ->
                stringResource(R.string.waiting_to_start)
            state.isMonitoring ->
                lastUpdated?.let { stringResource(R.string.checks_every_minute_updated, it) }
                    ?: stringResource(R.string.checks_every_minute)
            else ->
                lastUpdated?.let { stringResource(R.string.data_updated, it) }
                    ?: stringResource(R.string.manual_refresh_available)
        }
    Surface(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(20.dp),
        color =
            if (offline || failed) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 2.dp,
    ) {
        Row(
            Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                when {
                    offline -> Icons.Outlined.WifiOff
                    failed -> Icons.Outlined.ErrorOutline
                    state.isMonitoring -> Icons.Outlined.Sensors
                    else -> Icons.Outlined.PauseCircleOutline
                },
                contentDescription = null,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
                Text(detail, style = MaterialTheme.typography.bodySmall)
            }
            val refreshDescription =
                stringResource(
                    when {
                        state.refresh == EarthquakeMonitoringState.Refresh.Queued ->
                            R.string.refresh_queued
                        state.isRefreshing -> R.string.refreshing_earthquakes
                        else -> R.string.refresh_earthquakes
                    }
                )
            IconButton(
                onClick = onRefresh,
                enabled = !state.isRefreshing,
                modifier = Modifier.semantics { contentDescription = refreshDescription },
            ) {
                if (state.isRefreshing)
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                else Icon(Icons.Outlined.Refresh, contentDescription = null)
            }
        }
    }
}
