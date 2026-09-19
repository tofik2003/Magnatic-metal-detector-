package com.metaldetector.app.detection

/**
 * Exponential Moving Average (EMA) filter.
 * Formula: S_t = alpha * Y_t + (1 - alpha) * S_{t-1}
 * With alpha = 0.2 (per specification) to smooth high-frequency magnetometer jitter
 * while maintaining responsive detection.
 */
class SignalFilter(val alpha: Float = 0.2f) {
    private var filteredValue: Float? = null

    /**
     * Updates the filter with a new raw measurement and returns smoothed output.
     */
    fun filter(raw: Float): Float {
        val current = filteredValue
        val result = if (current == null) {
            raw
        } else {
            alpha * raw + (1.0f - alpha) * current
        }
        filteredValue = result
        return result
    }

    /**
     * Resets the filter's memory.
     */
    fun reset() {
        filteredValue = null
    }

    val current: Float? get() = filteredValue
}
