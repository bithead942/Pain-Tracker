package com.bithead942.paintracker

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class SettingsActivity : AppCompatActivity() {

    private lateinit var timePicker: TimePicker
    private lateinit var soundSwitch: Switch
    private lateinit var vibrateSwitch: Switch
    private lateinit var intervalGroup: RadioGroup
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.settingsToolbar)
        setSupportActionBar(toolbar)

        timePicker = findViewById(R.id.timePicker)
        soundSwitch = findViewById(R.id.soundSwitch)
        vibrateSwitch = findViewById(R.id.vibrateSwitch)
        intervalGroup = findViewById(R.id.intervalGroup)
        saveButton = findViewById(R.id.saveButton)

        val settings = SettingsStore(this)
        timePicker.hour = settings.reminderHour
        timePicker.minute = settings.reminderMinute
        soundSwitch.isChecked = settings.soundEnabled
        vibrateSwitch.isChecked = settings.vibrationEnabled
        setIntervalSelection(settings.reminderIntervalMinutes)

        saveButton.setOnClickListener { onSave(settings) }
    }

    private fun setIntervalSelection(minutes: Int) {
        val id = when (minutes) {
            5 -> R.id.interval5
            15 -> R.id.interval15
            30 -> R.id.interval30
            60 -> R.id.interval60
            else -> R.id.interval5
        }
        intervalGroup.check(id)
    }

    private fun onSave(settings: SettingsStore) {
        settings.reminderHour = timePicker.hour
        settings.reminderMinute = timePicker.minute
        settings.soundEnabled = soundSwitch.isChecked
        settings.vibrationEnabled = vibrateSwitch.isChecked
        settings.reminderIntervalMinutes = when (intervalGroup.checkedRadioButtonId) {
            R.id.interval5 -> 5
            R.id.interval15 -> 15
            R.id.interval30 -> 30
            R.id.interval60 -> 60
            else -> 5
        }
        ReminderManager.reschedule(this)
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_settings, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.action_back) {
            finish()
            true
        } else super.onOptionsItemSelected(item)
    }
}
