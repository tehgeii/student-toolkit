package com.tehgeii.studenttoolkit.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.tehgeii.studenttoolkit.core.Fmt
import com.tehgeii.studenttoolkit.data.TaskObserver
import com.tehgeii.studenttoolkit.domain.tasks.ReminderPresets
import com.tehgeii.studenttoolkit.domain.tasks.Task

/** Schedules deadline reminders with AlarmManager. No server and no background polling. */
class ReminderScheduler(private val context: Context) : TaskObserver {

    override fun onTaskChanged(old: Task?, new: Task?) {
        old?.let(::cancel)
        new?.let(::schedule)
    }

    fun schedule(task: Task) {
        val deadline = task.deadline ?: return
        if (task.completed) return
        val now = System.currentTimeMillis()
        for (minutes in task.reminders.distinct()) {
            val triggerAt = deadline - minutes * 60_000L
            if (triggerAt <= now) continue
            val text = buildString {
                append("Due in ").append(ReminderPresets.label(minutes))
                append(" · ").append(Fmt.dateTime(deadline))
                if (task.course.isNotBlank()) append(" · ").append(task.course)
            }
            val intent = baseIntent(task.id, minutes)
                .putExtra(AlarmReceiver.EXTRA_TASK_ID, task.id)
                .putExtra(AlarmReceiver.EXTRA_TEXT, text)
            val operation = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            Alarms.set(context, AlarmManager.RTC_WAKEUP, triggerAt, operation)
        }
    }

    fun cancel(task: Task) {
        for (minutes in task.reminders) {
            val operation = PendingIntent.getBroadcast(
                context, 0, baseIntent(task.id, minutes), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
            ) ?: continue
            Alarms.cancel(context, operation)
            operation.cancel()
        }
    }

    fun rescheduleAll(tasks: List<Task>) = tasks.forEach(::schedule)

    /** The data URI makes each (task, offset) pair a distinct PendingIntent. */
    private fun baseIntent(taskId: String, minutes: Int): Intent =
        Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_TASK_REMINDER)
            .setData(Uri.parse("studenttoolkit://reminder/$taskId/$minutes"))
}
