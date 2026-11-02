package com.johnturkson.sandbox.ui.details.reports

import android.content.Context
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.network.ktor3.KtorNetworkFetcherFactory
import io.ktor.client.HttpClient

@OptIn(ExperimentalCoilApi::class)
fun earthquakeReportImageLoader(context: Context, client: HttpClient): ImageLoader =
    ImageLoader.Builder(context)
        .components { add(KtorNetworkFetcherFactory(httpClient = client)) }
        .build()
