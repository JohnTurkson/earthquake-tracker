package com.johnturkson.sandbox.ui

import android.content.Context
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.CachePolicy
import io.ktor.client.HttpClient

@OptIn(ExperimentalCoilApi::class)
fun earthquakeReportImageLoader(context: Context, client: HttpClient): ImageLoader =
    ImageLoader.Builder(context)
        .components { add(KtorNetworkFetcherFactory(httpClient = client)) }
        .memoryCache(null)
        .diskCache(null)
        .memoryCachePolicy(CachePolicy.DISABLED)
        .diskCachePolicy(CachePolicy.DISABLED)
        .build()
