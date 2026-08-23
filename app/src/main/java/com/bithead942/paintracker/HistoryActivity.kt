package com.bithead942.paintracker

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity

class HistoryActivity : AppCompatActivity() {

    private lateinit var barChartView: BarChartView
    private lateinit var listView: ListView
    private lateinit var locationFilter: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        barChartView = findViewById(R.id.barChartView)
        listView = findViewById(R.id.historyListView)
        locationFilter = findViewById(R.id.locationFilter)

        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener { finish() }

        val locations = listOf("All") + BodyMapView.LOCATIONS
        locationFilter.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, locations).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        locationFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = if (position == 0) null else locations[position]
                loadHistory(selected)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        loadHistory(null)
    }

    private fun loadHistory(location: String?) {
        val days = 7
        val recent = PainLogStore.getRecentLogs(this, days, location)
        barChartView.setData(recent)

        val allLogs = PainLogStore.getAllLogs(this, descending = true)
        val logs = if (location == null) {
            allLogs
        } else {
            allLogs.filter { it.entries.any { e -> e.location == location } }
        }
        val items = logs.map { formatLog(it, location) }
        listView.adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                if (position % 2 == 1) {
                    view.setBackgroundColor(Color.parseColor("#F0F0F0"))
                } else {
                    view.setBackgroundColor(Color.TRANSPARENT)
                }
                return view
            }
        }
    }

    private fun formatLog(log: PainLogStore.PainLog, location: String?): String {
        val entries = if (location == null) log.entries else log.entries.filter { it.location == location }
        val parts = entries.map {
            val short = it.location
                .replace("Left ", "L ")
                .replace("Right ", "R ")
            "$short (${severityName(it.severity)})"
        }
        val pressurePart = if (log.pressure != -1) "${displayPressureLabel(log.pressureLabel)} (${log.pressure} hPa)" else null
        return when {
            pressurePart != null && parts.isEmpty() -> "${log.date}: $pressurePart - no pain recorded"
            pressurePart != null -> "${log.date}: $pressurePart - ${parts.joinToString(", ")}"
            parts.isEmpty() -> "${log.date}: no pain recorded"
            else -> "${log.date}: ${parts.joinToString(", ")}"
        }
    }

    private fun displayPressureLabel(label: String): String = when (label) {
        "low" -> "Low Pressure"
        "average" -> "Avg Pressure"
        "high" -> "High Pressure"
        else -> label
    }

    private fun severityName(sev: Int): String = when (sev) {
        1 -> "mild"
        2 -> "moderate"
        3 -> "severe"
        else -> "none"
    }
}
