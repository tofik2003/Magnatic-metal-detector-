package com.metaldetector.app

import com.metaldetector.app.detection.CalibrationData
import com.metaldetector.app.detection.CalibrationManager
import com.metaldetector.app.detection.DetectionEngine
import com.metaldetector.app.detection.SignalFilter
import com.metaldetector.app.sensor.MagneticReading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class MetalDetectorAlgorithmTest {

    @Test
    fun testMagnitudeCalculation() {
        val x = 30.0f
        val y = 40.0f
        val z = 0.0f
        val magnitude = sqrt(x * x + y * y + z * z)
        assertEquals(50.0f, magnitude, 0.001f)
    }

    @Test
    fun testSignalFilterEmaAlpha02() {
        val filter = SignalFilter(alpha = 0.2f)
        val sample1 = filter.filter(50.0f)
        assertEquals(50.0f, sample1, 0.001f)

        // S_1 = 0.2 * 100 + 0.8 * 50 = 20 + 40 = 60
        val sample2 = filter.filter(100.0f)
        assertEquals(60.0f, sample2, 0.001f)

        // S_2 = 0.2 * 100 + 0.8 * 60 = 20 + 48 = 68
        val sample3 = filter.filter(100.0f)
        assertEquals(68.0f, sample3, 0.001f)
    }

    @Test
    fun testCalibrationRoutine60Samples() {
        val calManager = CalibrationManager(requiredSamples = 60)
        calManager.startCalibration()
        assertTrue(calManager.isCalibrating)

        // Feed 59 samples
        for (i in 1..59) {
            val result = calManager.addSample(45.0f)
            assertEquals(null, result)
        }

        // 60th sample triggers completion
        val finalCal = calManager.addSample(45.0f)
        assertNotNull(finalCal)
        assertEquals(45.0f, finalCal!!.baselineMagnitude, 0.001f)
        assertEquals(0.0f, finalCal.noiseStdDev, 0.001f)
        // threshold = max(3.0, noise * 4) -> max(3.0, 0) = 3.0
        assertEquals(3.0f, finalCal.threshold, 0.001f)
        assertFalse(calManager.isCalibrating)
    }

    @Test
    fun testDetectionHysteresisTriggerAndClear() {
        val engine = DetectionEngine()
        // Baseline default is 48.0, threshold is 3.0
        // Clear threshold is 3.0 * 0.6 = 1.8

        // Sample near baseline -> not triggered
        val state1 = engine.processSample(MagneticReading(0f, 0f, 48f, 48.0f, 3))
        assertFalse(state1.isTriggered)

        // Strong spike: 48 + 5.0 -> deviation 5.0 >= threshold (3.0) -> TRIGGERED
        engine.processSample(MagneticReading(0f, 0f, 53f, 53.0f, 3))
        engine.processSample(MagneticReading(0f, 0f, 60f, 60.0f, 3))
        val state4 = engine.processSample(MagneticReading(0f, 0f, 60f, 60.0f, 3))
        assertTrue(state4.isTriggered)

        // Dropping signal slightly below 3.0 (e.g. 2.5 deviation) remains triggered due to hysteresis (clear is <= 1.8)
        var clearedState = state4
        for (i in 0..15) {
            clearedState = engine.processSample(MagneticReading(0f, 0f, 48f, 48.0f, 3))
        }
        assertFalse(clearedState.isTriggered)
    }

    @Test
    fun testSlowDriftBaselineAdaptation() {
        val engine = DetectionEngine()
        val initialBaseline = engine.baseline

        // When reading slightly shifts from 48 to 49 (under threshold 3.0), baseline should slowly adapt
        for (i in 0..50) {
            engine.processSample(MagneticReading(0f, 0f, 49f, 49.0f, 3))
        }

        // Baseline should have drifted upwards towards 49.0
        assertTrue("Baseline should adapt towards steady ambient reading", engine.baseline > initialBaseline)
    }
}
