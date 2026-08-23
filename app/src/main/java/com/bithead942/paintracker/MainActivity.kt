package com.bithead942.paintracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bodyMapView: BodyMapView
    private lateinit var statusText: TextView
    private lateinit var submitButton: Button
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

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

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        val toolbar = findViewById<Toolbar>(R.id.mainToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        toolbar.setNavigationIcon(R.drawable.ic_menu)
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_history -> startActivity(Intent(this, HistoryActivity::class.java))
                R.id.action_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

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
        val today = PainLogStore.today()
        if (PainLogStore.getLog(this, today) != null) {
            AlertDialog.Builder(this)
                .setTitle("Overwrite today?")
                .setMessage("A log for today already exists. Do you want to overwrite it?")
                .setPositiveButton("OK") { _, _ ->
                    saveLog(today, entries)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            saveLog(today, entries)
        }
    }

    private fun saveLog(date: String, entries: List<PainLogStore.PainEntry>) {
        PainLogStore.saveLog(this, date, entries)
        statusText.text = getString(R.string.log_recorded_today)
        ReminderManager.cancelFollowUp(this)
        Toast.makeText(this, "Pain log saved", Toast.LENGTH_SHORT).show()
    }
}
