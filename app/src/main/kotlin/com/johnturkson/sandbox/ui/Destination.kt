package com.johnturkson.sandbox.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Destination : NavKey {
    @Serializable data object MapRoute : Destination

    @Serializable data object ListRoute : Destination

    @Serializable data object NotificationsRoute : Destination
}
