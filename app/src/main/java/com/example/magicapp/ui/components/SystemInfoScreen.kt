package com.example.magicapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.magicapp.features.sysinfo.SystemInfoRepository

@Composable
fun SystemInfoScreen(repository: SystemInfoRepository, modifier: Modifier = Modifier) {
    val state by repository.state.collectAsState()
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("System Info", style = MaterialTheme.typography.titleLarge)
        InfoRow("CPU", "${state.cpuPercent}%")
        InfoRow("RAM", "${state.ramUsedMb} / ${state.ramTotalMb} MB")
        InfoRow("Storage", "%.1f / %.1f GB".format(state.storageUsedGb, state.storageTotalGb))
        InfoRow("Battery", "${state.batteryPercent}%${if (state.isCharging) " (charging)" else ""}")
        InfoRow("Network", state.networkType)
    }
}
