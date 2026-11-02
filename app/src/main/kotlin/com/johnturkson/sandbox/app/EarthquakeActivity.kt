package com.johnturkson.sandbox.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.johnturkson.sandbox.ui.EarthquakeApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EarthquakeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { EarthquakeApp() }
    }
}
