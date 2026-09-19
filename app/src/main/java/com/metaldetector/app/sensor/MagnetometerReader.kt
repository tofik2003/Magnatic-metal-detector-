package com.metaldetector.app.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Data class representing a 3-axis magnetic field reading in microteslas (µT).
 */
data class MagneticReading(
    val x: Float,
    val y: Float,
    val z: Float,
    val magnitude: Float,
    val accuracy: Int,
    val timestamp: Long = System.currentTimeMillis()
)

class MagnetometerReader(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val magnetometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    /**
     * Checks if the device has a hardware magnetometer available.
     */
    fun isSensorAvailable(): Boolean = magnetometer != null

    /**
     * Retrieves sensor details (name, vendor, maxRange, resolution).
     */
    fun getSensorInfo(): String {
        return magnetometer?.let {
            "${it.name} by ${it.vendor} (Max: ${it.maximumRange} µT, Res: ${it.resolution} µT)"
        } ?: "No magnetic field sensor found on this device"
    }

    /**
     * Emits continuous magnetic field readings.
     * Uses 30Hz target rate (~33,333 microseconds per sample) as per spec.
     */
    fun getReadingsFlow(): Flow<MagneticReading> = callbackFlow {
        if (magnetometer == null || sensorManager == null) {
            close()
            return@callbackFlow
        }

        val samplingPeriodUs = 1_000_000 / 30 // ~33,333 µs = 30 Hz

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    val magnitude = kotlin.math.sqrt(x * x + y * y + z * z)

                    trySend(
                        MagneticReading(
                            x = x,
                            y = y,
                            z = z,
                            magnitude = magnitude,
                            accuracy = event.accuracy
                        )
                    )
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Sensor accuracy changes (0=unreliable, 1=low, 2=medium, 3=high)
            }
        }

        sensorManager.registerListener(listener, magnetometer, samplingPeriodUs)

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
