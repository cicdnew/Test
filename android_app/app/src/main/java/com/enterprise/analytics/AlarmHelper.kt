package com.enterprise.analytics

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object AlarmHelper {
    fun scheduleReminder(context: Context, id: Int, title: String, dateInMillis: Long, isRecurring: Boolean, recurrenceType: String, videoUriStr: String?) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("REMINDER_ID", id)
            putExtra("REMINDER_TITLE", title)
            putExtra("REMINDER_VIDEO_URI", videoUriStr)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Add some permission checking here ideally if Android 12+, assuming exact alarms allowed for now
        if (isRecurring) {
            val interval = if (recurrenceType == "DAILY") AlarmManager.INTERVAL_DAY else AlarmManager.INTERVAL_DAY * 7
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                dateInMillis,
                interval,
                pendingIntent
            )
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                dateInMillis,
                pendingIntent
            )
        }
    }
}
