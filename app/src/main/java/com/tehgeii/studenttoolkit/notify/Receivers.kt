package com.tehgeii.studenttoolkit.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tehgeii.studenttoolkit.StudentToolkitApp
import com.tehgeii.studenttoolkit.ui.Routes

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = StudentToolkitApp.container(context)
        when (intent.action) {
            ACTION_TASK_REMINDER -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
                val task = container.tasks.get(taskId) ?: return
                if (task.completed) return
                Notifier.post(
                    context,
                    id = intent.dataString.hashCode(),
                    channel = Notifier.CHANNEL_DEADLINES,
                    title = task.title,
                    text = intent.getStringExtra(EXTRA_TEXT) ?: container.lang()("Deadline reminder", "Pengingat deadline"),
                    route = Routes.task(taskId),
                )
            }
            ACTION_FOCUS_DONE -> container.focus.checkFinished()
        }
    }

    companion object {
        const val ACTION_TASK_REMINDER = "com.tehgeii.studenttoolkit.action.TASK_REMINDER"
        const val ACTION_FOCUS_DONE = "com.tehgeii.studenttoolkit.action.FOCUS_DONE"
        const val EXTRA_TASK_ID = "taskId"
        const val EXTRA_TEXT = "text"
    }
}

/** Alarms do not survive a reboot or an app update, so they are rebuilt from local data. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val container = StudentToolkitApp.container(context)
        container.reminders.rescheduleAll(container.tasks.tasks.value)
        container.focus.restoreAlarms()
    }
}
