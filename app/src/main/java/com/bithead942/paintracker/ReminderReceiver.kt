package com.bithead942.paintracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val settings = SettingsStore(context)
        when (intent.action) {
            ACTION_DAILY -> {
                if (SettingsStore(context).lastPurgeDate != PainLogStore.today()) {
                    PainLogStore.purgeOldLogs(context)
                    SettingsStore(context).lastPurgeDate = PainLogStore.today()
                }
                notify(context, settings)
                ReminderManager.reschedule(context)
                ReminderManager.scheduleFollowUp(context)
            }
            ACTION_FOLLOW_UP -> {
                notify(context, settings)
                ReminderManager.scheduleFollowUp(context)
            }
        }
    }

    private fun notify(context: Context, settings: SettingsStore) {
        val soundUri = if (settings.soundEnabled && settings.soundUri.isNotEmpty()) Uri.parse(settings.soundUri) else null
        NotificationHelper.createNotificationChannel(context, settings.soundEnabled, settings.vibrationEnabled, soundUri)
        NotificationHelper.showNotification(context, settings.soundEnabled, settings.vibrationEnabled, soundUri)
    }

    companion object {
        const val ACTION_DAILY = "com.bithead942.paintracker.ACTION_DAILY"
        const val ACTION_FOLLOW_UP = "com.bithead942.paintracker.ACTION_FOLLOW_UP"
    }
}
