package com.johnturkson.sandbox.data

import kotlinx.serialization.Serializable

@Serializable
data class ItemNetworkModel(override val id: String) : Item
