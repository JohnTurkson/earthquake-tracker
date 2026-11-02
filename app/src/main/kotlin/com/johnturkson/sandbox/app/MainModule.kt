package com.johnturkson.sandbox.app

import android.content.Context
import androidx.room.Room
import com.johnturkson.sandbox.annotations.DefaultDispatcher
import com.johnturkson.sandbox.annotations.IoDispatcher
import com.johnturkson.sandbox.annotations.MainDispatcher
import com.johnturkson.sandbox.data.ItemDao
import com.johnturkson.sandbox.data.MainDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.resources.Resources
import io.ktor.serialization.kotlinx.json.json
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
object MainModule {
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
            install(ContentNegotiation) { json(json) }
            install(Logging) { level = LogLevel.ALL }
            install(Resources)
        }
    }

    @Provides
    @Singleton
    fun provideMainDatabase(@ApplicationContext context: Context): MainDatabase {
        return Room.databaseBuilder(context, MainDatabase::class.java, "MainDatabase").build()
    }

    @Provides
    @Singleton
    fun provideItemDao(database: MainDatabase): ItemDao {
        return database.itemDao()
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
