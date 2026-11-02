package com.johnturkson.sandbox.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.johnturkson.sandbox.R

@Composable
fun EarthquakeNotificationPage(
    state: EarthquakeNotificationState,
    monitoring: EarthquakeMonitoringState,
    onClose: () -> Unit,
    onEnableNotifications: () -> Unit,
    onAlertsEnabled: (Boolean) -> Unit,
    onMagnitudeSelected: (Int) -> Unit,
    onMonitoringEnabled: (Boolean) -> Unit,
    onChannelSettings: (Boolean) -> Unit,
    onPreviewAlert: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    Surface(
        modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "title") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.earthquake_alerts),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Outlined.Close, stringResource(R.string.close_earthquake_alerts))
                    }
                }
            }
            item(key = "permission") {
                when {
                    state.isChecking ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator()
                            Text(stringResource(R.string.checking_notifications))
                        }
                    !state.permissionGranted || !state.appEnabled ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Column(
                                Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Outlined.NotificationsOff, contentDescription = null)
                                Text(
                                    stringResource(R.string.notifications_disabled),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    stringResource(R.string.notification_permission_explanation),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Button(onClick = onEnableNotifications) {
                                    Text(
                                        stringResource(
                                            if (state.requiresSettings)
                                                R.string.open_notification_settings
                                            else R.string.enable_notifications
                                        )
                                    )
                                }
                            }
                        }
                }
            }
            item(key = "alerts") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            stringResource(R.string.worldwide_alerts),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            stringResource(
                                when {
                                    !state.alertsEnabled -> R.string.alerts_off
                                    !state.permissionGranted || !state.appEnabled ->
                                        R.string.waiting_notification_permission
                                    !state.alertsChannelEnabled -> R.string.alert_channel_disabled
                                    !monitoring.isMonitoring -> R.string.alerts_manual_only
                                    else -> R.string.alerts_on
                                }
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(
                        checked = state.alertsEnabled,
                        onCheckedChange = onAlertsEnabled,
                        enabled = !state.isChecking,
                        modifier = Modifier.notificationToggleSemantics(R.string.worldwide_alerts),
                    )
                }
            }
            item(key = "magnitude") {
                Column {
                    Text(
                        stringResource(R.string.minimum_magnitude),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 6, 7).forEach { magnitude ->
                            FilterChip(
                                selected = state.minimumMagnitude == magnitude,
                                enabled = !state.isChecking,
                                onClick = { onMagnitudeSelected(magnitude) },
                                label = {
                                    Text(stringResource(R.string.magnitude_threshold, magnitude))
                                },
                            )
                        }
                    }
                }
            }
            item(key = "monitoring") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                stringResource(R.string.live_monitoring),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                stringResource(R.string.monitoring_session_explanation),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(
                            checked = monitoring.isMonitoring,
                            onCheckedChange = onMonitoringEnabled,
                            enabled =
                                monitoring.status != EarthquakeMonitoringState.Status.Checking,
                            modifier =
                                Modifier.notificationToggleSemantics(R.string.live_monitoring),
                        )
                    }
                    Text(
                        stringResource(
                            when (monitoring.status) {
                                EarthquakeMonitoringState.Status.Checking ->
                                    R.string.checking_monitoring
                                EarthquakeMonitoringState.Status.Starting ->
                                    R.string.monitoring_starting
                                EarthquakeMonitoringState.Status.Offline ->
                                    R.string.monitoring_offline
                                EarthquakeMonitoringState.Status.Failed ->
                                    R.string.monitoring_failed
                                EarthquakeMonitoringState.Status.Running ->
                                    R.string.monitoring_active
                                EarthquakeMonitoringState.Status.Stopped ->
                                    R.string.monitoring_stopped
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (
                        !state.monitoringChannelEnabled &&
                            state.permissionGranted &&
                            state.appEnabled
                    )
                        Text(
                            stringResource(R.string.monitor_channel_disabled),
                            style = MaterialTheme.typography.bodySmall,
                        )
                }
            }
            item(key = "channels") {
                Column {
                    TextButton(
                        onClick = { onChannelSettings(false) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.monitoring_channel_settings))
                    }
                    TextButton(
                        onClick = { onChannelSettings(true) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.alert_channel_settings))
                    }
                    OutlinedButton(
                        onClick = onPreviewAlert,
                        enabled = state.canPostAlerts,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.preview_alert))
                    }
                    if (state.settingsError)
                        Text(
                            stringResource(R.string.notification_settings_error),
                            color = MaterialTheme.colorScheme.error,
                        )
                }
            }
        }
    }
}

@Composable
fun EarthquakeNotificationRationale(onContinue: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.enable_notifications)) },
        text = { Text(stringResource(R.string.notification_permission_explanation)) },
        confirmButton = {
            TextButton(onClick = onContinue) { Text(stringResource(R.string.continue_action)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.not_now)) }
        },
    )
}

@Composable
private fun Modifier.notificationToggleSemantics(label: Int): Modifier {
    val description = stringResource(label)
    return semantics { contentDescription = description }
}
