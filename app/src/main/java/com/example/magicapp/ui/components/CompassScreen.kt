package com.example.magicapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.magicapp.features.compass.CompassRepository
import kotlin.math.min

@Composable
fun CompassScreen(repository: CompassRepository, modifier: Modifier = Modifier) {
    val state by repository.state.collectAsState()
    val primary = MaterialTheme.colorScheme.primary
    val onBg = MaterialTheme.colorScheme.onBackground

    if (!state.hasHardware) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No magnetometer sensor found",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Canvas(modifier = Modifier.size(200.dp)) {
            val r = min(this.size.width, this.size.height) / 2f
            drawCircle(color = onBg.copy(alpha = 0.1f), radius = r)
            rotate(degrees = -state.azimuthDeg) {
                drawLine(
                    color = primary,
                    start = Offset(this.size.width / 2f, this.size.height / 2f),
                    end = Offset(this.size.width / 2f, this.size.height / 2f - r * 0.8f),
                    strokeWidth = 6f
                )
                drawLine(
                    color = Color.Red,
                    start = Offset(this.size.width / 2f, this.size.height / 2f),
                    end = Offset(this.size.width / 2f, this.size.height / 2f + r * 0.4f),
                    strokeWidth = 4f
                )
            }
        }
        Text(
            "%.1f° %s".format(state.azimuthDeg, state.cardinalDirection),
            style = MaterialTheme.typography.displaySmall
        )
    }
}
