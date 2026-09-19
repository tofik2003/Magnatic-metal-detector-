package com.metaldetector.app.detection

import com.metaldetector.app.sensor.MagneticReading
import kotlin.math.abs
import kotlin.math.pow

/**
 * Enhanced detection state holding microtesla metrics, depth estimations,
 * 3-axis dominant direction vectors, and alert classifications.
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
    val alertLevel: AlertLevel = AlertLevel.NONE,
    // Advanced features:
    val estimatedDistanceCm: Float = 20.0f, // Proximity model
    val depthCategory: String = "Clear", // "Direct Contact (<2cm)", "Shallow (2-6cm)", "Medium (6-12cm)", "Deep/Faint (>12cm)"
    val dominantAxis: DominantAxis = DominantAxis.BALANCED,
    val signalGradient: Float = 0f // rate of change d(µT)/dt
)

enum class DominantAxis(val label: String) {
    X_AXIS("Lateral (X)"),
    Y_AXIS("Longitudinal (Y)"),
    Z_AXIS("Perpendicular / Depth (Z)"),
    BALANCED("Omnidirectional")
}

enum class AlertLevel {
    NONE,
    WEAK,
    MODERATE,
    STRONG,
    CRITICAL
}

/**
 * DetectionEngine implementing:
 * - 30 Hz sensor rate
 * - magnitude = sqrt(x*x + y*y + z*z)
 * - EMA filter with alpha = 0.2
 * - Calibration: baseline + noise over 60 samples
 * - Trigger condition: Signal (deviation) >= threshold
 * - Clear hysteresis condition: Signal <= threshold * 0.6
 * - Baseline tracking: updates slowly when no target is detected
 * - Proximity Distance Estimation: modeled via magnetic dipole decay r ~ (M / delta_B)^(1/3)
 * - 3D Directional Vector Anomaly analysis
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
    private val baselineAdaptationAlpha = 0.005f
    private var lastSmoothedMagnitude: Float = 48.0f

    fun applyCalibration(cal: CalibrationData) {
        baseline = cal.baselineMagnitude
        noise = cal.noiseStdDev
        threshold = cal.threshold
        isCurrentlyTriggered = false
        filter.reset()
    }

    fun processSample(reading: MagneticReading): DetectionState {
        if (calibrationManager.isCalibrating) {
            val cal = calibrationManager.addSample(reading.magnitude)
            if (cal != null) {
                applyCalibration(cal)
            }
        }

        // Apply EMA filter
        val smoothed = filter.filter(reading.magnitude)
        val gradient = smoothed - lastSmoothedMagnitude
        lastSmoothedMagnitude = smoothed

        // Magnetic anomaly deviation from ambient baseline
        val signal = abs(smoothed - baseline)

        // Hysteresis trigger logic:
        val clearThreshold = threshold * 0.6f

        if (!isCurrentlyTriggered) {
            if (signal >= threshold) {
                isCurrentlyTriggered = true
            } else {
                // Adaptive drift cancellation
                baseline = (1f - baselineAdaptationAlpha) * baseline + (baselineAdaptationAlpha * smoothed)
            }
        } else {
            if (signal <= clearThreshold) {
                isCurrentlyTriggered = false
            }
        }

        // Signal percentage (0% to 100%)
        val maxTargetDeviation = maxOf(threshold * 8.0f, 40.0f)
        val signalStrengthPercent = ((signal / maxTargetDeviation) * 100f).coerceIn(0f, 100f)

        // Advanced Proximity Distance Estimation (Dipole approximation: B ~ 1 / r^3 => r ~ (K / B)^(1/3))
        // Calibrated against typical ferrous objects (rebar, screws, pipes)
        val estimatedDistanceCm = if (signal < 0.5f) {
            25.0f
        } else {
            val approxDistance = (1200.0f / signal).pow(1.0f / 3.0f) * 1.8f
            approxDistance.coerceIn(0.5f, 25.0f)
        }

        val depthCategory = when {
            !isCurrentlyTriggered -> "Clear (No Anomaly)"
            estimatedDistanceCm <= 2.5f -> "Direct Contact (<2.5 cm)"
            estimatedDistanceCm <= 6.5f -> "Shallow (2.5 - 6.5 cm)"
            estimatedDistanceCm <= 12.0f -> "Moderate (6.5 - 12 cm)"
            else -> "Deep / Peripheral (>12 cm)"
        }

        // Dominant axis determination
        val absX = abs(reading.x)
        val absY = abs(reading.y)
        val absZ = abs(reading.z)
        val dominantAxis = when {
            absZ > absX * 1.4f && absZ > absY * 1.4f -> DominantAxis.Z_AXIS
            absY > absX * 1.3f && absY > absZ * 1.3f -> DominantAxis.Y_AXIS
            absX > absY * 1.3f && absX > absZ * 1.3f -> DominantAxis.X_AXIS
            else -> DominantAxis.BALANCED
        }

        val alertLevel = when {
            !isCurrentlyTriggered -> AlertLevel.NONE
            signalStrengthPercent > 80f -> AlertLevel.CRITICAL
            signalStrengthPercent > 50f -> AlertLevel.STRONG
            signalStrengthPercent > 25f -> AlertLevel.MODERATE
            else -> AlertLevel.WEAK
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
            alertLevel = alertLevel,
            estimatedDistanceCm = estimatedDistanceCm,
            depthCategory = depthCategory,
            dominantAxis = dominantAxis,
            signalGradient = gradient
        )
    }

    fun resetState() {
        isCurrentlyTriggered = false
        filter.reset()
    }
}
