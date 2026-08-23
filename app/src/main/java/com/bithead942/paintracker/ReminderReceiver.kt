package com.bithead942.paintracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val settings = SettingsStore(context)
        when (intent.action) {
            ACTION_DAILY -> {
                notify(context, settings)
                ReminderManager.scheduleFollowUp(context)
            }
            ACTION_FOLLOW_UP -> {
                notify(context, settings)
                ReminderManager.scheduleFollowUp(context)
            }
        }
    }

    private fun notify(context: Context, settings: SettingsStore) {
        NotificationHelper.createNotificationChannel(context, settings.soundEnabled, settings.vibrationEnabled)
        NotificationHelper.showNotification(context, settings.soundEnabled, settings.vibrationEnabled)
    }

    companion object {
        const val ACTION_DAILY = "com.bithead942.paintracker.ACTION_DAILY"
        const val ACTION_FOLLOW_UP = "com.bithead942.paintracker.ACTION_FOLLOW_UP"
    }
}
