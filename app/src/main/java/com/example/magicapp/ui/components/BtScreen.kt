package com.example.magicapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import com.example.magicapp.features.scanner.BtRepository

@Composable
fun BtScreen(repository: BtRepository, modifier: Modifier = Modifier) {
    val state by repository.state.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Bluetooth", style = MaterialTheme.typography.titleLarge)
            Button(onClick = { repository.scan() }, enabled = !state.isScanning) {
                Text(if (state.isScanning) "Scanning..." else "Scan")
            }
        }

        if (!state.locationServicesEnabled) {
            Text(
                "Enable Location Services to scan for BLE devices",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
        if (!state.hasPermission) {
            PermissionRequired("BLE scan", "Location")
            return@Column
        }

        state.lastScannedMs?.let { ts ->
            val secsAgo = (System.currentTimeMillis() - ts) / 1000
            Text(
                "Last scanned: ${secsAgo}s ago",
                style = MaterialTheme.typography.labelSmall
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "BLE Devices (${state.bleDevices.size})",
            style = MaterialTheme.typography.titleMedium
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(state.bleDevices.sortedByDescending { it.rssi }) { dev ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(dev.name, style = MaterialTheme.typography.bodyLarge)
                        Text("${dev.rssi} dBm", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        if (state.pairedDevices.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Paired Devices", style = MaterialTheme.typography.titleMedium)
            state.pairedDevices.forEach { name ->
                Text("• $name", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
