package com.bithead942.paintracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var bodyMapView: BodyMapView
    private lateinit var statusText: TextView
    private lateinit var submitButton: Button

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Notification permission is required for reminders", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.mainToolbar)
        setSupportActionBar(toolbar)

        bodyMapView = findViewById(R.id.bodyMapView)
        statusText = findViewById(R.id.statusText)
        submitButton = findViewById(R.id.submitButton)

        submitButton.setOnClickListener { onSubmit() }

        loadToday()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> { }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> { }
                else -> requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun loadToday() {
        val log = PainLogStore.getLog(this, PainLogStore.today())
        if (log != null) {
            statusText.text = getString(R.string.log_recorded_today)
            bodyMapView.setJoints(log.entries)
        } else {
            statusText.text = getString(R.string.no_log_today)
        }
    }

    private fun onSubmit() {
        val entries = bodyMapView.getActiveEntries()
        PainLogStore.saveLog(this, PainLogStore.today(), entries)
        statusText.text = getString(R.string.log_recorded_today)
        ReminderManager.cancelFollowUp(this)
        Toast.makeText(this, "Pain log saved", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_history -> {
                startActivity(Intent(this, HistoryActivity::class.java))
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
