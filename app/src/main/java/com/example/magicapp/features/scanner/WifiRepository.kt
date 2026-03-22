package com.example.magicapp.features.scanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WifiNetwork(
    val ssid: String,
    val rssi: Int,
    val security: String
)

data class WifiState(
    val networks: List<WifiNetwork> = emptyList(),
    val lastScannedMs: Long? = null,
    val isFromCache: Boolean = false,
    val isScanning: Boolean = false,
    val hasPermission: Boolean = false
)

class WifiRepository(private val context: Context) {

    private val _state = MutableStateFlow(WifiState())
    val state: StateFlow<WifiState> = _state.asStateFlow()

    private val wifiManager = context.applicationContext
        .getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            val results = wifiManager.scanResults
            _state.value = _state.value.copy(
                networks = results.map { it.toWifiNetwork() },
                lastScannedMs = System.currentTimeMillis(),
                isFromCache = !success,
                isScanning = false,
                hasPermission = true
            )
        }
    }

    fun register() {
        context.registerReceiver(
            receiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        )
    }

    fun unregister() {
        try {
            context.unregisterReceiver(receiver)
        } catch (_: Exception) {}
    }

    fun scan() {
        _state.value = _state.value.copy(isScanning = true, hasPermission = true)
        wifiManager.startScan()
    }

    private fun ScanResult.toWifiNetwork() = WifiNetwork(
        ssid = SSID.removePrefix("\"").removeSuffix("\"").ifBlank { "<hidden>" },
        rssi = level,
        security = when {
            capabilities.contains("WPA3") -> "WPA3"
            capabilities.contains("WPA2") -> "WPA2"
            capabilities.contains("WPA") -> "WPA"
            capabilities.contains("WEP") -> "WEP"
            else -> "Open"
        }
    )
}
