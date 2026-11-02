package com.johnturkson.sandbox.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.johnturkson.sandbox.data.notifications.EarthquakeNotifications
import com.johnturkson.sandbox.ui.navigation.EarthquakeApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EarthquakeActivity : ComponentActivity() {
    private var notificationEventId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            notificationEventId = intent.getStringExtra(EarthquakeNotifications.EventIdExtra)
        }
        enableEdgeToEdge()
        setContent {
            EarthquakeApp(
                notificationEventId = notificationEventId,
                onNotificationEventConsumed = { notificationEventId = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationEventId = intent.getStringExtra(EarthquakeNotifications.EventIdExtra)
    }
}
