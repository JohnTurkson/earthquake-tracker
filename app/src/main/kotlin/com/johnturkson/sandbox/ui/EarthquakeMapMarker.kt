package com.johnturkson.sandbox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun EarthquakeMapMarker(isSelected: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .size(if (isSelected) 24.dp else 16.dp)
                .background(if (isSelected) Color(0xFF2563EB) else Color(0xFFDC2626), CircleShape)
                .border(2.dp, Color.White, CircleShape)
    )
}
