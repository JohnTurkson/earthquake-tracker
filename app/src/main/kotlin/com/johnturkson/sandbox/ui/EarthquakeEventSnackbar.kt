package com.johnturkson.sandbox.ui

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun EarthquakeEventSnackbar(data: SnackbarData, modifier: Modifier = Modifier) {
    Snackbar(snackbarData = data, modifier = modifier)
}
