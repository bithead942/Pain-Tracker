package com.bithead942.paintracker

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class HistoryActivity : AppCompatActivity() {

    private lateinit var barChartView: BarChartView
    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val toolbar = findViewById<Toolbar>(R.id.historyToolbar)
        setSupportActionBar(toolbar)

        barChartView = findViewById(R.id.barChartView)
        listView = findViewById(R.id.historyListView)

        val recent = PainLogStore.getRecentLogs(this, 7)
        barChartView.setData(recent)

        val logs = PainLogStore.getAllLogs(this, descending = true)
        val items = logs.map { formatLog(it) }
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
    }

    private fun formatLog(log: PainLogStore.PainLog): String {
        val parts = log.entries.map { "${it.location} (${severityName(it.severity)})" }
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_history, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.action_back) {
            finish()
            true
        } else super.onOptionsItemSelected(item)
    }
}
