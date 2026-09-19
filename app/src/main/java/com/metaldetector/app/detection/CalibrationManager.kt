package com.metaldetector.app.detection

import kotlin.math.sqrt

/**
 * Result data from a completed calibration routine.
 * Baseline represents ambient geomagnetic magnitude (typical Earth field 30 - 60 µT).
 * Noise is sample standard deviation during calibration.
 * Threshold is dynamically computed as max(3.0 µT, noise * 4).
 */
data class CalibrationData(
    val baselineMagnitude: Float = 48.0f,
    val noiseStdDev: Float = 0.5f,
    val threshold: Float = 3.0f,
    val sampleCount: Int = 60,
    val timestamp: Long = System.currentTimeMillis()
)

class CalibrationManager(val requiredSamples: Int = 60) {

    private val samples = mutableListOf<Float>()
    var isCalibrating: Boolean = false
        private set

    /**
     * Starts collecting 60 samples.
     */
    fun startCalibration() {
        samples.clear()
        isCalibrating = true
    }

    /**
     * Feeds magnitude into calibration accumulator.
     * Returns CalibrationData when 60 samples are collected, or null if still gathering.
     */
    fun addSample(magnitude: Float): CalibrationData? {
        if (!isCalibrating) return null

        samples.add(magnitude)
        if (samples.size >= requiredSamples) {
            val count = samples.size
            val mean = samples.sum() / count
            val variance = samples.map { (it - mean) * (it - mean) }.sum() / count
            val noise = sqrt(variance)

            // As specified: threshold = max(3.0, noise * 4)
            val threshold = maxOf(3.0f, noise * 4.0f)

            isCalibrating = false
            return CalibrationData(
                baselineMagnitude = mean,
                noiseStdDev = noise,
                threshold = threshold,
                sampleCount = count
            )
        }
        return null
    }

    fun getProgress(): Float = (samples.size.toFloat() / requiredSamples.toFloat()).coerceIn(0f, 1f)
    fun getSamplesGathered(): Int = samples.size

    fun cancelCalibration() {
        samples.clear()
        isCalibrating = false
    }
}
