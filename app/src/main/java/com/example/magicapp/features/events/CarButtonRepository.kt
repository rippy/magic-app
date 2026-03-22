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
        val keyName = try {
            KeyEvent.keyCodeToString(keyCode)
        } catch (e: RuntimeException) {
            "KEYCODE_$keyCode"
        }
        val newEvent = CarEvent(
            timestamp = fmt.format(Date(eventTime)),
            action = if (action == KeyEvent.ACTION_DOWN) "DOWN" else "UP",
            keyCode = "$keyCode",
            keyName = keyName
        )
        _events.value = (_events.value + newEvent).takeLast(100)
    }

    // Convenience overload for production code paths that already have a KeyEvent.
    fun onKeyEvent(event: KeyEvent) = onKeyEvent(event.eventTime, event.action, event.keyCode)

    fun clear() {
        _events.value = emptyList()
    }
}
