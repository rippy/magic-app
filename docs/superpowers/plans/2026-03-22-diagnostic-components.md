# Diagnostic Components Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace all 8 diagnostic FeaturePlaceholders with live implementations and overhaul the GlanceScreen hero card with live GPS data.

**Architecture:** Each diagnostic feature has a repository class (wraps Android API, exposes StateFlow) and a screen composable. A new DiagnosticViewModel (AndroidViewModel) holds all repositories. AppViewModel stays unchanged.

**Tech Stack:** Kotlin, Jetpack Compose, LocationManager, WifiManager, BluetoothLeScanner, SensorManager, AudioRecord, ActivityManager, BatteryManager, ConnectivityManager

---

## Worktree Setup

Before starting any tasks, create the worktree and branch:

```bash
cd /home/deck/projects/github.com/rippy/magic-app && git worktree add .worktrees/diagnostic-components -b feature/diagnostic-components
```

All subsequent commands use the path prefix:
```
/home/deck/projects/github.com/rippy/magic-app/.worktrees/diagnostic-components
```

---

## File Map

```text
app/src/main/java/com/example/magicapp/
  DiagnosticViewModel.kt                              (create)
  MainActivity.kt                                     (update)
  AppNavHost.kt                                       (update)
  features/
    gps/
      GpsRepository.kt                               (create)
    scanner/
      WifiRepository.kt                              (create)
      BtRepository.kt                                (create)
    mic/
      MicRepository.kt                               (create)
    sysinfo/
      SystemInfoRepository.kt                        (create)
    compass/
      CompassRepository.kt                           (create)
    clock/
      ClockRepository.kt                             (create)
    events/
      CarButtonRepository.kt                         (create)
  ui/
    components/
      GpsScreen.kt                                   (create)
      WifiScreen.kt                                  (create)
      BtScreen.kt                                    (create)
      MicScreen.kt                                   (create)
      SystemInfoScreen.kt                            (create)
      CompassScreen.kt                               (create)
      ClockScreen.kt                                 (create)
      CarButtonScreen.kt                             (create)
    DetailScreen.kt                                  (update)
    GlanceScreen.kt                                  (update)
    Feature.kt                                       (update)
app/src/test/java/com/example/magicapp/
  DiagnosticViewModelTest.kt                         (create)
```

---

## Task 1 — DiagnosticViewModel skeleton + wire into MainActivity and AppNavHost

Create `DiagnosticViewModel.kt`, add `diagnosticViewModel` to `MainActivity` and `AppNavHost`. This task produces no behavior change but must compile cleanly.

- [ ] Create `app/src/main/java/com/example/magicapp/DiagnosticViewModel.kt`:

```kotlin
package com.example.magicapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.magicapp.features.clock.ClockRepository
import com.example.magicapp.features.compass.CompassRepository
import com.example.magicapp.features.events.CarButtonRepository
import com.example.magicapp.features.gps.GpsRepository
import com.example.magicapp.features.mic.MicRepository
import com.example.magicapp.features.scanner.BtRepository
import com.example.magicapp.features.scanner.WifiRepository
import com.example.magicapp.features.sysinfo.SystemInfoRepository

class DiagnosticViewModel(application: Application) : AndroidViewModel(application) {

    val gps = GpsRepository(application)
    val wifi = WifiRepository(application)
    val bt = BtRepository(application)
    val mic = MicRepository()
    val sysInfo = SystemInfoRepository(application)
    val compass = CompassRepository(application)
    val clock = ClockRepository()
    val carButtons = CarButtonRepository()

    init {
        // GPS is started from MainActivity after ACCESS_FINE_LOCATION permission is granted.
        // gps.start() is NOT called here; see MainActivity.permissionLauncher.
        wifi.register()
        compass.start()
        clock.start()
        sysInfo.start()
    }

    override fun onCleared() {
        super.onCleared()
        gps.stop()
        wifi.unregister()
        bt.stopScan()
        mic.release()
        sysInfo.stop()
        compass.stop()
        clock.stop()
    }
}
```

- [ ] Update `app/src/main/java/com/example/magicapp/MainActivity.kt` — add `diagnosticViewModel` field and pass it to `AppNavHost`. Do NOT yet add permissions or key event overrides (those come in Task 2). The updated file should look like:

```kotlin
package com.example.magicapp

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.magicapp.ui.theme.MagicAppTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()
    private val diagnosticViewModel: DiagnosticViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            MagicAppTheme {
                AppNavHost(
                    viewModel = viewModel,
                    diagnosticViewModel = diagnosticViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
```

- [ ] Update `app/src/main/java/com/example/magicapp/AppNavHost.kt` — add `diagnosticViewModel: DiagnosticViewModel` parameter and pass it through to `GlanceScreen` and `DetailScreen`. Both screens still show stubs at this point; just thread the parameter. Example signature change only (preserve existing screen calls, just add the new param):

```kotlin
package com.example.magicapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.magicapp.ui.DetailScreen
import com.example.magicapp.ui.GlanceScreen

@Composable
fun AppNavHost(
    viewModel: AppViewModel,
    diagnosticViewModel: DiagnosticViewModel,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    val mode by viewModel.mode.collectAsState()

    LaunchedEffect(mode) {
        when (mode) {
            AppMode.GLANCE -> navController.navigate("glance") {
                popUpTo("glance") { inclusive = true }
            }
            AppMode.DETAIL -> navController.navigate("detail") {
                popUpTo("glance")
            }
        }
    }

    NavHost(navController = navController, startDestination = "glance", modifier = modifier) {
        composable("glance") {
            GlanceScreen(viewModel = viewModel, diagnosticViewModel = diagnosticViewModel)
        }
        composable("detail") {
            DetailScreen(viewModel = viewModel, diagnosticViewModel = diagnosticViewModel)
        }
    }
}
```

- [ ] Update `app/src/main/java/com/example/magicapp/ui/GlanceScreen.kt` — add `diagnosticViewModel: DiagnosticViewModel` parameter to the `GlanceScreen` composable signature (no behavior change yet). Ensure the file compiles.

- [ ] Update `app/src/main/java/com/example/magicapp/ui/DetailScreen.kt` — add `diagnosticViewModel: DiagnosticViewModel` parameter to the `DetailScreen` composable signature (no behavior change yet). Ensure the file compiles.

- [ ] Create all empty package directories so Kotlin can compile the imports in `DiagnosticViewModel.kt`. Create stub source files (they will be replaced in subsequent tasks):
  - `app/src/main/java/com/example/magicapp/features/gps/GpsRepository.kt` — empty data class + class stubs
  - `app/src/main/java/com/example/magicapp/features/scanner/WifiRepository.kt` — empty stubs
  - `app/src/main/java/com/example/magicapp/features/scanner/BtRepository.kt` — empty stubs
  - `app/src/main/java/com/example/magicapp/features/mic/MicRepository.kt` — empty stubs
  - `app/src/main/java/com/example/magicapp/features/sysinfo/SystemInfoRepository.kt` — empty stubs
  - `app/src/main/java/com/example/magicapp/features/compass/CompassRepository.kt` — empty stubs
  - `app/src/main/java/com/example/magicapp/features/clock/ClockRepository.kt` — empty stubs
  - `app/src/main/java/com/example/magicapp/features/events/CarButtonRepository.kt` — empty stubs

  Each stub file needs the package declaration and enough of a class/data class skeleton to satisfy the `DiagnosticViewModel` imports and field references. Minimal stubs:

  **GpsRepository.kt stub:**
  ```kotlin
  package com.example.magicapp.features.gps

  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.flow.asStateFlow

  data class GpsState(
      val speedKph: Float = 0f,
      val latitude: Double = 0.0,
      val longitude: Double = 0.0,
      val altitude: Double = 0.0,
      val bearing: Float = 0f,
      val accuracy: Float = 0f,
      val satelliteCount: Int = 0,
      val hasPermission: Boolean = false
  )

  class GpsRepository(private val context: android.content.Context) {
      private val _state = MutableStateFlow(GpsState())
      val state: StateFlow<GpsState> = _state.asStateFlow()
      fun start() {}
      fun stop() {}
  }
  ```

  **WifiRepository.kt stub:**
  ```kotlin
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
  ```

  **BtRepository.kt stub:**
  ```kotlin
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
  ```

  **MicRepository.kt stub:**
  ```kotlin
  package com.example.magicapp.features.mic

  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.flow.asStateFlow

  data class MicState(
      val amplitudeDb: Float = -60f,
      val isRecording: Boolean = false,
      val hasPermission: Boolean = false
  )

  class MicRepository {
      private val _state = MutableStateFlow(MicState())
      val state: StateFlow<MicState> = _state.asStateFlow()
      fun startRecording() {}
      fun stopRecording() {}
      fun release() {}
  }
  ```

  **SystemInfoRepository.kt stub:**
  ```kotlin
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
  ```

  **CompassRepository.kt stub:**
  ```kotlin
  package com.example.magicapp.features.compass

  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.flow.asStateFlow

  data class CompassState(
      val azimuthDeg: Float = 0f,
      val cardinalDirection: String = "N",
      val hasHardware: Boolean = true
  )

  class CompassRepository(private val context: android.content.Context) {
      private val _state = MutableStateFlow(CompassState())
      val state: StateFlow<CompassState> = _state.asStateFlow()
      fun start() {}
      fun stop() {}
  }
  ```

  **ClockRepository.kt stub:**
  ```kotlin
  package com.example.magicapp.features.clock

  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.flow.asStateFlow

  data class ClockState(
      val timeString: String = "",
      val tripElapsedSec: Long = 0L,
      val tripRunning: Boolean = false
  )

  class ClockRepository {
      private val _state = MutableStateFlow(ClockState())
      val state: StateFlow<ClockState> = _state.asStateFlow()
      fun start() {}
      fun stop() {}
      fun startTrip() {}
      fun stopTrip() {}
      fun resetTrip() {}
  }
  ```

  **CarButtonRepository.kt stub:**
  ```kotlin
  package com.example.magicapp.features.events

  import android.view.KeyEvent
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.flow.asStateFlow

  data class CarEvent(
      val timestamp: String,
      val action: String,
      val keyCode: String,
      val keyName: String
  )

  class CarButtonRepository {
      private val _events = MutableStateFlow<List<CarEvent>>(emptyList())
      val events: StateFlow<List<CarEvent>> = _events.asStateFlow()
      fun onKeyEvent(eventTime: Long, action: Int, keyCode: Int) {}
      fun onKeyEvent(event: KeyEvent) = onKeyEvent(event.eventTime, event.action, event.keyCode)
      fun clear() {}
  }
  ```

- [ ] Run build to verify compilation:

```bash
cd /home/deck/projects/github.com/rippy/magic-app/.worktrees/diagnostic-components && ./gradlew assembleDebug
```

- [ ] Commit:

```bash
cd /home/deck/projects/github.com/rippy/magic-app/.worktrees/diagnostic-components && git add app/src/main/java/com/example/magicapp/DiagnosticViewModel.kt app/src/main/java/com/example/magicapp/MainActivity.kt app/src/main/java/com/example/magicapp/AppNavHost.kt app/src/main/java/com/example/magicapp/ui/GlanceScreen.kt app/src/main/java/com/example/magicapp/ui/DetailScreen.kt app/src/main/java/com/example/magicapp/features/ && git commit -m "feat: add DiagnosticViewModel skeleton and wire into MainActivity/AppNavHost"
```

---

## Task 2 — Runtime permissions + key event forwarding in MainActivity

> **Note:** No `AndroidManifest.xml` changes are needed in this task. All required permissions (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `RECORD_AUDIO`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, `BLUETOOTH`, `BLUETOOTH_ADMIN`, `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`) were already added in Plan 1. `BLUETOOTH` and `BLUETOOTH_ADMIN` (with `android:maxSdkVersion="30"`) cover our `targetSdk=29` target; the API 31+ `BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT` entries are already present for future-proofing. No manifest edits required.

- [ ] Replace `app/src/main/java/com/example/magicapp/MainActivity.kt` with the full version including permissions and key event forwarding:

```kotlin
package com.example.magicapp

import android.Manifest
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.magicapp.ui.theme.MagicAppTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()
    private val diagnosticViewModel: DiagnosticViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            diagnosticViewModel.gps.start()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            MagicAppTheme {
                AppNavHost(
                    viewModel = viewModel,
                    diagnosticViewModel = diagnosticViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.RECORD_AUDIO
            )
        )
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        event?.let { diagnosticViewModel.carButtons.onKeyEvent(it) }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        event?.let { diagnosticViewModel.carButtons.onKeyEvent(it) }
        return super.onKeyUp(keyCode, event)
    }
}
```

- [ ] Run build:

```bash
cd /home/deck/projects/github.com/rippy/magic-app/.worktrees/diagnostic-components && ./gradlew assembleDebug
```

- [ ] Commit:

```bash
cd /home/deck/projects/github.com/rippy/magic-app/.worktrees/diagnostic-components && git add app/src/main/java/com/example/magicapp/MainActivity.kt && git commit -m "feat: add runtime permissions and key event forwarding in MainActivity"
```

---

## Task 3 — GpsRepository + GpsScreen

Replace the GpsRepository stub with the full implementation and create the GpsScreen composable.

- [ ] Replace `app/src/main/java/com/example/magicapp/features/gps/GpsRepository.kt` with the full implementation:

```kotlin
package com.example.magicapp.features.gps

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GpsState(
    val speedKph: Float = 0f,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val bearing: Float = 0f,
    val accuracy: Float = 0f,
    val satelliteCount: Int = 0,
    val hasPermission: Boolean = false
)

class GpsRepository(private val context: Context) {

    private val _state = MutableStateFlow(GpsState())
    val state: StateFlow<GpsState> = _state.asStateFlow()

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val listener = LocationListener { location ->
        _state.value = GpsState(
            speedKph = location.speed * 3.6f,
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            bearing = location.bearing,
            accuracy = location.accuracy,
            satelliteCount = location.extras?.getInt("satellites") ?: 0,
            hasPermission = true
        )
    }

    @SuppressLint("MissingPermission")
    fun start() {
        _state.value = _state.value.copy(hasPermission = true)
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER, 1000L, 0f, listener
        )
    }

    fun stop() {
        locationManager.removeUpdates(listener)
    }
}
```

- [ ] Create `app/src/main/java/com/example/magicapp/ui/components/GpsScreen.kt`:

```kotlin
package com.example.magicapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.magicapp.features.gps.GpsRepository

@Composable
fun GpsScreen(repository: GpsRepository, modifier: Modifier = Modifier) {
    val state by repository.state.collectAsState()

    if (!state.hasPermission) {
        PermissionRequired(feature = "GPS", permission = "Location")
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "%.0f km/h".format(state.speedKph),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary
        )
        InfoRow("Lat / Lon", "%.5f, %.5f".format(state.latitude, state.longitude))
        InfoRow("Heading", "%.1f°".format(state.bearing))
        InfoRow("Altitude", "%.0f m".format(state.altitude))
        InfoRow("Accuracy", "±%.0f m".format(state.accuracy))
        InfoRow("Satellites", "${state.satelliteCount}")
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun PermissionRequired(feature: String, permission: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "$feature requires $permission permission",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
    }
}
```

- [ ] Run build:

```bash
cd /home/deck/projects/github.com/rippy/magic-app/.worktrees/diagnostic-components && ./gradlew assembleDebug
```

- [ ] Commit:

```bash
cd /home/deck/projects/github.com/rippy/magic-app/.worktrees/diagnostic-components && git add app/src/main/java/com/example/magicapp/features/gps/GpsRepository.kt app/src/main/java/com/example/magicapp/ui/components/GpsScreen.kt && git commit -m "feat: implement GpsRepository and GpsScreen"
```

---

## Task 4 — WifiRepository + WifiScreen

Replace the WifiRepository stub with the full implementation and create WifiScreen.

- [ ] Replace `app/src/main/java/com/example/magicapp/features/scanner/WifiRepository.kt` with the full implementation:

```kotlin
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
```

- [ ] Create `app/src/main/java/com/example/magicapp/ui/components/WifiScreen.kt`:

```kotlin
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
import com.example.magicapp.features.scanner.WifiRepository

@Composable
fun WifiScreen(repository: WifiRepository, modifier: Modifier = Modifier) {
    val state by repository.state.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "WiFi Networks (${state.networks.size})",
                style = MaterialTheme.typography.titleLarge
            )
            Button(onClick = { repository.scan() }, enabled = !state.isScanning) {
                Text(if (state.isScanning) "Scanning..." else "Scan")
            }
        }

        state.lastScannedMs?.let { ts ->
            val secsAgo = (System.currentTimeMillis() - ts) / 1000
            Text(
                "Last scanned: ${secsAgo}s ago",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            if (state.isFromCache) {
                Text(
                    "Results may be from cache",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        if (!state.hasPermission) {
            PermissionRequired("WiFi scan", "Location")
            return@Column
        }

        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(state.networks.sortedByDescending { it.rssi }) { net ->
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
                        Text(net.ssid, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${net.rssi} dBm · ${net.security}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] Run build:

```bash
cd /home/deck/projects/github.com/rippy/magic-app/.worktrees/diagnostic-components && ./gradlew assembleDebug
```

- [ ] Commit:

```bash
cd /home/deck/projects/github.com/rippy/magic-app/.worktrees/diagnostic-components && git add app/src/main/java/com/example/magicapp/features/scanner/WifiRepository.kt app/src/main/java/com/example/magicapp/ui/components/WifiScreen.kt && git commit -m "feat: implement WifiRepository and WifiScreen"
```

---

## Task 5 — BtRepository + BtScreen

Replace the BtRepository stub with the full implementation (including 10-second auto-stop via coroutine) and create BtScreen.

- [ ] Replace `app/src/main/java/com/example/magicapp/features/scanner/BtRepository.kt` with the full implementation:

```kotlin
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
```

- [ ] Create `app/src/main/java/com/example/magicapp/ui/components/BtScreen.kt`:

```kotlin
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
```

- [ ] Run build:

```bash
cd /home/deck/projects/github.com/rippy/magic-app/.worktrees/diagnostic-components && ./gradlew assembleDebug
```

- [ ] Commit:

```bash
cd /home/deck/projects/github.com/rippy/magic-app/.worktrees/diagnostic-components && git add app/src/main/java/com/example/magicapp/features/scanner/BtRepository.kt app/src/main/java/com/example/magicapp/ui/components/BtScreen.kt && git commit -m "feat: implement BtRepository with auto-stop and BtScreen"
```

---

## Task 6 — MicRepository + MicScreen

Replace the MicRepository stub with the full AudioRecord-based implementation and create MicScreen.

- [ ] Replace `app/src/main/java/com/example/magicapp/features/mic/MicRepository.kt` with the full implementation:

```kotlin
package com.example.magicapp.features.mic

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.log10

data class MicState(
    val amplitudeDb: Float = -60f, // -60 = silence, 0 = max
    val isRecording: Boolean = false,
    val hasPermission: Boolean = false
)

class MicRepository {

    private val _state = MutableStateFlow(MicState())
    val state: StateFlow<MicState> = _state.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val sampleRate = 44100
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
    )

    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    fun startRecording() {
        if (_state.value.isRecording) return
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        audioRecord?.startRecording()
        _state.value = _state.value.copy(isRecording = true, hasPermission = true)
        recordingJob = scope.launch {
            val buffer = ShortArray(bufferSize)
            while (isActive) {
                val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                if (read > 0) {
                    val maxAmp = buffer.take(read).maxOf { abs(it.toInt()) }
                    val db = if (maxAmp > 0) 20f * log10(maxAmp / 32768f) else -60f
                    _state.value = _state.value.copy(amplitudeDb = db.coerceAtLeast(-60f))
                }
            }
        }
    }

    fun stopRecording() {
        recordingJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        _state.value = _state.value.copy(isRecording = false, amplitudeDb = -60f)
    }

    fun release() = stopRecording()
}
```

- [ ] Create `app/src/main/java/com/example/magicapp/ui/components/MicScreen.kt`:

```kotlin
package com.example.magicapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.example.magicapp.features.mic.MicRepository

@Composable
fun MicScreen(repository: MicRepository, modifier: Modifier = Modifier) {
    val state by repository.state.collectAsState()
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
    ) {
        Text("Microphone", style = MaterialTheme.typography.titleLarge)

        val level = ((state.amplitudeDb + 60f) / 60f).coerceIn(0f, 1f)
        Canvas(modifier = Modifier.fillMaxWidth().height(32.dp)) {
            drawRect(color = surface, size = Size(this.size.width, this.size.height))
            drawRect(color = primary, size = Size(this.size.width * level, this.size.height))
        }
        Text("%.0f dB".format(state.amplitudeDb), style = MaterialTheme.typography.bodyLarge)

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = {
                if (state.isRecording) repository.stopRecording()
                else repository.startRecording()
            }) {
                Text(if (state.isRecording) "Stop" else "Record")
            }
        }

        if (!state.hasPermission) {
            PermissionRequired("Microphone", "Record Audio")
        }
    }
}
```

- [ ] Run build:

```bash
cd /home/deck/projects/github.com/rippy/magic-app/.worktrees/diagnostic-components && ./gradlew assembleDebug
```

- [ ] Commit:

```bash
cd /home/deck/projects/github.com/rippy/magic-app/.worktrees/diagnostic-components && git add app/src/main/java/com/example/magicapp/features/mic/MicRepository.kt app/src/main/java/com/example/magicapp/ui/components/MicScreen.kt && git commit -m "feat: implement MicRepository with AudioRecord and MicScreen"
```

---

## Task 7 — SystemInfoRepository + SystemInfoScreen

Replace the SystemInfoRepository stub with the full polling implementation and create SystemInfoScreen.

- [ ] Replace `app/src/main/java/com/example/magicapp/features/sysinfo/SystemInfoRepository.kt` with the full implementation:

```kotlin
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
```

- [ ] Create `app/src/main/java/com/example/magicapp/ui/components/SystemInfoScreen.kt`:

```kotlin
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
```

- [ ] Run build:

```bash
cd /home/deck/projects/github.com/rippy/magic-app/.worktrees/diagnostic-components && ./gradlew assembleDebug
```

- [ ] Commit:

```bash
cd /home/deck/projects/github.com/rippy/magic-app/.worktrees/diagnostic-components && git add app/src/main/java/com/example/magicapp/features/sysinfo/SystemInfoRepository.kt app/src/main/java/com/example/magicapp/ui/components/SystemInfoScreen.kt && git commit -m "feat: implement SystemInfoRepository with CPU/RAM/storage polling and SystemInfoScreen"
```

---

## Task 8 — CompassRepository + CompassScreen + unit test for toCardinal

Replace the CompassRepository stub with the full SensorManager implementation, create CompassScreen, and add a unit test for the cardinal direction logic.

- [ ] Replace `app/src/main/java/com/example/magicapp/features/compass/CompassRepository.kt` with the full implementation:

```kotlin
package com.example.magicapp.features.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CompassState(
    val azimuthDeg: Float = 0f,
    val cardinalDirection: String = "N",
    val hasHardware: Boolean = true
)

class CompassRepository(private val context: Context) {

    private val _state = MutableStateFlow(CompassState())
    val state: StateFlow<CompassState> = _state.asStateFlow()

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private val listener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        override fun onSensorChanged(event: SensorEvent) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
            val normalised = (azimuth + 360) % 360
            _state.value = CompassState(
                azimuthDeg = normalised,
                cardinalDirection = toCardinal(normalised),
                hasHardware = true
            )
        }
    }

    fun start() {
        if (rotationSensor == null) {
            _state.value = CompassState(hasHardware = false)
            return
        }
        sensorManager.registerListener(
            listener, rotationSensor, SensorManager.SENSOR_DELAY_UI
        )
    }

    fun stop() {
        sensorManager.unregisterListener(listener)
    }

    companion object {
        fun toCardinal(deg: Float): String = when {
            deg < 22.5 || deg >= 337.5 -> "N"
            deg < 67.5 -> "NE"
            deg < 112.5 -> "E"
            deg < 157.5 -> "SE"
            deg < 202.5 -> "S"
            deg < 247.5 -> "SW"
            deg < 292.5 -> "W"
            else -> "NW"
        }
    }
}
```

Note: `toCardinal` is placed in a `companion object` so it can be called as `CompassRepository.toCardinal(deg)` without an instance (no Android dependencies, pure logic). The instance's `onSensorChanged` calls it as `toCardinal(normalised)` — which resolves to the companion function via Kotlin's companion object lookup.

- [ ] Create `app/src/main/java/com/example/magicapp/ui/components/CompassScreen.kt`:

```kotlin
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
```

- [ ] Create `app/src/test/java/com/example/magicapp/DiagnosticViewModelTest.kt` with compass cardinal direction tests (add other tests in Tasks 9 and 10 to the same file):

```kotlin
package com.example.magicapp

import com.example.magicapp.features.compass.CompassRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class CompassCardinalTest {

    // toCardinal is a companion object function — no Android context required.
    // Call it directly via CompassRepository.toCardinal(deg).

    @Test fun north() = assertEquals("N", CompassRepository.toCardinal(0f))
    @Test fun northAlsoAtMax() = assertEquals("N", CompassRepository.toCardinal(359f))
    @Test fun northEast() = assertEquals("NE", CompassRepository.toCardinal(45f))
    @Test fun east() = assertEquals("E", CompassRepository.toCardinal(90f))
    @Test fun southEast() = assertEquals("SE", CompassRepository.toCardinal(135f))
    @Test fun south() = assertEquals("S", CompassRepository.toCardinal(180f))
    @Test fun southWest() = assertEquals("SW", CompassRepository.toCardinal(225f))
    @Test fun west() = assertEquals("W", CompassRepository.toCardinal(270f))
    @Test fun northWest() = assertEquals("NW", CompassRepository.toCardinal(315f))
}
```

- [ ] Run tests and build:

```bash
cd /home/deck/projects/github.com/rippy/magic-app/.worktrees/diagnostic-components && ./gradlew test assembleDebug
```

- [ ] Commit:

```bash
cd /home/deck/projects/github.com/rippy/magic-app/.worktrees/diagnostic-components && git add app/src/main/java/com/example/magicapp/features/compass/CompassRepository.kt app/src/main/java/com/example/magicapp/ui/components/CompassScreen.kt app/src/test/java/com/example/magicapp/DiagnosticViewModelTest.kt && git commit -m "feat: implement CompassRepository and CompassScreen; add cardinal direction unit tests"
```

---

## Task 9 — ClockRepository + ClockScreen + unit tests for trip timer

Replace the ClockRepository stub with the full coroutine-based implementation, create ClockScreen, and add trip timer unit tests.

- [ ] Replace `app/src/main/java/com/example/magicapp/features/clock/ClockRepository.kt` with the full implementation:

```kotlin
package com.example.magicapp.features.clock

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ClockState(
    val timeString: String = "",
    val tripElapsedSec: Long = 0L,
    val tripRunning: Boolean = false
)

class ClockRepository {

    private val _state = MutableStateFlow(ClockState())
    val state: StateFlow<ClockState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default)
    private var tickJob: Job? = null
    private var tripStartMs: Long = 0L
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun start() {
        tickJob = scope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val elapsed = if (_state.value.tripRunning) {
                    (now - tripStartMs) / 1000L
                } else {
                    _state.value.tripElapsedSec
                }
                _state.value = _state.value.copy(
                    timeString = fmt.format(Date(now)),
                    tripElapsedSec = elapsed
                )
                delay(1000)
            }
        }
    }

    fun stop() {
        tickJob?.cancel()
    }

    fun startTrip() {
        tripStartMs = System.currentTimeMillis() - _state.value.tripElapsedSec * 1000L
        _state.value = _state.value.copy(tripRunning = true)
    }

    fun stopTrip() {
        _state.value = _state.value.copy(tripRunning = false)
    }

    fun resetTrip() {
        tripStartMs = System.currentTimeMillis()
        _state.value = _state.value.copy(tripElapsedSec = 0L, tripRunning = false)
    }
}
```

- [ ] Create `app/src/main/java/com/example/magicapp/ui/components/ClockScreen.kt`:

```kotlin
package com.example.magicapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.magicapp.features.clock.ClockRepository

@Composable
fun ClockScreen(repository: ClockRepository, modifier: Modifier = Modifier) {
    val state by repository.state.collectAsState()
    val h = state.tripElapsedSec / 3600
    val m = (state.tripElapsedSec % 3600) / 60
    val s = state.tripElapsedSec % 60

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
    ) {
        Text(
            state.timeString,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text("Trip Timer", style = MaterialTheme.typography.titleMedium)
        Text(
            "%02d:%02d:%02d".format(h, m, s),
            style = MaterialTheme.typography.displaySmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!state.tripRunning) {
                Button(onClick = { repository.startTrip() }) { Text("Start") }
            } else {
                Button(onClick = { repository.stopTrip() }) { Text("Stop") }
            }
            OutlinedButton(onClick = { repository.resetTrip() }) { Text("Reset") }
        }
    }
}
```

- [ ] Add trip timer unit tests to `app/src/test/java/com/example/magicapp/DiagnosticViewModelTest.kt` (append to the existing file):

```kotlin
// --- Trip Timer Tests ---

class ClockRepositoryTripTest {

    @Test
    fun `startTrip sets tripRunning true`() {
        val repo = com.example.magicapp.features.clock.ClockRepository()
        repo.startTrip()
        assertEquals(true, repo.state.value.tripRunning)
    }

    @Test
    fun `stopTrip sets tripRunning false`() {
        val repo = com.example.magicapp.features.clock.ClockRepository()
        repo.startTrip()
        repo.stopTrip()
        assertEquals(false, repo.state.value.tripRunning)
    }

    @Test
    fun `resetTrip sets elapsed to zero and tripRunning false`() {
        val repo = com.example.magicapp.features.clock.ClockRepository()
        repo.startTrip()
        repo.resetTrip()
        assertEquals(0L, repo.state.value.tripElapsedSec)
        assertEquals(false, repo.state.value.tripRunning)
    }
}
```

- [ ] Run tests and build:

```bash
cd /home/deck/projects/github.com/rippy/magic-app/.worktrees/diagnostic-components && ./gradlew test assembleDebug
```

- [ ] Commit:

```bash
cd /home/deck/projects/github.com/rippy/magic-app/.worktrees/diagnostic-components && git add app/src/main/java/com/example/magicapp/features/clock/ClockRepository.kt app/src/main/java/com/example/magicapp/ui/components/ClockScreen.kt app/src/test/java/com/example/magicapp/DiagnosticViewModelTest.kt && git commit -m "feat: implement ClockRepository and ClockScreen; add trip timer unit tests"
```

---

## Task 10 — CarButtonRepository + CarButtonScreen + unit tests

Replace the CarButtonRepository stub with the full implementation and create CarButtonScreen.

- [ ] Replace `app/src/main/java/com/example/magicapp/features/events/CarButtonRepository.kt` with the full implementation:

```kotlin
package com.example.magicapp.features.events

import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CarEvent(
    val timestamp: String,
    val action: String,
    val keyCode: String,
    val keyName: String
)

class CarButtonRepository {

    private val _events = MutableStateFlow<List<CarEvent>>(emptyList())
    val events: StateFlow<List<CarEvent>> = _events.asStateFlow()

    private val fmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    // Primary overload: accepts raw primitive values so JVM unit tests can call it
    // without instantiating android.view.KeyEvent (which fails in JVM test environments).
    fun onKeyEvent(eventTime: Long, action: Int, keyCode: Int) {
        val newEvent = CarEvent(
            timestamp = fmt.format(Date(eventTime)),
            action = if (action == KeyEvent.ACTION_DOWN) "DOWN" else "UP",
            keyCode = "$keyCode",
            keyName = KeyEvent.keyCodeToString(keyCode)
        )
        _events.value = (_events.value + newEvent).takeLast(100)
    }

    // Convenience overload for production code paths that already have a KeyEvent.
    fun onKeyEvent(event: KeyEvent) = onKeyEvent(event.eventTime, event.action, event.keyCode)

    fun clear() {
        _events.value = emptyList()
    }
}
```

- [ ] Create `app/src/main/java/com/example/magicapp/ui/components/CarButtonScreen.kt`:

```kotlin
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.magicapp.features.events.CarButtonRepository

@Composable
fun CarButtonScreen(repository: CarButtonRepository, modifier: Modifier = Modifier) {
    val events by repository.events.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Car Button Events (${events.size})",
                style = MaterialTheme.typography.titleLarge
            )
            TextButton(onClick = { repository.clear() }) { Text("Clear") }
        }
        Spacer(Modifier.height(8.dp))
        if (events.isEmpty()) {
            Text(
                "Press car hardware buttons to capture events",
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(events.reversed()) { event ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                event.timestamp,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                "${event.action} ${event.keyName}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] Add CarButtonRepository unit tests to `app/src/test/java/com/example/magicapp/DiagnosticViewModelTest.kt` (append to the existing file):

```kotlin
// --- CarButtonRepository Tests ---
// Uses the (Long, Int, Int) overload to avoid instantiating android.view.KeyEvent
// in a JVM test environment (Android stubs throw RuntimeException when called from JVM).

class CarButtonRepositoryTest {

    // KeyEvent constants are safe to reference as they are just Int literals.
    private val ACTION_DOWN = android.view.KeyEvent.ACTION_DOWN
    private val ACTION_UP = android.view.KeyEvent.ACTION_UP
    private val KEYCODE_MEDIA_PLAY = android.view.KeyEvent.KEYCODE_MEDIA_PLAY

    @Test
    fun `onKeyEvent appends event to list`() {
        val repo = com.example.magicapp.features.events.CarButtonRepository()
        repo.onKeyEvent(System.currentTimeMillis(), ACTION_DOWN, KEYCODE_MEDIA_PLAY)
        assertEquals(1, repo.events.value.size)
        assertEquals("DOWN", repo.events.value[0].action)
    }

    @Test
    fun `onKeyEvent records UP action correctly`() {
        val repo = com.example.magicapp.features.events.CarButtonRepository()
        repo.onKeyEvent(System.currentTimeMillis(), ACTION_UP, KEYCODE_MEDIA_PLAY)
        assertEquals("UP", repo.events.value[0].action)
    }

    @Test
    fun `onKeyEvent keeps only last 100 events`() {
        val repo = com.example.magicapp.features.events.CarButtonRepository()
        repeat(105) {
            repo.onKeyEvent(System.currentTimeMillis(), ACTION_DOWN, KEYCODE_MEDIA_PLAY)
        }
        assertEquals(100, repo.events.value.size)
    }

    @Test
    fun `clear empties the event list`() {
        val repo = com.example.magicapp.features.events.CarButtonRepository()
        repo.onKeyEvent(System.currentTimeMillis(), ACTION_DOWN, KEYCODE_MEDIA_PLAY)
        repo.clear()
        assertEquals(0, repo.events.value.size)
    }
}
```

- [ ] Run tests and build:

```bash
cd /home/deck/projects/github.com/rippy/magic-app/.worktrees/diagnostic-components && ./gradlew test assembleDebug
```

- [ ] Commit:

```bash
cd /home/deck/projects/github.com/rippy/magic-app/.worktrees/diagnostic-components && git add app/src/main/java/com/example/magicapp/features/events/CarButtonRepository.kt app/src/main/java/com/example/magicapp/ui/components/CarButtonScreen.kt app/src/test/java/com/example/magicapp/DiagnosticViewModelTest.kt && git commit -m "feat: implement CarButtonRepository and CarButtonScreen; add event log unit tests"
```

---

## Task 11 — DetailScreen feature router

Replace the `FeaturePlaceholder` stub call in `DetailScreen.kt` with a `when (selectedFeature)` dispatcher routing to each diagnostic screen composable.

- [ ] Update `app/src/main/java/com/example/magicapp/ui/DetailScreen.kt`.

The `DetailScreen` composable already receives `diagnosticViewModel: DiagnosticViewModel` (wired in Task 1). Find the section where `FeaturePlaceholder(feature = selectedFeature, ...)` is called and replace it with the full `when` expression. The complete updated content area should be:

```kotlin
when (selectedFeature) {
    Feature.GPS -> GpsScreen(
        repository = diagnosticViewModel.gps,
        modifier = Modifier.fillMaxSize()
    )
    Feature.WIFI_SCANNER -> WifiScreen(
        repository = diagnosticViewModel.wifi,
        modifier = Modifier.fillMaxSize()
    )
    Feature.BT_SCANNER -> BtScreen(
        repository = diagnosticViewModel.bt,
        modifier = Modifier.fillMaxSize()
    )
    Feature.MICROPHONE -> MicScreen(
        repository = diagnosticViewModel.mic,
        modifier = Modifier.fillMaxSize()
    )
    Feature.SYSTEM_INFO -> SystemInfoScreen(
        repository = diagnosticViewModel.sysInfo,
        modifier = Modifier.fillMaxSize()
    )
    Feature.COMPASS -> CompassScreen(
        repository = diagnosticViewModel.compass,
        modifier = Modifier.fillMaxSize()
    )
    Feature.CLOCK -> ClockScreen(
        repository = diagnosticViewModel.clock,
        modifier = Modifier.fillMaxSize()
    )
    Feature.CAR_BUTTONS -> CarButtonScreen(
        repository = diagnosticViewModel.carButtons,
        modifier = Modifier.fillMaxSize()
    )
    else -> FeaturePlaceholder(
        feature = selectedFeature,
        modifier = Modifier.fillMaxSize()
    )
}
```

Add the necessary imports at the top of `DetailScreen.kt`:

```kotlin
import com.example.magicapp.DiagnosticViewModel
import com.example.magicapp.ui.components.BtScreen
import com.example.magicapp.ui.components.CarButtonScreen
import com.example.magicapp.ui.components.ClockScreen
import com.example.magicapp.ui.components.CompassScreen
import com.example.magicapp.ui.components.GpsScreen
import com.example.magicapp.ui.components.MicScreen
import com.example.magicapp.ui.components.SystemInfoScreen
import com.example.magicapp.ui.components.WifiScreen
```

- [ ] Run build:

```bash
cd /home/deck/projects/github.com/rippy/magic-app/.worktrees/diagnostic-components && ./gradlew assembleDebug
```

- [ ] Commit:

```bash
cd /home/deck/projects/github.com/rippy/magic-app/.worktrees/diagnostic-components && git add app/src/main/java/com/example/magicapp/ui/DetailScreen.kt && git commit -m "feat: replace FeaturePlaceholder with feature router in DetailScreen"
```

---

## Task 12 — GlanceScreen overhaul + Feature.glanceTiles update

Rewrite `GlanceScreen.kt` to implement the horizontal split layout (hero card ~60% + WiFi/BT scan tiles stacked right), live GPS data in `HeroCard`, `ScanTile` composable, and update `Feature.glanceTiles` to the four quick-launch tiles.

- [ ] Update `app/src/main/java/com/example/magicapp/Feature.kt` — change `glanceTiles` to return the four quick-launch features instead of the diagnostic ones. Find the `glanceTiles` property and update it:

```kotlin
val glanceTiles = listOf(NEWS, YOUTUBE, BROWSER, VOICE_ASSISTANT)
```

(The full `Feature.kt` enum must still contain all 14 entries; only `glanceTiles` changes.)

- [ ] Replace `app/src/main/java/com/example/magicapp/ui/GlanceScreen.kt` with the full overhauled layout:

```kotlin
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
import com.example.magicapp.ui.components.ControlsOverlay

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
```

Note: If `ControlsOverlay` is in a different package (e.g., `com.example.magicapp.ui`), adjust the import accordingly. If `QuickTile` or `HeroCard` were previously defined in separate files, those definitions can be removed.

- [ ] Run build:

```bash
cd /home/deck/projects/github.com/rippy/magic-app/.worktrees/diagnostic-components && ./gradlew assembleDebug
```

- [ ] Commit:

```bash
cd /home/deck/projects/github.com/rippy/magic-app/.worktrees/diagnostic-components && git add app/src/main/java/com/example/magicapp/Feature.kt app/src/main/java/com/example/magicapp/ui/GlanceScreen.kt && git commit -m "feat: overhaul GlanceScreen with GPS hero card, scan tiles, and updated quick-launch tiles"
```

---

## Task 13 — Build verification

Run a clean full build and verify `BUILD SUCCESSFUL`.

- [ ] Run full clean build:

```bash
cd /home/deck/projects/github.com/rippy/magic-app/.worktrees/diagnostic-components && ./gradlew clean assembleDebug
```

- [ ] Verify the output contains `BUILD SUCCESSFUL`. If any errors appear, fix them before proceeding.

- [ ] Run all unit tests:

```bash
cd /home/deck/projects/github.com/rippy/magic-app/.worktrees/diagnostic-components && ./gradlew test
```

- [ ] Verify all tests pass (look for `BUILD SUCCESSFUL` and no failing test output).

- [ ] Commit a final summary commit if any fixup changes were made during verification. If no changes were needed, no commit is required.

- [ ] Create a PR from `feature/diagnostic-components` into `main`:

```bash
cd /home/deck/projects/github.com/rippy/magic-app/.worktrees/diagnostic-components && git push -u origin feature/diagnostic-components
gh pr create --title "feat: Diagnostic Components (Plan 2)" --body "$(cat <<'EOF'
## Summary

- Adds `DiagnosticViewModel` (`AndroidViewModel`) holding all 8 diagnostic repositories
- Implements live GPS, WiFi scanner, BT/BLE scanner, microphone, system info, compass, clock/trip timer, and car button event log screens
- Overhauled `GlanceScreen` with horizontal split: hero card showing live GPS speed/coordinates/heading, WiFi and BT scan tiles stacked on the right, and four quick-launch tiles at the bottom
- Routes `DetailScreen` to the correct diagnostic screen via `when (selectedFeature)` — no more `FeaturePlaceholder` for these 8 features
- Runtime permissions (`ACCESS_FINE_LOCATION`, `RECORD_AUDIO`) requested at app start; GPS starts after grant
- Car hardware key events forwarded from `MainActivity` to `CarButtonRepository`

## Test plan

- [ ] `./gradlew test` — all unit tests pass (compass cardinal directions, trip timer states, car button event log)
- [ ] `./gradlew assembleDebug` — `BUILD SUCCESSFUL`
- [ ] Install APK on device, grant location + microphone permissions
- [ ] Verify GlanceScreen shows speed in hero card (0 km/h when stationary is expected)
- [ ] Navigate to Detail → GPS: coordinates update as device moves
- [ ] Detail → WiFi: tap Scan, results appear sorted by RSSI
- [ ] Detail → BT: tap Scan, BLE devices appear; scan auto-stops after 10s
- [ ] Detail → Microphone: tap Record, amplitude bar animates with sound
- [ ] Detail → System Info: CPU/RAM/storage/battery values update every 2s
- [ ] Detail → Compass: needle rotates as device rotates
- [ ] Detail → Clock: time updates; Start/Stop/Reset trip timer works
- [ ] Detail → Car Buttons: press hardware buttons, events appear in list; Clear clears them

Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

---

## Summary

| Task | Files Created / Modified | Key Android API |
|------|--------------------------|-----------------|
| 1 | `DiagnosticViewModel.kt`, `MainActivity.kt`, `AppNavHost.kt`, `GlanceScreen.kt`, `DetailScreen.kt`, 8 stub repositories | ViewModel |
| 2 | `MainActivity.kt` | `ActivityResultContracts.RequestMultiplePermissions` |
| 3 | `GpsRepository.kt`, `GpsScreen.kt` | `LocationManager.requestLocationUpdates` |
| 4 | `WifiRepository.kt`, `WifiScreen.kt` | `WifiManager`, `BroadcastReceiver` |
| 5 | `BtRepository.kt`, `BtScreen.kt` | `BluetoothLeScanner`, `ScanCallback` |
| 6 | `MicRepository.kt`, `MicScreen.kt` | `AudioRecord` |
| 7 | `SystemInfoRepository.kt`, `SystemInfoScreen.kt` | `ActivityManager`, `StatFs`, `BatteryManager`, `ConnectivityManager` |
| 8 | `CompassRepository.kt`, `CompassScreen.kt`, unit tests | `SensorManager.TYPE_ROTATION_VECTOR` |
| 9 | `ClockRepository.kt`, `ClockScreen.kt`, unit tests | Coroutines, `SimpleDateFormat` |
| 10 | `CarButtonRepository.kt`, `CarButtonScreen.kt`, unit tests | `KeyEvent` |
| 11 | `DetailScreen.kt` | `when` dispatch |
| 12 | `GlanceScreen.kt`, `Feature.kt` | Compose layout |
| 13 | Build verification + PR | — |
