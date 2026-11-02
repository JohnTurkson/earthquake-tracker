package com.johnturkson.sandbox.ui.map.location

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.johnturkson.sandbox.R
import com.johnturkson.sandbox.ui.layout.EarthquakePermissionRationale

@Composable
fun EarthquakeLocationRationale(onContinue: () -> Unit, onDismiss: () -> Unit) {
    EarthquakePermissionRationale(
        title = stringResource(R.string.location_permission_title),
        explanation = stringResource(R.string.location_permission_explanation),
        onContinue = onContinue,
        onDismiss = onDismiss,
    )
}
