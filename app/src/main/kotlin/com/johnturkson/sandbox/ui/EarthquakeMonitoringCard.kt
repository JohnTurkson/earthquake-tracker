package com.johnturkson.sandbox.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PauseCircleOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.johnturkson.sandbox.R
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarthquakeMonitoringCard(
    state: EarthquakeMonitoringState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    lastUpdatedLabel: String? = null,
    floating: Boolean = true,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val offline = state.status == EarthquakeMonitoringState.Status.Offline
    val failed =
        state.status == EarthquakeMonitoringState.Status.Failed ||
            state.refresh == EarthquakeMonitoringState.Refresh.Failed
    val title =
        when {
            state.status == EarthquakeMonitoringState.Status.Checking ->
                R.string.checking_monitoring
            state.refresh == EarthquakeMonitoringState.Refresh.Queued -> R.string.refresh_queued
            state.isRefreshing -> R.string.refreshing_earthquakes
            state.status == EarthquakeMonitoringState.Status.Failed -> R.string.monitoring_failed
            failed -> R.string.refresh_failed
            offline -> R.string.monitoring_offline
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
    val shortTitle =
        stringResource(
            when {
                state.status == EarthquakeMonitoringState.Status.Checking ->
                    R.string.monitoring_bar_checking
                state.refresh == EarthquakeMonitoringState.Refresh.Queued -> R.string.refresh_queued
                state.isRefreshing -> R.string.monitoring_bar_refreshing
                state.status == EarthquakeMonitoringState.Status.Failed ->
                    R.string.monitoring_bar_failed
                failed -> R.string.monitoring_bar_refresh_failed
                offline -> R.string.monitoring_bar_offline
                state.status == EarthquakeMonitoringState.Status.Starting ->
                    R.string.monitoring_bar_starting
                state.isMonitoring -> R.string.live_monitoring
                else -> R.string.monitoring_bar_off
            }
        )
    val shortDetail =
        when {
            state.isRefreshing || state.status == EarthquakeMonitoringState.Status.Starting ->
                lastUpdated?.let { stringResource(R.string.monitoring_bar_updated, it) }
            offline || failed ->
                lastUpdated?.let { stringResource(R.string.monitoring_bar_saved, it) }
                    ?: stringResource(
                        if (state.hasStoredData) R.string.monitoring_bar_saved_data
                        else R.string.monitoring_bar_no_data
                    )
            lastUpdated != null -> stringResource(R.string.monitoring_bar_updated, lastUpdated)
            state.isMonitoring -> stringResource(R.string.checks_every_minute)
            else -> null
        }
    val titleLabel = stringResource(title)
    val fullLabel = stringResource(R.string.monitoring_bar_summary, titleLabel, detail)
    val color =
        if (offline || failed) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.surfaceContainer
    Surface(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(if (floating) 20.dp else 0.dp),
        color = color,
        shadowElevation = if (floating) 2.dp else 0.dp,
    ) {
        TopAppBar(
            title = {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        when {
                            failed -> Icons.Outlined.ErrorOutline
                            offline -> Icons.Outlined.WifiOff
                            state.isMonitoring -> Icons.Outlined.Sensors
                            else -> Icons.Outlined.PauseCircleOutline
                        },
                        contentDescription = null,
                    )
                    BoxWithConstraints(Modifier.weight(1f)) {
                        val textMeasurer = rememberTextMeasurer()
                        val style = MaterialTheme.typography.titleSmall
                        val candidates =
                            listOfNotNull(
                                fullLabel,
                                shortDetail?.let {
                                    stringResource(R.string.monitoring_bar_summary, shortTitle, it)
                                },
                                shortTitle,
                            )
                        val label =
                            candidates.firstOrNull {
                                textMeasurer
                                    .measure(AnnotatedString(it), style, maxLines = 1)
                                    .size
                                    .width <= constraints.maxWidth
                            } ?: shortTitle
                        Text(
                            label,
                            Modifier.clearAndSetSemantics { contentDescription = fullLabel },
                            style = style,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            },
            navigationIcon = navigationIcon,
            actions = {
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
                actions()
            },
            expandedHeight = earthquakeTopAppBarHeight,
            windowInsets = WindowInsets(0, 0, 0, 0),
            colors = TopAppBarDefaults.topAppBarColors(containerColor = color),
        )
    }
}
