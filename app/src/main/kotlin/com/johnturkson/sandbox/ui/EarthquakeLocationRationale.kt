package com.johnturkson.sandbox.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.johnturkson.sandbox.R

@Composable
fun EarthquakeLocationRationale(onContinue: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.location_permission_title)) },
        text = { Text(stringResource(R.string.location_permission_explanation)) },
        confirmButton = {
            TextButton(onClick = onContinue) { Text(stringResource(R.string.continue_action)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.not_now)) }
        },
    )
}
