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
