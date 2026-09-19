package com.metaldetector.app.detection

import com.metaldetector.app.sensor.MagneticReading
import kotlin.math.abs

/**
 * Detection state holding all metrics for real-time UI display.
 */
data class DetectionState(
    val rawMagnitude: Float = 0f,
    val filteredMagnitude: Float = 0f,
    val baseline: Float = 48.0f,
    val deviation: Float = 0f, // Absolute diff |filtered - baseline|
    val threshold: Float = 3.0f,
    val isTriggered: Boolean = false,
    val signalStrengthPercent: Float = 0f, // 0 to 100%
    val noise: Float = 0.5f,
    val alertLevel: AlertLevel = AlertLevel.NONE
)

enum class AlertLevel {
    NONE,
    WEAK,
    MODERATE,
    STRONG
}

/**
 * DetectionEngine implementing the complete algorithmic specification:
 * - 30 Hz sensor rate
 * - magnitude = sqrt(x*x + y*y + z*z)
 * - EMA filter with alpha = 0.2
 * - Calibration: baseline + noise over 60 samples
 * - Trigger condition: Signal (deviation) >= threshold
 * - Clear hysteresis condition: Signal <= threshold * 0.6
 * - Baseline tracking: updates slowly (drift adaptation) when no target is detected
 */
class DetectionEngine(
    private val filter: SignalFilter = SignalFilter(alpha = 0.2f),
    val calibrationManager: CalibrationManager = CalibrationManager(requiredSamples = 60)
) {
    var baseline: Float = 48.0f
        private set

    var threshold: Float = 3.0f
        private set

    var noise: Float = 0.5f
        private set

    private var isCurrentlyTriggered: Boolean = false

    // Baseline slow tracking adaptation rate (alpha = 0.005 for smooth drift rejection without suppressing real target)
    private val baselineAdaptationAlpha = 0.005f

    /**
     * Updates calibration parameters.
     */
    fun applyCalibration(cal: CalibrationData) {
        baseline = cal.baselineMagnitude
        noise = cal.noiseStdDev
        threshold = cal.threshold
        isCurrentlyTriggered = false
        filter.reset()
    }

    /**
     * Process a new magnetometer sample and return the updated detection state.
     */
    fun processSample(reading: MagneticReading): DetectionState {
        // If currently in calibration mode, feed the sample to CalibrationManager
        if (calibrationManager.isCalibrating) {
            val cal = calibrationManager.addSample(reading.magnitude)
            if (cal != null) {
                applyCalibration(cal)
            }
        }

        // Apply EMA filter
        val smoothed = filter.filter(reading.magnitude)

        // Calculate magnetic anomaly signal: deviation from ambient baseline
        val signal = abs(smoothed - baseline)

        // Hysteresis trigger logic:
        // Trigger if signal >= threshold
        // Clear if signal <= threshold * 0.6
        val clearThreshold = threshold * 0.6f

        if (!isCurrentlyTriggered) {
            if (signal >= threshold) {
                isCurrentlyTriggered = true
            } else {
                // When no target is detected, update baseline slowly to account for natural geomagnetic drift or room movement
                baseline = (1f - baselineAdaptationAlpha) * baseline + (baselineAdaptationAlpha * smoothed)
            }
        } else {
            if (signal <= clearThreshold) {
                isCurrentlyTriggered = false
            }
        }

        // Calculate relative signal percentage (0% at 0 deviation, 100% at ~5x threshold or saturation)
        val maxTargetDeviation = maxOf(threshold * 6.0f, 30.0f)
        val signalStrengthPercent = ((signal / maxTargetDeviation) * 100f).coerceIn(0f, 100f)

        val alertLevel = when {
            !isCurrentlyTriggered -> AlertLevel.NONE
            signal < threshold * 1.8f -> AlertLevel.WEAK
            signal < threshold * 3.5f -> AlertLevel.MODERATE
            else -> AlertLevel.STRONG
        }

        return DetectionState(
            rawMagnitude = reading.magnitude,
            filteredMagnitude = smoothed,
            baseline = baseline,
            deviation = signal,
            threshold = threshold,
            isTriggered = isCurrentlyTriggered,
            signalStrengthPercent = signalStrengthPercent,
            noise = noise,
            alertLevel = alertLevel
        )
    }

    fun resetState() {
        isCurrentlyTriggered = false
        filter.reset()
    }
}
