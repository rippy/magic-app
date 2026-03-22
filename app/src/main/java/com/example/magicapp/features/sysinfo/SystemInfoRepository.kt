package com.example.magicapp.features.sysinfo

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

class SystemInfoRepository(private val context: Context) {

    private val _state = MutableStateFlow(SystemInfoState())
    val state: StateFlow<SystemInfoState> = _state.asStateFlow()

    private val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val batteryManager =
        context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    private val scope = CoroutineScope(Dispatchers.Default)
    private var pollingJob: Job? = null

    private var prevTotal = 0L
    private var prevIdle = 0L

    fun start() {
        pollingJob = scope.launch {
            while (isActive) {
                _state.value = buildState()
                delay(2000)
            }
        }
    }

    fun stop() {
        pollingJob?.cancel()
    }

    private fun buildState(): SystemInfoState {
        val memInfo = ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }
        val ramTotal = memInfo.totalMem / (1024 * 1024)
        val ramUsed = ramTotal - memInfo.availMem / (1024 * 1024)

        val stat = StatFs(Environment.getDataDirectory().path)
        val storageTotal = stat.totalBytes / (1024f * 1024 * 1024)
        val storageAvail = stat.availableBytes / (1024f * 1024 * 1024)
        val storageUsed = storageTotal - storageAvail

        val batteryPct = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val isCharging = batteryManager.isCharging

        val network = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(network)
        val networkType = when {
            caps == null -> "None"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Other"
        }

        val cpuPercent = readCpuPercent()

        return SystemInfoState(
            cpuPercent = cpuPercent,
            ramUsedMb = ramUsed,
            ramTotalMb = ramTotal,
            storageUsedGb = storageUsed,
            storageTotalGb = storageTotal,
            batteryPercent = batteryPct,
            isCharging = isCharging,
            networkType = networkType
        )
    }

    private fun readCpuPercent(): Int {
        return try {
            val line = java.io.File("/proc/stat").bufferedReader().readLine() ?: return 0
            val parts = line.split(" ").filter { it.isNotBlank() }.drop(1)
                .map { it.toLongOrNull() ?: 0L }
            if (parts.size < 4) return 0
            val idle = parts[3]
            val total = parts.sum()
            val diffTotal = total - prevTotal
            val diffIdle = idle - prevIdle
            prevTotal = total
            prevIdle = idle
            if (diffTotal == 0L) 0 else ((diffTotal - diffIdle) * 100 / diffTotal).toInt()
        } catch (_: Exception) {
            0
        }
    }
}
