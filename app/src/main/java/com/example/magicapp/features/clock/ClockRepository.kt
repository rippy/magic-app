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
