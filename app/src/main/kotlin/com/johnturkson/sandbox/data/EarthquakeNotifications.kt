package com.johnturkson.sandbox.data

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import com.johnturkson.sandbox.R
import com.johnturkson.sandbox.app.EarthquakeActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EarthquakeNotifications
@Inject
constructor(@ApplicationContext private val context: Context) {
    data class Capabilities(
        val permissionGranted: Boolean,
        val appEnabled: Boolean,
        val monitoringEnabled: Boolean,
        val alertsEnabled: Boolean,
    )

    private val manager = context.getSystemService(NotificationManager::class.java)

    fun registerChannels() {
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                        MonitoringChannel,
                        context.getString(R.string.monitor_channel_name),
                        NotificationManager.IMPORTANCE_LOW,
                    )
                    .apply {
                        description = context.getString(R.string.monitor_channel_description)
                        setSound(null, null)
                    },
                NotificationChannel(
                        AlertsChannel,
                        context.getString(R.string.alert_channel_name),
                        NotificationManager.IMPORTANCE_HIGH,
                    )
                    .apply {
                        description = context.getString(R.string.alert_channel_description)
                        enableVibration(true)
                    },
            )
        )
    }

    fun capabilities(): Capabilities {
        val granted =
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
        return Capabilities(
            granted,
            NotificationManagerCompat.from(context).areNotificationsEnabled(),
            manager.getNotificationChannel(MonitoringChannel)?.let {
                it.importance != NotificationManager.IMPORTANCE_NONE
            } == true,
            manager.getNotificationChannel(AlertsChannel)?.let {
                it.importance != NotificationManager.IMPORTANCE_NONE
            } == true,
        )
    }

    fun foreground(
        workId: UUID,
        offline: Boolean = false,
    ): ForegroundInfo {
        val notification =
            NotificationCompat.Builder(context, MonitoringChannel)
                .setSmallIcon(R.drawable.ic_notification_earthquake)
                .setContentTitle(
                    context.getString(
                        if (offline) R.string.monitoring_offline
                        else R.string.monitor_notification_title
                    )
                )
                .setContentText(
                    context.getString(
                        if (offline) R.string.existing_data_visible
                        else R.string.monitor_notification_detail
                    )
                )
                .setContentIntent(openApp())
                .setOngoing(true)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .addAction(
                    0,
                    context.getString(R.string.stop_monitoring),
                    WorkManager.getInstance(context).createCancelPendingIntent(workId),
                )
                .build()
        return if (Build.VERSION.SDK_INT >= 29)
            ForegroundInfo(
                MonitoringId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        else ForegroundInfo(MonitoringId, notification)
    }

    fun postAlert(item: EarthquakeDatabaseModel): Boolean {
        val capability = capabilities()
        if (!capability.permissionGranted || !capability.appEnabled || !capability.alertsEnabled)
            return false
        val title =
            item.title?.takeIf { it.isNotBlank() }?.replaceFirst(Regex("^M\\s+"), "M")
                ?: context.getString(
                    R.string.alert_fallback_title,
                    item.magnitude.toString(),
                    item.place ?: context.getString(R.string.unknown_location),
                )
        val notification =
            NotificationCompat.Builder(context, AlertsChannel)
                .setSmallIcon(R.drawable.ic_notification_earthquake)
                .setContentTitle(title)
                .setContentText(context.getString(R.string.alerts_on))
                .setStyle(NotificationCompat.BigTextStyle().bigText(title))
                .setContentIntent(openApp(item.id))
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_EVENT)
                .setWhen(item.time.toEpochMilliseconds())
                .build()
        return post(item.id, AlertId, notification)
    }

    fun previewAlert(): Boolean {
        val capability = capabilities()
        if (!capability.permissionGranted || !capability.appEnabled || !capability.alertsEnabled)
            return false
        return post(
            "preview",
            PreviewId,
            NotificationCompat.Builder(context, AlertsChannel)
                .setSmallIcon(R.drawable.ic_notification_earthquake)
                .setContentTitle(context.getString(R.string.preview_alert_title))
                .setContentText(context.getString(R.string.preview_alert_detail))
                .setContentIntent(openApp())
                .setAutoCancel(true)
                .build(),
        )
    }

    private fun post(tag: String, id: Int, notification: Notification): Boolean =
        try {
            manager.notify(tag, id, notification)
            true
        } catch (_: SecurityException) {
            false
        }

    private fun openApp(eventId: String? = null): PendingIntent {
        val intent =
            Intent(context, EarthquakeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                data =
                    if (eventId == null) "usgstracker://monitoring".toUri()
                    else "usgstracker://earthquake/${android.net.Uri.encode(eventId)}".toUri()
                putExtra(EventIdExtra, eventId)
            }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val MonitoringChannel = "earthquake_monitoring"
        const val AlertsChannel = "earthquake_alerts"
        const val MonitoringId = 1001
        const val AlertId = 1002
        const val PreviewId = 1003
        const val EventIdExtra = "earthquake_id"
    }
}
