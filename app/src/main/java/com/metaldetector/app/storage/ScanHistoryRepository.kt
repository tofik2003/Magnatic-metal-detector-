package com.metaldetector.app.storage

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Scan session record saved to local storage.
 */
data class ScanRecord(
    val id: String,
    val title: String,
    val type: String, // "Live Detector", "Wall Scan", "Grass Scan"
    val timestamp: Long,
    val maxMicroTesla: Float,
    val avgMicroTesla: Float,
    val targetsDetectedCount: Int,
    val durationSeconds: Int,
    val notes: String = ""
)

class ScanHistoryRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("metal_detector_history", Context.MODE_PRIVATE)
    private val keyRecords = "scan_records"

    fun getAllRecords(): List<ScanRecord> {
        val jsonStr = prefs.getString(keyRecords, null) ?: return emptyList()
        val list = mutableListOf<ScanRecord>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ScanRecord(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        type = obj.getString("type"),
                        timestamp = obj.getLong("timestamp"),
                        maxMicroTesla = obj.getDouble("maxMicroTesla").toFloat(),
                        avgMicroTesla = obj.getDouble("avgMicroTesla").toFloat(),
                        targetsDetectedCount = obj.getInt("targetsDetectedCount"),
                        durationSeconds = obj.getInt("durationSeconds"),
                        notes = obj.optString("notes", "")
                    )
                )
            }
        } catch (_: Exception) {}
        return list.sortedByDescending { it.timestamp }
    }

    fun saveRecord(record: ScanRecord) {
        val current = getAllRecords().toMutableList()
        current.add(0, record)
        persist(current)
    }

    fun deleteRecord(id: String) {
        val filtered = getAllRecords().filter { it.id != id }
        persist(filtered)
    }

    fun clearAll() {
        prefs.edit().remove(keyRecords).apply()
    }

    private fun persist(list: List<ScanRecord>) {
        val array = JSONArray()
        list.take(100).forEach { record ->
            val obj = JSONObject()
            obj.put("id", record.id)
            obj.put("title", record.title)
            obj.put("type", record.type)
            obj.put("timestamp", record.timestamp)
            obj.put("maxMicroTesla", record.maxMicroTesla.toDouble())
            obj.put("avgMicroTesla", record.avgMicroTesla.toDouble())
            obj.put("targetsDetectedCount", record.targetsDetectedCount)
            obj.put("durationSeconds", record.durationSeconds)
            obj.put("notes", record.notes)
            array.put(obj)
        }
        prefs.edit().putString(keyRecords, array.toString()).apply()
    }
}
