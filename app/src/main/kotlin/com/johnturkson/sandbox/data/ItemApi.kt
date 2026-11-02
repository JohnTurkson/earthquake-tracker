package com.johnturkson.sandbox.data

import io.ktor.client.HttpClient
import javax.inject.Inject

class ItemApi @Inject constructor(private val client: HttpClient)
