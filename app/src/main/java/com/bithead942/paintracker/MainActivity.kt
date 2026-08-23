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
import android.location.LocationManager
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
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
    private lateinit var pressureLabel: TextView
    private lateinit var pressureText: TextView
    private lateinit var hamburgerButton: ImageButton

    private var currentPressure: Int? = null
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

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            fetchPressure()
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
        pressureLabel = findViewById(R.id.pressureLabel)
        pressureText = findViewById(R.id.pressureText)

        submitButton.setOnClickListener { onSubmit() }

        if (!hasLocationPermission()) {
            locationPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }

        val settings = SettingsStore(this)
        if (!settings.hasTestData) {
            PainLogStore.regenerateTestData(this, 31)
            settings.hasTestData = true
        }

        if (settings.lastPurgeDate != PainLogStore.today()) {
            PainLogStore.purgeOldLogs(this)
            settings.lastPurgeDate = PainLogStore.today()
        }

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
        if (hasLocationPermission()) fetchPressure()
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

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun fetchPressure() {
        try {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (location == null) {
                runOnUiThread {
                    pressureLabel.text = ""
                    pressureText.text = getString(R.string.pressure)
                }
                return
            }
            val url = URL(
                "https://api.open-meteo.com/v1/forecast?latitude=${location.latitude}&longitude=${location.longitude}&current=surface_pressure"
            )
            Thread {
                try {
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    connection.disconnect()
                    val json = org.json.JSONObject(response)
                    val current = json.getJSONObject("current")
                    val pressure = current.getDouble("surface_pressure")
                    val color = when {
                        pressure < 1010 -> android.graphics.Color.GREEN
                        pressure < 1020 -> android.graphics.Color.YELLOW
                        else -> android.graphics.Color.RED
                    }
                    val pressureInt = pressure.toInt()
                    currentPressure = pressureInt
                    val label = when {
                        pressure < 1010 -> "Low Pressure"
                        pressure < 1020 -> "Avg Pressure"
                        else -> "High Pressure"
                    }
                    runOnUiThread {
                        pressureLabel.text = label
                        pressureText.text = "$pressureInt hPa"
                        pressureLabel.setTextColor(color)
                        pressureText.setTextColor(color)
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        pressureLabel.text = ""
                        pressureText.text = getString(R.string.pressure)
                    }
                }
            }.start()
        } catch (e: Exception) {
            pressureText.text = getString(R.string.pressure)
        }
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
        PainLogStore.saveLog(this, date, entries, currentPressure ?: -1)
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
