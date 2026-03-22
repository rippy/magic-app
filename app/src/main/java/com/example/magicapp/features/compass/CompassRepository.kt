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
