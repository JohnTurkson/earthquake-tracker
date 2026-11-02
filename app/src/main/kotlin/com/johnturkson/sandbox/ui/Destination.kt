package com.johnturkson.sandbox.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Destination : NavKey {
    @Serializable data object MainRoute : Destination
}
