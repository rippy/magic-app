package com.example.magicapp.features.mic

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.log10

data class MicState(
    val amplitudeDb: Float = -60f,
    val isRecording: Boolean = false,
    val hasPermission: Boolean = false
)

class MicRepository {

    private val _state = MutableStateFlow(MicState())
    val state: StateFlow<MicState> = _state.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val sampleRate = 44100
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
    )

    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    fun startRecording() {
        if (_state.value.isRecording) return
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        audioRecord?.startRecording()
        _state.value = _state.value.copy(isRecording = true, hasPermission = true)
        recordingJob = scope.launch {
            val buffer = ShortArray(bufferSize)
            while (isActive) {
                val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                if (read > 0) {
                    val maxAmp = buffer.take(read).maxOf { abs(it.toInt()) }
                    val db = if (maxAmp > 0) 20f * log10(maxAmp / 32768f) else -60f
                    _state.value = _state.value.copy(amplitudeDb = db.coerceAtLeast(-60f))
                }
            }
        }
    }

    fun stopRecording() {
        recordingJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        _state.value = _state.value.copy(isRecording = false, amplitudeDb = -60f)
    }

    fun release() = stopRecording()
}
