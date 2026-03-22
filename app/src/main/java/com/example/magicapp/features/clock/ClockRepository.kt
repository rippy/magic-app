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
