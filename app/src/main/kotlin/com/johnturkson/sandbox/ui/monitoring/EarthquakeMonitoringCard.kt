package com.johnturkson.sandbox.ui.monitoring

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
import androidx.compose.material.icons.outlined.Refresh
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.johnturkson.sandbox.R
import com.johnturkson.sandbox.ui.layout.earthquakeTopAppBarHeight
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
    val failed = state.refresh == EarthquakeMonitoringState.Refresh.Failed
    val title =
        when (state.refresh) {
            EarthquakeMonitoringState.Refresh.Idle -> R.string.refresh_idle
            EarthquakeMonitoringState.Refresh.Queued -> R.string.refresh_queued
            EarthquakeMonitoringState.Refresh.Running -> R.string.refreshing_earthquakes
            EarthquakeMonitoringState.Refresh.Succeeded -> R.string.refresh_succeeded
            EarthquakeMonitoringState.Refresh.Failed -> R.string.refresh_failed
            EarthquakeMonitoringState.Refresh.Cancelled -> R.string.refresh_cancelled
        }
    val lastUpdated =
        lastUpdatedLabel
            ?: state.lastUpdated?.let {
                DateFormat.getTimeFormat(LocalContext.current)
                    .format(Date(it.toEpochMilliseconds()))
            }
    val shortTitle =
        stringResource(
            when (state.refresh) {
                EarthquakeMonitoringState.Refresh.Idle -> R.string.refresh_idle
                EarthquakeMonitoringState.Refresh.Queued -> R.string.refresh_queued
                EarthquakeMonitoringState.Refresh.Running -> R.string.monitoring_bar_refreshing
                EarthquakeMonitoringState.Refresh.Succeeded ->
                    R.string.monitoring_bar_refresh_succeeded
                EarthquakeMonitoringState.Refresh.Failed -> R.string.monitoring_bar_refresh_failed
                EarthquakeMonitoringState.Refresh.Cancelled -> R.string.refresh_cancelled
            }
        )
    val titleLabel = stringResource(title)
    val updatedLabel = lastUpdated?.let { stringResource(R.string.monitoring_bar_updated, it) }
    val fullLabel =
        updatedLabel?.let { stringResource(R.string.monitoring_bar_summary, titleLabel, it) }
            ?: titleLabel
    val color =
        if (failed) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
    Surface(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        shape =
            RoundedCornerShape(
                if (floating) {
                    20.dp
                } else {
                    0.dp
                }
            ),
        color = color,
        shadowElevation =
            if (floating) {
                2.dp
            } else {
                0.dp
            },
    ) {
        TopAppBar(
            title = {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (failed) {
                            Icons.Outlined.ErrorOutline
                        } else {
                            Icons.Outlined.Refresh
                        },
                        contentDescription = null,
                    )
                    BoxWithConstraints(Modifier.weight(1f)) {
                        val textMeasurer = rememberTextMeasurer()
                        val titleStyle = MaterialTheme.typography.titleSmall
                        val updatedStyle =
                            MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal)
                        val spacing = with(LocalDensity.current) { 8.dp.roundToPx() }
                        val updatedWidth =
                            updatedLabel?.let {
                                textMeasurer
                                    .measure(AnnotatedString(it), updatedStyle, maxLines = 1)
                                    .size
                                    .width
                            } ?: 0
                        val titles = listOf(titleLabel, shortTitle)
                        val titleWithUpdated =
                            if (updatedLabel != null) {
                                titles.firstOrNull {
                                    textMeasurer
                                        .measure(AnnotatedString(it), titleStyle, maxLines = 1)
                                        .size
                                        .width + spacing + updatedWidth <= constraints.maxWidth
                                }
                            } else {
                                null
                            }
                        val visibleTitle =
                            titleWithUpdated
                                ?: titles.firstOrNull {
                                    textMeasurer
                                        .measure(AnnotatedString(it), titleStyle, maxLines = 1)
                                        .size
                                        .width <= constraints.maxWidth
                                }
                                ?: shortTitle
                        Row(
                            Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
                                contentDescription = fullLabel
                            },
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                visibleTitle,
                                Modifier.weight(1f, fill = false),
                                style = titleStyle,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (titleWithUpdated != null && updatedLabel != null) {
                                Text(
                                    updatedLabel,
                                    style = updatedStyle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
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
                    if (state.isRefreshing) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.Refresh, contentDescription = null)
                    }
                }
                actions()
            },
            expandedHeight = earthquakeTopAppBarHeight,
            windowInsets = WindowInsets(0, 0, 0, 0),
            colors = TopAppBarDefaults.topAppBarColors(containerColor = color),
        )
    }
}
