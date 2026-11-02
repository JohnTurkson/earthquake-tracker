package com.johnturkson.sandbox.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.work.WorkManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.johnturkson.sandbox.data.database.EarthquakeDao
import com.johnturkson.sandbox.data.database.EarthquakeDatabase
import com.johnturkson.sandbox.data.database.EarthquakeMonitoringDao
import com.johnturkson.sandbox.data.network.EarthquakeConnection
import com.johnturkson.sandbox.data.network.EarthquakeConnectivity
import com.johnturkson.sandbox.data.repository.EarthquakeRefreshRepository
import com.johnturkson.sandbox.data.repository.EarthquakeRefresher
import com.johnturkson.sandbox.di.qualifiers.DefaultDispatcher
import com.johnturkson.sandbox.di.qualifiers.IoDispatcher
import com.johnturkson.sandbox.di.qualifiers.MainDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.resources.Resources
import io.ktor.serialization.kotlinx.json.json
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
object EarthquakeModule {
    @Provides
    fun provideConnection(connection: EarthquakeConnectivity): EarthquakeConnection = connection

    @Provides
    fun provideRefresher(repository: EarthquakeRefreshRepository): EarthquakeRefresher = repository

    @Provides
    fun provideMonitoringDao(database: EarthquakeDatabase): EarthquakeMonitoringDao =
        database.monitoringDao()

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    @Provides
    @Singleton
    fun provideEarthquakesDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            scope = CoroutineScope(ioDispatcher + SupervisorJob()),
            produceFile = { context.preferencesDataStoreFile("earthquakes") },
        )
    }

    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }
    }

    @Provides
    @Singleton
    fun provideHttpClient(json: Json): HttpClient {
        return HttpClient(CIO) {
            defaultRequest { url("https://earthquake.usgs.gov/") }
            install(ContentNegotiation) { json(json) }
            install(Logging) { level = LogLevel.ALL }
            install(Resources)
        }
    }

    @Provides
    @Singleton
    fun provideEarthquakeDatabase(@ApplicationContext context: Context): EarthquakeDatabase {
        return Room.databaseBuilder(context, EarthquakeDatabase::class.java, "EarthquakeDatabase")
            .build()
    }

    @Provides
    @Singleton
    fun provideEarthquakeDao(database: EarthquakeDatabase): EarthquakeDao {
        return database.earthquakeDao()
    }

    @Provides
    @Singleton
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher {
        return Dispatchers.Main.immediate
    }

    @Provides
    @Singleton
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher {
        return Dispatchers.Default
    }

    @Provides
    @Singleton
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher {
        return Dispatchers.IO
    }
}
