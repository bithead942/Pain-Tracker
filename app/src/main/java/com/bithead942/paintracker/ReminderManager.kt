package com.bithead942.paintracker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.*

object ReminderManager {
    private const val DAILY_REQUEST = 100
    private const val FOLLOW_UP_REQUEST = 101

    fun reschedule(context: Context) {
        val settings = SettingsStore(context)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelDaily(context)
        cancelFollowUp(context)

        val first = nextDailyTime(settings.reminderHour, settings.reminderMinute)
        val dailyIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_DAILY
        }
        val dailyPending = PendingIntent.getBroadcast(
            context, DAILY_REQUEST, dailyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, first.timeInMillis, dailyPending)
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, first.timeInMillis, dailyPending)
        }
    }

    fun rescheduleForTomorrow(context: Context) {
        val settings = SettingsStore(context)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelDaily(context)
        cancelFollowUp(context)

        val first = nextDailyTime(settings.reminderHour, settings.reminderMinute)
        val now = Calendar.getInstance()
        if (first.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) {
            first.add(Calendar.DAY_OF_YEAR, 1)
        }

        val dailyIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_DAILY
        }
        val dailyPending = PendingIntent.getBroadcast(
            context, DAILY_REQUEST, dailyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, first.timeInMillis, dailyPending)
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, first.timeInMillis, dailyPending)
        }
    }

    fun scheduleFollowUp(context: Context) {
        val settings = SettingsStore(context)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val interval = settings.reminderIntervalMinutes * 60_000L
        val followUp = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_FOLLOW_UP
        }
        val pending = PendingIntent.getBroadcast(
            context, FOLLOW_UP_REQUEST, followUp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val trigger = System.currentTimeMillis() + interval

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
        } else {
            am.set(AlarmManager.RTC_WAKEUP, trigger, pending)
        }
    }

    fun cancelFollowUp(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_FOLLOW_UP
        }
        val pending = PendingIntent.getBroadcast(
            context, FOLLOW_UP_REQUEST, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pending != null) {
            am.cancel(pending)
            pending.cancel()
        }
    }

    fun cancelAll(context: Context) {
        cancelDaily(context)
        cancelFollowUp(context)
    }

    private fun cancelDaily(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_DAILY
        }
        val pending = PendingIntent.getBroadcast(
            context, DAILY_REQUEST, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pending != null) {
            am.cancel(pending)
            pending.cancel()
        }
    }

    private fun nextDailyTime(hour: Int, minute: Int): Calendar {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target
    }
}
