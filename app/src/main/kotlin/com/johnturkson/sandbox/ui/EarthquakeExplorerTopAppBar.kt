package com.johnturkson.sandbox.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.johnturkson.sandbox.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarthquakeExplorerTopAppBar(
    monitoring: EarthquakeMonitoringState?,
    isListPanelVisible: Boolean,
    onListPanelVisibilityChanged: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier,
    lastUpdatedLabel: String? = null,
) {
    val navigationIcon: @Composable () -> Unit = {
        if (isListPanelVisible) {
            IconButton(onClick = { onListPanelVisibilityChanged(false) }) {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    stringResource(R.string.minimize_earthquake_list),
                )
            }
        } else {
            TextButton(onClick = { onListPanelVisibilityChanged(true) }) {
                Icon(Icons.AutoMirrored.Outlined.List, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.show_earthquake_list), maxLines = 1)
            }
        }
    }
    val actions: @Composable RowScope.() -> Unit = {
        IconButton(onNotificationsClick) {
            Icon(Icons.Outlined.Notifications, stringResource(R.string.earthquake_alerts))
        }
    }
    if (monitoring != null) {
        EarthquakeMonitoringCard(
            state = monitoring,
            onRefresh = onRefresh,
            modifier = modifier,
            lastUpdatedLabel = lastUpdatedLabel,
            floating = false,
            navigationIcon = navigationIcon,
            actions = actions,
        )
    } else {
        TopAppBar(
            title = { Text(stringResource(R.string.recent_earthquakes), maxLines = 1) },
            modifier = modifier,
            navigationIcon = navigationIcon,
            actions = actions,
            windowInsets = WindowInsets(0, 0, 0, 0),
        )
    }
}
