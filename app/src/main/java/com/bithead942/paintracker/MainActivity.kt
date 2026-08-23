package com.bithead942.paintracker

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bodyMapView: BodyMapView
    private lateinit var statusText: TextView
    private lateinit var submitButton: Button
    private lateinit var hamburgerButton: ImageButton
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    private val dateChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            loadToday()
        }
    }

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
        hamburgerButton = findViewById(R.id.hamburgerButton)

        hamburgerButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_history -> startActivity(Intent(this, HistoryActivity::class.java))
                R.id.action_settings -> startActivity(Intent(this, SettingsActivity::class.java))
                R.id.action_reset_history -> confirmResetHistory()
                R.id.action_share -> shareHistory()
                R.id.action_exit -> finish()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        bodyMapView = findViewById(R.id.bodyMapView)
        statusText = findViewById(R.id.statusText)
        submitButton = findViewById(R.id.submitButton)

        submitButton.setOnClickListener { onSubmit() }

        val settings = SettingsStore(this)
        if (settings.lastPurgeDate != PainLogStore.today()) {
            PainLogStore.purgeOldLogs(this)
            settings.lastPurgeDate = PainLogStore.today()
        }

        PainLogStore.seedTestData(this)
        loadToday()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> { }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> { }
                else -> requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(dateChangeReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
        })
        loadToday()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(dateChangeReceiver)
    }

    private fun shareHistory() {
        val today = PainLogStore.today()
        val subject = "Pain tracker history as of $today"
        val body = PainLogStore.buildHistoryText(this, 30)
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        startActivity(Intent.createChooser(intent, "Send email"))
    }

    private fun confirmResetHistory() {
        AlertDialog.Builder(this)
            .setTitle("Reset history")
            .setMessage("All log history will be deleted. Are you sure?")
            .setPositiveButton("Yes") { _, _ ->
                PainLogStore.clearHistory(this)
                loadToday()
                Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun loadToday() {
        val log = PainLogStore.getLog(this, PainLogStore.today())
        if (log != null) {
            statusText.text = if (log.savedAt.isNotEmpty()) {
                "${getString(R.string.log_recorded_today)} at ${log.savedAt}"
            } else {
                getString(R.string.log_recorded_today)
            }
            bodyMapView.setJoints(log.entries)
        } else {
            statusText.text = getString(R.string.no_log_today)
            bodyMapView.setJoints(emptyList())
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
        val saved = PainLogStore.getLog(this, date)
        statusText.text = if (saved?.savedAt?.isNotEmpty() == true) {
            "${getString(R.string.log_recorded_today)} at ${saved.savedAt}"
        } else {
            getString(R.string.log_recorded_today)
        }
        NotificationHelper.cancelNotification(this)
        ReminderManager.rescheduleForTomorrow(this)
        Toast.makeText(this, "Pain log saved", Toast.LENGTH_SHORT).show()
    }
}
