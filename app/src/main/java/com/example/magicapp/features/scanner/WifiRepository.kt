package com.example.magicapp.features.scanner

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WifiNetwork(val ssid: String, val rssi: Int, val security: String)

data class WifiState(
    val networks: List<WifiNetwork> = emptyList(),
    val lastScannedMs: Long? = null,
    val isFromCache: Boolean = false,
    val isScanning: Boolean = false,
    val hasPermission: Boolean = false
)

class WifiRepository(private val context: android.content.Context) {
    private val _state = MutableStateFlow(WifiState())
    val state: StateFlow<WifiState> = _state.asStateFlow()
    fun register() {}
    fun unregister() {}
    fun scan() {}
}
