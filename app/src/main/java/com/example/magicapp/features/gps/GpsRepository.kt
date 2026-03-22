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
