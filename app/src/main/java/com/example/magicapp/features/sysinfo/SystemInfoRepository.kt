package com.example.magicapp.features.sysinfo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SystemInfoState(
    val cpuPercent: Int = 0,
    val ramUsedMb: Long = 0,
    val ramTotalMb: Long = 0,
    val storageUsedGb: Float = 0f,
    val storageTotalGb: Float = 0f,
    val batteryPercent: Int = 0,
    val isCharging: Boolean = false,
    val networkType: String = "Unknown"
)

class SystemInfoRepository(private val context: android.content.Context) {
    private val _state = MutableStateFlow(SystemInfoState())
    val state: StateFlow<SystemInfoState> = _state.asStateFlow()
    fun start() {}
    fun stop() {}
}
