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

    data class PainLog(val date: String, val savedAt: String, val entries: List<PainEntry>)
    data class PainEntry(val location: String, val severity: Int)

    private val dateFormatter = SimpleDateFormat(DATE_FORMAT, Locale.US)
    private val timeFormatter = SimpleDateFormat(TIME_FORMAT, Locale.US)

    fun today(): String = dateFormatter.format(Date())

    fun saveLog(context: Context, date: String, entries: List<PainEntry>) {
        val logs = loadAll(context).toMutableMap()
        val time = timeFormatter.format(Date())
        logs[date] = PainLog(date, time, entries)
        writeLogs(context, logs.values.toList())
    }

    fun getLog(context: Context, date: String): PainLog? = loadAll(context)[date]

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
                val entry = log?.entries?.find { it.location == location }
                if (entry != null) {
                    result.add(date to entry.severity)
                }
            }
        }
        return result
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
            map[key] = PainLog(key, savedAt, entries)
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
            obj.put(log.date, logObj)
        }
        File(context.filesDir, FILE_NAME).writeText(obj.toString())
    }
}
