package com.enterprise.analytics

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.enterprise.analytics.service.FloatingReminderService
import android.provider.Settings

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("REMINDER_TITLE") ?: "Reminder"
        val videoUriStr = intent.getStringExtra("REMINDER_VIDEO_URI")
        val reminderId = intent.getIntExtra("REMINDER_ID", 0)

        // Get customized colors from shared preferences
        val sharedPrefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val bgColor = sharedPrefs.getString("floating_bg_color", "#FFFFFF")
        val textColor = sharedPrefs.getString("floating_text_color", "#000000")

        if (Settings.canDrawOverlays(context)) {
            val serviceIntent = Intent(context, FloatingReminderService::class.java).apply {
                putExtra("REMINDER_TITLE", title)
                if (videoUriStr != null) {
                    putExtra("REMINDER_VIDEO_URI", videoUriStr)
                }
                putExtra("BG_COLOR", bgColor)
                putExtra("TEXT_COLOR", textColor)
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "reminder_channel",
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(context, "reminder_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Reminder: $title")
            .setContentText(if (Settings.canDrawOverlays(context)) "Floating reminder shown." else "Reminder triggered. Missing overlay permission. Tap to enable.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (!Settings.canDrawOverlays(context)) {
            val settingsIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${context.packageName}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                reminderId,
                settingsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            notificationBuilder.setContentIntent(pendingIntent)
        }

        val notification = notificationBuilder.build()

        notificationManager.notify(reminderId, notification)
    }
}
