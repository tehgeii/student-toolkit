package com.tehgeii.studenttoolkit.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.tehgeii.studenttoolkit.MainActivity
import com.tehgeii.studenttoolkit.R

/** Notification channels: Deadlines and Focus (Pomodoro + timer). Users can tune each in system settings. */
object Notifier {
    const val CHANNEL_DEADLINES = "deadlines"
    const val CHANNEL_FOCUS = "focus"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_DEADLINES, "Deadline reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Reminders before your task deadlines"
                },
                NotificationChannel(CHANNEL_FOCUS, "Pomodoro & timer", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Alerts when a focus session, break or timer ends"
                },
            ),
        )
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun canPost(context: Context): Boolean =
        hasPermission(context) && context.getSystemService(NotificationManager::class.java)?.areNotificationsEnabled() == true

    @SuppressLint("MissingPermission")
    fun post(context: Context, id: Int, channel: String, title: String, text: String, route: String?) {
        if (!canPost(context)) return
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (route != null) putExtra(MainActivity.EXTRA_ROUTE, route)
        }
        val contentIntent = PendingIntent.getActivity(
            context, id, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = Notification.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF4F46E5.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(if (channel == CHANNEL_FOCUS) Notification.CATEGORY_ALARM else Notification.CATEGORY_REMINDER)
            .build()
        try {
            context.getSystemService(NotificationManager::class.java)?.notify(id, notification)
        } catch (e: SecurityException) {
            Log.w("StudentToolkit", "Notification not allowed", e)
        }
    }
}

/**
 * Uses exact alarms when the user allows them (needed for precise timers on Android 12+),
 * and falls back to inexact while-idle alarms otherwise.
 */
object Alarms {
    fun canScheduleExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true

    @SuppressLint("ScheduleExactAlarm", "MissingPermission")
    fun set(context: Context, type: Int, triggerAt: Long, operation: PendingIntent) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        try {
            if (canScheduleExact(context)) {
                manager.setExactAndAllowWhileIdle(type, triggerAt, operation)
            } else {
                manager.setAndAllowWhileIdle(type, triggerAt, operation)
            }
        } catch (e: SecurityException) {
            manager.setAndAllowWhileIdle(type, triggerAt, operation)
        }
    }

    fun cancel(context: Context, operation: PendingIntent) {
        context.getSystemService(AlarmManager::class.java)?.cancel(operation)
    }
}
