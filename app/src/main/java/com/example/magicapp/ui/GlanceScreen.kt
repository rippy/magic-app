package com.example.magicapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.magicapp.AppViewModel
import com.example.magicapp.DiagnosticViewModel
import com.example.magicapp.Feature

@Composable
fun GlanceScreen(
    viewModel: AppViewModel,
    diagnosticViewModel: DiagnosticViewModel,
    modifier: Modifier = Modifier
) {
    val wifiState by diagnosticViewModel.wifi.state.collectAsState()
    val btState by diagnosticViewModel.bt.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top row: hero card (weight 3) + WiFi/BT scan tiles stacked (weight 1)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HeroCard(
                viewModel = viewModel,
                diagnosticViewModel = diagnosticViewModel,
                modifier = Modifier
                    .weight(3f)
                    .fillMaxHeight()
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScanTile(
                    label = "WiFi",
                    count = wifiState.networks.size,
                    lastScannedMs = wifiState.lastScannedMs,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
                ScanTile(
                    label = "BT",
                    count = btState.bleDevices.size,
                    lastScannedMs = btState.lastScannedMs,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        }

        // Bottom quick-launch tiles
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Feature.glanceTiles.forEach { feature ->
                QuickTile(
                    label = feature.label,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
fun HeroCard(
    viewModel: AppViewModel,
    diagnosticViewModel: DiagnosticViewModel,
    modifier: Modifier = Modifier
) {
    val gpsState by diagnosticViewModel.gps.state.collectAsState()

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            ) {
                Text(
                    "%.0f".format(gpsState.speedKph),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "km/h",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                androidx.compose.foundation.layout.Spacer(
                    modifier = Modifier.height(8.dp)
                )
                Text(
                    "%.4f, %.4f".format(gpsState.latitude, gpsState.longitude),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "%.1f°  Alt %.0fm".format(gpsState.bearing, gpsState.altitude),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            ControlsOverlay(
                viewModel = viewModel,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            )
        }
    }
}

@Composable
fun ScanTile(
    label: String,
    count: Int,
    lastScannedMs: Long?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    "$count",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                if (lastScannedMs != null) {
                    val secs = (System.currentTimeMillis() - lastScannedMs) / 1000
                    Text(
                        "Last scanned: ${secs}s ago",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                } else {
                    Text(
                        "—",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickTile(label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
