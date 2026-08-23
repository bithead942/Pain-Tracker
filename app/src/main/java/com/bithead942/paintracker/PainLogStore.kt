package com.bithead942.paintracker

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object PainLogStore {
    private const val FILE_NAME = "pain_logs.json"
    private const val DATE_FORMAT = "yyyy-MM-dd"
    private const val TIME_FORMAT = "h:mm a"

    data class PainLog(val date: String, val savedAt: String, val entries: List<PainEntry>, val pressure: Int = -1, val pressureLabel: String = "")
    data class PainEntry(val location: String, val severity: Int)

    private val dateFormatter = SimpleDateFormat(DATE_FORMAT, Locale.US)
    private val timeFormatter = SimpleDateFormat(TIME_FORMAT, Locale.US)

    fun today(): String = dateFormatter.format(Date())

    fun saveLog(context: Context, date: String, entries: List<PainEntry>, pressure: Int = -1) {
        val logs = loadAll(context).toMutableMap()
        val time = timeFormatter.format(Date())
        val pressureLabel = when {
            pressure == -1 -> ""
            pressure < 1010 -> "low"
            pressure < 1020 -> "average"
            else -> "high"
        }
        logs[date] = PainLog(date, time, entries, pressure, pressureLabel)
        writeLogs(context, logs.values.toList())
    }

    fun getLog(context: Context, date: String): PainLog? = loadAll(context)[date]

    fun purgeOldLogs(context: Context, months: Int = 6) {
        val logs = loadAll(context).toMutableMap()
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -months)
        val cutoff = dateFormatter.format(cal.time)
        val toRemove = logs.keys.filter { it < cutoff }
        for (date in toRemove) logs.remove(date)
        writeLogs(context, logs.values.toList())
    }

    fun getRecentLogs(context: Context, days: Int = 7, location: String? = null): List<Pair<String, Int>> {
        val all = loadAll(context)
        val result = mutableListOf<Pair<String, Int>>()
        val cal = Calendar.getInstance()
        for (i in days - 1 downTo 0) {
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val date = dateFormatter.format(cal.time)
            val log = all[date]
            if (location == null) {
                val max = log?.entries?.maxOfOrNull { it.severity } ?: 0
                result.add(date to max)
            } else {
                val severity = log?.entries?.find { it.location == location }?.severity ?: 0
                result.add(date to severity)
            }
        }
        return result
    }

    fun clearHistory(context: Context) {
        val file = File(context.filesDir, FILE_NAME)
        if (file.exists()) file.delete()
    }

    fun buildHistoryText(context: Context, days: Int = 30): String {
        val all = getAllLogs(context, descending = true)
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -days) }
        val cutoff = dateFormatter.format(cal.time)
        val logs = all.filter { it.date >= cutoff }
        val sb = StringBuilder()
        sb.appendLine("Pain log history for the past $days days:")
        sb.appendLine()
        for (log in logs) {
            sb.appendLine(log.date)
            if (log.pressure != -1) {
                val label = when (log.pressureLabel) {
                    "low" -> "Low Pressure"
                    "average" -> "Avg Pressure"
                    "high" -> "High Pressure"
                    else -> log.pressureLabel
                }
                sb.appendLine("  Pressure: $label (${log.pressure} hPa)")
            }
            if (log.entries.isEmpty()) {
                sb.appendLine("  No pain recorded")
            } else {
                for (e in log.entries) {
                    val severity = when (e.severity) {
                        1 -> "Mild"
                        2 -> "Moderate"
                        3 -> "Severe"
                        else -> "None"
                    }
                    sb.appendLine("  - ${e.location}: $severity")
                }
            }
            sb.appendLine()
        }
        return sb.toString()
    }

    fun getAllLogs(context: Context, descending: Boolean = true): List<PainLog> {
        val all = loadAll(context).values.toList()
        return if (descending) all.sortedByDescending { it.date } else all.sortedBy { it.date }
    }

    private fun loadAll(context: Context): Map<String, PainLog> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyMap()
        val json = file.readText()
        if (json.isBlank()) return emptyMap()
        val obj = JSONObject(json)
        val map = mutableMapOf<String, PainLog>()
        for (key in obj.keys()) {
            val savedAt: String
            val arr: JSONArray
            val value = obj.get(key)
            if (value is JSONObject) {
                savedAt = value.optString("savedAt", "")
                arr = value.getJSONArray("entries")
            } else {
                arr = obj.getJSONArray(key)
                savedAt = ""
            }
            val entries = mutableListOf<PainEntry>()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                entries.add(PainEntry(item.getString("location"), item.getInt("severity")))
            }
            val pressure = if (value is JSONObject) value.optInt("pressure", -1) else -1
            val pressureLabel = if (value is JSONObject) value.optString("pressureLabel", "") else ""
            map[key] = PainLog(key, savedAt, entries, pressure, pressureLabel)
        }
        return map
    }

    private fun writeLogs(context: Context, logs: List<PainLog>) {
        val obj = JSONObject()
        for (log in logs) {
            val arr = JSONArray()
            for (e in log.entries) {
                val item = JSONObject()
                item.put("location", e.location)
                item.put("severity", e.severity)
                arr.put(item)
            }
            val logObj = JSONObject()
            logObj.put("savedAt", log.savedAt)
            logObj.put("entries", arr)
            logObj.put("pressure", log.pressure)
            logObj.put("pressureLabel", log.pressureLabel)
            obj.put(log.date, logObj)
        }
        File(context.filesDir, FILE_NAME).writeText(obj.toString())
    }
}
