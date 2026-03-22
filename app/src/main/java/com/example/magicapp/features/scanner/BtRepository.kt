package com.example.magicapp.features.scanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.location.LocationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BleDevice(
    val name: String,
    val address: String,
    val rssi: Int
)

data class BtState(
    val bleDevices: List<BleDevice> = emptyList(),
    val pairedDevices: List<String> = emptyList(),
    val lastScannedMs: Long? = null,
    val isScanning: Boolean = false,
    val hasPermission: Boolean = false,
    val locationServicesEnabled: Boolean = true
)

class BtRepository(private val context: Context) {

    private val _state = MutableStateFlow(BtState())
    val state: StateFlow<BtState> = _state.asStateFlow()

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val discovered = mutableMapOf<String, BleDevice>()
    private val scope = CoroutineScope(Dispatchers.Default)
    private var autoStopJob: Job? = null

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = BleDevice(
                name = result.device.name ?: "Unknown",
                address = result.device.address,
                rssi = result.rssi
            )
            discovered[device.address] = device
            _state.value = _state.value.copy(bleDevices = discovered.values.toList())
        }
    }

    fun isLocationEnabled() =
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

    @SuppressLint("MissingPermission")
    fun scan() {
        if (!isLocationEnabled()) {
            _state.value = _state.value.copy(locationServicesEnabled = false)
            return
        }
        discovered.clear()
        _state.value = _state.value.copy(
            isScanning = true,
            hasPermission = true,
            locationServicesEnabled = true,
            bleDevices = emptyList()
        )
        val adapter = bluetoothManager.adapter ?: return
        val pairedNames = adapter.bondedDevices.mapNotNull { it.name }
        _state.value = _state.value.copy(pairedDevices = pairedNames)
        adapter.bluetoothLeScanner?.startScan(scanCallback)

        autoStopJob?.cancel()
        autoStopJob = scope.launch {
            delay(10_000L)
            stopScan()
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        autoStopJob?.cancel()
        bluetoothManager.adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        _state.value = _state.value.copy(
            isScanning = false,
            lastScannedMs = if (_state.value.isScanning) System.currentTimeMillis() else _state.value.lastScannedMs
        )
    }
}
