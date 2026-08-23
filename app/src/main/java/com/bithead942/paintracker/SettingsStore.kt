package com.bithead942.paintracker

import android.content.Context
import android.content.SharedPreferences

class SettingsStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var reminderHour: Int
        get() = prefs.getInt(HOUR, 9)
        set(value) = prefs.edit().putInt(HOUR, value).apply()

    var reminderMinute: Int
        get() = prefs.getInt(MINUTE, 0)
        set(value) = prefs.edit().putInt(MINUTE, value).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean(SOUND, true)
        set(value) = prefs.edit().putBoolean(SOUND, value).apply()

    var vibrationEnabled: Boolean
        get() = prefs.getBoolean(VIBRATE, true)
        set(value) = prefs.edit().putBoolean(VIBRATE, value).apply()

    var reminderIntervalMinutes: Int
        get() = prefs.getInt(INTERVAL, 5)
        set(value) = prefs.edit().putInt(INTERVAL, value).apply()

    var remindersEnabled: Boolean
        get() = prefs.getBoolean(REMINDERS, true)
        set(value) = prefs.edit().putBoolean(REMINDERS, value).apply()

    companion object {
        private const val PREFS_NAME = "pain_tracker_settings"
        private const val HOUR = "reminder_hour"
        private const val MINUTE = "reminder_minute"
        private const val SOUND = "sound_enabled"
        private const val VIBRATE = "vibration_enabled"
        private const val INTERVAL = "reminder_interval"
        private const val REMINDERS = "reminders_enabled"
    }
}
