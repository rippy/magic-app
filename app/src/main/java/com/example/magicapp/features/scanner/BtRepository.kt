package com.example.magicapp.features.scanner

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BleDevice(val name: String, val address: String, val rssi: Int)

data class BtState(
    val bleDevices: List<BleDevice> = emptyList(),
    val pairedDevices: List<String> = emptyList(),
    val lastScannedMs: Long? = null,
    val isScanning: Boolean = false,
    val hasPermission: Boolean = false,
    val locationServicesEnabled: Boolean = true
)

class BtRepository(private val context: android.content.Context) {
    private val _state = MutableStateFlow(BtState())
    val state: StateFlow<BtState> = _state.asStateFlow()
    fun scan() {}
    fun stopScan() {}
}
