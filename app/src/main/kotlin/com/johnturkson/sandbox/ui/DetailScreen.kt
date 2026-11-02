package com.johnturkson.sandbox.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.result.LocalResultEventBus

@Composable
fun DetailScreen(
    navKey: Destination.DetailRoute,
    modifier: Modifier = Modifier,
) {
    val navigationResults = LocalResultEventBus.current
    Scaffold(
        modifier = modifier,
        bottomBar = {
            BottomAppBar {
                Button(
                    onClick = {
                        navigationResults.sendResult("saveTimer", Unit)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Save")
                }
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // TODO content
            // TODO default state
        }
    }
}
