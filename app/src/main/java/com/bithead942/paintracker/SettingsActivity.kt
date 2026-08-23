package com.bithead942.paintracker

import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var reminderEnabledSwitch: Switch
    private lateinit var timePicker: TimePicker
    private lateinit var soundSwitch: Switch
    private lateinit var vibrateSwitch: Switch
    private lateinit var intervalGroup: RadioGroup
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener { finish() }

        reminderEnabledSwitch = findViewById(R.id.reminderEnabledSwitch)
        timePicker = findViewById(R.id.timePicker)
        soundSwitch = findViewById(R.id.soundSwitch)
        vibrateSwitch = findViewById(R.id.vibrateSwitch)
        intervalGroup = findViewById(R.id.intervalGroup)
        saveButton = findViewById(R.id.saveButton)

        val settings = SettingsStore(this)
        reminderEnabledSwitch.isChecked = settings.remindersEnabled
        timePicker.hour = settings.reminderHour
        timePicker.minute = settings.reminderMinute
        soundSwitch.isChecked = settings.soundEnabled
        vibrateSwitch.isChecked = settings.vibrationEnabled
        setIntervalSelection(settings.reminderIntervalMinutes)

        updateUi(settings.remindersEnabled)

        reminderEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            updateUi(isChecked)
        }

        saveButton.setOnClickListener { onSave(settings) }
    }

    private fun updateUi(enabled: Boolean) {
        timePicker.isEnabled = enabled
        soundSwitch.isEnabled = enabled
        vibrateSwitch.isEnabled = enabled
        for (i in 0 until intervalGroup.childCount) {
            intervalGroup.getChildAt(i).isEnabled = enabled
        }
        timePicker.alpha = if (enabled) 1.0f else 0.5f
        soundSwitch.alpha = if (enabled) 1.0f else 0.5f
        vibrateSwitch.alpha = if (enabled) 1.0f else 0.5f
        intervalGroup.alpha = if (enabled) 1.0f else 0.5f
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
        settings.remindersEnabled = reminderEnabledSwitch.isChecked
        if (reminderEnabledSwitch.isChecked) {
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
        } else {
            ReminderManager.cancelAll(this)
        }
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        finish()
    }
}
