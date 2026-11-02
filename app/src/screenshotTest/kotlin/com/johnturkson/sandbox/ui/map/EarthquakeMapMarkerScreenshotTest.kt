package com.johnturkson.sandbox.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest

class EarthquakeMapMarkerScreenshotTest {
    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFFE7E9E3)
    @Composable
    fun UnselectedMarker() {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            EarthquakeMapMarker(isSelected = false)
        }
    }

    @PreviewTest
    @Preview(showBackground = true, backgroundColor = 0xFFE7E9E3)
    @Composable
    fun SelectedMarker() {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            EarthquakeMapMarker(isSelected = true)
        }
    }
}
