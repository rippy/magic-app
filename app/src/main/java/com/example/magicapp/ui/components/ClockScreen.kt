package com.example.magicapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.magicapp.features.clock.ClockRepository

@Composable
fun ClockScreen(repository: ClockRepository, modifier: Modifier = Modifier) {
    val state by repository.state.collectAsState()
    val h = state.tripElapsedSec / 3600
    val m = (state.tripElapsedSec % 3600) / 60
    val s = state.tripElapsedSec % 60

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
    ) {
        Text(
            state.timeString,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text("Trip Timer", style = MaterialTheme.typography.titleMedium)
        Text(
            "%02d:%02d:%02d".format(h, m, s),
            style = MaterialTheme.typography.displaySmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!state.tripRunning) {
                Button(onClick = { repository.startTrip() }) { Text("Start") }
            } else {
                Button(onClick = { repository.stopTrip() }) { Text("Stop") }
            }
            OutlinedButton(onClick = { repository.resetTrip() }) { Text("Reset") }
        }
    }
}
