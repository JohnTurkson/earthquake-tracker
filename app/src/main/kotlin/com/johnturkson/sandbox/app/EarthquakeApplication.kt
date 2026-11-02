package com.johnturkson.sandbox.app

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.johnturkson.sandbox.data.notifications.EarthquakeNotifications
import com.johnturkson.sandbox.ui.details.reports.earthquakeReportImageLoader
import dagger.hilt.android.HiltAndroidApp
import io.ktor.client.HttpClient
import javax.inject.Inject

@HiltAndroidApp
class EarthquakeApplication : Application(), Configuration.Provider, SingletonImageLoader.Factory {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notifications: EarthquakeNotifications
    @Inject lateinit var client: HttpClient

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun newImageLoader(context: Context): ImageLoader =
        earthquakeReportImageLoader(context, client)

    override fun onCreate() {
        super.onCreate()
        notifications.registerChannels()
    }
}
