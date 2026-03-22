package com.example.magicapp.features.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CompassState(
    val azimuthDeg: Float = 0f,
    val cardinalDirection: String = "N",
    val hasHardware: Boolean = true
)

class CompassRepository(private val context: Context) {

    private val _state = MutableStateFlow(CompassState())
    val state: StateFlow<CompassState> = _state.asStateFlow()

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private val listener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        override fun onSensorChanged(event: SensorEvent) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
            val normalised = (azimuth + 360) % 360
            _state.value = CompassState(
                azimuthDeg = normalised,
                cardinalDirection = toCardinal(normalised),
                hasHardware = true
            )
        }
    }

    fun start() {
        if (rotationSensor == null) {
            _state.value = CompassState(hasHardware = false)
            return
        }
        sensorManager.registerListener(
            listener, rotationSensor, SensorManager.SENSOR_DELAY_UI
        )
    }

    fun stop() {
        sensorManager.unregisterListener(listener)
    }

    companion object {
        fun toCardinal(deg: Float): String = when {
            deg < 22.5 || deg >= 337.5 -> "N"
            deg < 67.5 -> "NE"
            deg < 112.5 -> "E"
            deg < 157.5 -> "SE"
            deg < 202.5 -> "S"
            deg < 247.5 -> "SW"
            deg < 292.5 -> "W"
            else -> "NW"
        }
    }
}
