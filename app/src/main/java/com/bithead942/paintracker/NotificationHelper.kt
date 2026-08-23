package com.bithead942.paintracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
    private const val CHANNEL_ID = "pain_tracker_channel"

    fun createNotificationChannel(context: Context, sound: Boolean, vibrate: Boolean, soundUri: Uri? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.deleteNotificationChannel(CHANNEL_ID)
            val name = context.getString(R.string.pain_tracker)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                if (sound && soundUri != null) {
                    setSound(soundUri, audioAttributes)
                } else {
                    setSound(null, audioAttributes)
                }
                enableVibration(vibrate)
                if (!vibrate) vibrationPattern = null
            }
            nm.createNotificationChannel(channel)
        }
    }

    fun showNotification(context: Context, sound: Boolean, vibrate: Boolean, soundUri: Uri? = null) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.pain_tracker))
            .setContentText(context.getString(R.string.log_today))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            if (sound && soundUri != null) {
                builder.setSound(soundUri)
            }
            if (vibrate) {
                builder.setVibrate(longArrayOf(0, 500))
            }
        }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(1, builder.build())
    }

    fun cancelNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(1)
    }
}
