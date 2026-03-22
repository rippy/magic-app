package com.example.magicapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.example.magicapp.features.mic.MicRepository

@Composable
fun MicScreen(repository: MicRepository, modifier: Modifier = Modifier) {
    val state by repository.state.collectAsState()
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
    ) {
        Text("Microphone", style = MaterialTheme.typography.titleLarge)

        val level = ((state.amplitudeDb + 60f) / 60f).coerceIn(0f, 1f)
        Canvas(modifier = Modifier.fillMaxWidth().height(32.dp)) {
            drawRect(color = surface, size = Size(this.size.width, this.size.height))
            drawRect(color = primary, size = Size(this.size.width * level, this.size.height))
        }
        Text("%.0f dB".format(state.amplitudeDb), style = MaterialTheme.typography.bodyLarge)

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = {
                if (state.isRecording) repository.stopRecording()
                else repository.startRecording()
            }) {
                Text(if (state.isRecording) "Stop" else "Record")
            }
        }

        if (!state.hasPermission) {
            PermissionRequired("Microphone", "Record Audio")
        }
    }
}
