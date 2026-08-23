package com.bithead942.paintracker

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class HistoryActivity : AppCompatActivity() {

    private lateinit var barChartView: BarChartView
    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        barChartView = findViewById(R.id.barChartView)
        listView = findViewById(R.id.historyListView)

        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener { finish() }

        val recent = PainLogStore.getRecentLogs(this, 7)
        barChartView.setData(recent)

        val logs = PainLogStore.getAllLogs(this, descending = true)
        val items = logs.map { formatLog(it) }
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
    }

    private fun formatLog(log: PainLogStore.PainLog): String {
        val parts = log.entries.map {
            val short = it.location
                .replace("Left ", "L ")
                .replace("Right ", "R ")
            "$short (${severityName(it.severity)})"
        }
        return if (parts.isEmpty()) {
            "${log.date}: no pain recorded"
        } else {
            "${log.date}: ${parts.joinToString(", ")}"
        }
    }

    private fun severityName(sev: Int): String = when (sev) {
        1 -> "mild"
        2 -> "moderate"
        3 -> "severe"
        else -> "none"
    }
}
