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
