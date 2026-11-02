package com.johnturkson.sandbox.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.johnturkson.sandbox.data.EarthquakeNotifications

@Composable
fun EarthquakeNotificationsScreen(
    navKey: Destination.EarthquakeNotificationsRoute,
    onClose: () -> Unit,
    expanded: Boolean = false,
    modifier: Modifier = Modifier,
    viewModel: EarthquakeNotificationsViewModel =
        hiltViewModel<EarthquakeNotificationsViewModel, EarthquakeNotificationsViewModel.Factory>(
            creationCallback = { factory -> factory.create(navKey) }
        ),
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    var notificationRationale by rememberSaveable { mutableStateOf(false) }
    var shouldExplainNotifications by remember { mutableStateOf(false) }
    fun updateNotificationPermission() {
        viewModel.refreshNotificationCapabilities()
        shouldExplainNotifications =
            Build.VERSION.SDK_INT >= 33 &&
                activity != null &&
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS,
                )
    }
    val notificationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            updateNotificationPermission()
        }
    LifecycleResumeEffect(Unit) {
        updateNotificationPermission()
        onPauseOrDispose {}
    }
    val notificationState = viewModel.notifications
    val notificationSettingsRequired =
        notificationState.permissionGranted && !notificationState.appEnabled ||
            notificationState.hasRequestedPermission && !shouldExplainNotifications
    fun launchNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            viewModel.recordNotificationPermissionRequest()
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    fun enableNotifications() {
        when {
            notificationSettingsRequired || Build.VERSION.SDK_INT < 33 ->
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                )
            shouldExplainNotifications -> notificationRationale = true
            else -> launchNotificationPermission()
        }
    }
    EarthquakeNotificationsScreen(
        state = notificationState.copy(requiresSettings = notificationSettingsRequired),
        monitoring = viewModel.monitoring,
        onClose = onClose,
        onEnableNotifications = ::enableNotifications,
        onAlertsEnabled = { enabled ->
            viewModel.setAlertsEnabled(enabled)
            if (enabled && (!notificationState.permissionGranted || !notificationState.appEnabled))
                enableNotifications()
        },
        onMagnitudeSelected = viewModel::setMinimumMagnitude,
        onMonitoringEnabled = viewModel::setMonitoring,
        onChannelSettings = { alerts ->
            context.startActivity(
                Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    .putExtra(
                        Settings.EXTRA_CHANNEL_ID,
                        if (alerts) EarthquakeNotifications.AlertsChannel
                        else EarthquakeNotifications.MonitoringChannel,
                    )
            )
        },
        onPreviewAlert = viewModel::previewNotification,
        modifier = modifier,
        expanded = expanded,
    )
    if (notificationRationale) {
        EarthquakeNotificationRationale(
            onContinue = {
                notificationRationale = false
                launchNotificationPermission()
            },
            onDismiss = { notificationRationale = false },
        )
    }
}

@Composable
fun EarthquakeNotificationsScreen(
    state: EarthquakeNotificationState,
    monitoring: EarthquakeMonitoringState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    onEnableNotifications: () -> Unit = {},
    onAlertsEnabled: (Boolean) -> Unit = {},
    onMagnitudeSelected: (Int) -> Unit = {},
    onMonitoringEnabled: (Boolean) -> Unit = {},
    onChannelSettings: (Boolean) -> Unit = {},
    onPreviewAlert: () -> Unit = {},
) {
    Box(
        modifier
            .fillMaxSize()
            .then(
                if (expanded) Modifier else Modifier.background(MaterialTheme.colorScheme.surface)
            )
    ) {
        EarthquakeNotificationPage(
            state = state,
            monitoring = monitoring,
            onClose = onClose,
            onEnableNotifications = onEnableNotifications,
            onAlertsEnabled = onAlertsEnabled,
            onMagnitudeSelected = onMagnitudeSelected,
            onMonitoringEnabled = onMonitoringEnabled,
            onChannelSettings = onChannelSettings,
            onPreviewAlert = onPreviewAlert,
            modifier =
                Modifier.fillMaxSize()
                    .padding(WindowInsets.safeDrawing.asPaddingValues())
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = if (expanded) 84.dp else 12.dp,
                        bottom = 16.dp,
                    ),
        )
    }
}
