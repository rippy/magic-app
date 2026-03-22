package com.example.magicapp

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppViewModel : ViewModel() {

    private val _mode = MutableStateFlow(AppMode.GLANCE)
    val mode: StateFlow<AppMode> = _mode.asStateFlow()

    private val _isMicMuted = MutableStateFlow(false)
    val isMicMuted: StateFlow<Boolean> = _isMicMuted.asStateFlow()

    fun toggleMode() {
        _mode.value = if (_mode.value == AppMode.GLANCE) AppMode.DETAIL else AppMode.GLANCE
    }

    fun toggleMic() {
        _isMicMuted.value = !_isMicMuted.value
    }
}
