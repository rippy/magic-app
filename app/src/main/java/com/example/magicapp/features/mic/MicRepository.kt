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
