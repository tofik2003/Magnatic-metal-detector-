package com.metaldetector.app.storage

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Saved pinned detection target pin with spatial metadata and notes.
 */
data class DetectionPin(
    val id: String,
    val title: String,
    val surfaceType: String, // "Drywall", "Floor", "Lawn", "Concrete"
    val timestamp: Long,
    val peakMicroTesla: Float,
    val estimatedDistanceCm: Float,
    val relativeDepthCategory: String, // "Surface (0-3 cm)", "Shallow (3-8 cm)", "Deep (8-15 cm)"
    val notes: String = "",
    val gridPosition: String? = null // e.g. "Row 3, Col 2"
)

class SavedPinsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("metal_detector_pins", Context.MODE_PRIVATE)
    private val keyPins = "saved_detection_pins"

    fun getAllPins(): List<DetectionPin> {
        val jsonStr = prefs.getString(keyPins, null) ?: return emptyList()
        val list = mutableListOf<DetectionPin>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    DetectionPin(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        surfaceType = obj.getString("surfaceType"),
                        timestamp = obj.getLong("timestamp"),
                        peakMicroTesla = obj.getDouble("peakMicroTesla").toFloat(),
                        estimatedDistanceCm = obj.getDouble("estimatedDistanceCm").toFloat(),
                        relativeDepthCategory = obj.getString("relativeDepthCategory"),
                        notes = obj.optString("notes", ""),
                        gridPosition = if (obj.has("gridPosition")) obj.getString("gridPosition") else null
                    )
                )
            }
        } catch (_: Exception) {}
        return list.sortedByDescending { it.timestamp }
    }

    fun savePin(pin: DetectionPin) {
        val current = getAllPins().toMutableList()
        current.add(0, pin)
        persist(current)
    }

    fun deletePin(id: String) {
        val filtered = getAllPins().filter { it.id != id }
        persist(filtered)
    }

    fun clearAllPins() {
        prefs.edit().remove(keyPins).apply()
    }

    private fun persist(list: List<DetectionPin>) {
        val arr = JSONArray()
        list.take(200).forEach { pin ->
            val obj = JSONObject()
            obj.put("id", pin.id)
            obj.put("title", pin.title)
            obj.put("surfaceType", pin.surfaceType)
            obj.put("timestamp", pin.timestamp)
            obj.put("peakMicroTesla", pin.peakMicroTesla.toDouble())
            obj.put("estimatedDistanceCm", pin.estimatedDistanceCm.toDouble())
            obj.put("relativeDepthCategory", pin.relativeDepthCategory)
            obj.put("notes", pin.notes)
            if (pin.gridPosition != null) {
                obj.put("gridPosition", pin.gridPosition)
            }
            arr.put(obj)
        }
        prefs.edit().putString(keyPins, arr.toString()).apply()
    }
}
