package com.bithead942.paintracker

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var reminderEnabledSwitch: Switch
    private lateinit var timePicker: TimePicker
    private lateinit var soundSwitch: Switch
    private lateinit var soundNameText: TextView
    private lateinit var soundSelectButton: Button
    private lateinit var vibrateSwitch: Switch
    private lateinit var intervalGroup: RadioGroup
    private lateinit var saveButton: Button

    private var selectedSoundUri: Uri? = null

    private val ringtonePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                selectedSoundUri = uri
                updateSoundName(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener { finish() }

        reminderEnabledSwitch = findViewById(R.id.reminderEnabledSwitch)
        timePicker = findViewById(R.id.timePicker)
        soundSwitch = findViewById(R.id.soundSwitch)
        soundNameText = findViewById(R.id.soundNameText)
        soundSelectButton = findViewById(R.id.soundSelectButton)
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

        selectedSoundUri = Uri.parse(settings.soundUri)
        updateSoundName(selectedSoundUri)

        updateUi(settings.remindersEnabled)

        reminderEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            updateUi(isChecked)
        }

        soundSwitch.setOnCheckedChangeListener { _, _ ->
            updateUi(reminderEnabledSwitch.isChecked)
        }

        soundSelectButton.setOnClickListener { openRingtonePicker() }

        saveButton.setOnClickListener { onSave(settings) }
    }

    private fun openRingtonePicker() {
        val currentUri = selectedSoundUri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select notification sound")
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        }
        ringtonePickerLauncher.launch(intent)
    }

    private fun updateSoundName(uri: Uri?) {
        val name = if (uri != null) {
            RingtoneManager.getRingtone(this, uri)?.getTitle(this) ?: getString(R.string.change_sound)
        } else {
            getString(R.string.change_sound)
        }
        soundNameText.text = name
    }

    private fun updateUi(enabled: Boolean) {
        val soundEnabled = soundSwitch.isChecked && enabled
        timePicker.isEnabled = enabled
        soundSwitch.isEnabled = enabled
        soundNameText.isEnabled = soundEnabled
        soundSelectButton.isEnabled = soundEnabled
        vibrateSwitch.isEnabled = enabled
        for (i in 0 until intervalGroup.childCount) {
            intervalGroup.getChildAt(i).isEnabled = enabled
        }
        timePicker.alpha = if (enabled) 1.0f else 0.5f
        soundSwitch.alpha = if (enabled) 1.0f else 0.5f
        soundNameText.alpha = if (soundEnabled) 1.0f else 0.5f
        soundSelectButton.alpha = if (soundEnabled) 1.0f else 0.5f
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
        settings.reminderHour = timePicker.hour
        settings.reminderMinute = timePicker.minute
        settings.soundEnabled = soundSwitch.isChecked
        selectedSoundUri?.let { settings.soundUri = it.toString() }
        settings.vibrationEnabled = vibrateSwitch.isChecked
        settings.reminderIntervalMinutes = when (intervalGroup.checkedRadioButtonId) {
            R.id.interval5 -> 5
            R.id.interval15 -> 15
            R.id.interval30 -> 30
            R.id.interval60 -> 60
            else -> 5
        }
        if (reminderEnabledSwitch.isChecked) {
            ReminderManager.reschedule(this)
        } else {
            ReminderManager.cancelAll(this)
        }
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        finish()
    }
}
