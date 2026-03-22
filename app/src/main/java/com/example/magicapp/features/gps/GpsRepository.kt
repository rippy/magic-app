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
