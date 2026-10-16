package com.tehgeii.studenttoolkit.domain.tasks

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class Priority { LOW, MEDIUM, HIGH }

data class Subtask(val id: String, val title: String, val done: Boolean = false)

data class Task(
    val id: String,
    val title: String,
    val course: String = "",
    /** Deadline as an absolute instant (epoch millis); shown in the device time zone. */
    val deadline: Long? = null,
    val priority: Priority = Priority.MEDIUM,
    val estimateMinutes: Int? = null,
    val notes: String = "",
    val completed: Boolean = false,
    val completedAt: Long? = null,
    val subtasks: List<Subtask> = emptyList(),
    /** Reminder offsets in minutes before the deadline. */
    val reminders: List<Int> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

enum class DeadlineStatus { OVERDUE, DUE_TODAY, ONE_DAY_LEFT, DUE_SOON, UPCOMING, NO_DEADLINE, COMPLETED }

object Deadlines {
    /** "Due soon" covers deadlines within this many calendar days. */
    const val SOON_DAYS = 3

    private fun localDate(epochMillis: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    /** Calendar days between today and the deadline date (negative when in the past). */
    fun daysUntil(deadline: Long, now: Long, zone: ZoneId): Long =
        ChronoUnit.DAYS.between(localDate(now, zone), localDate(deadline, zone))

    fun status(task: Task, now: Long, zone: ZoneId): DeadlineStatus {
        if (task.completed) return DeadlineStatus.COMPLETED
        val deadline = task.deadline ?: return DeadlineStatus.NO_DEADLINE
        if (deadline < now) return DeadlineStatus.OVERDUE
        val days = daysUntil(deadline, now, zone)
        return when {
            days <= 0 -> DeadlineStatus.DUE_TODAY
            days == 1L -> DeadlineStatus.ONE_DAY_LEFT
            days <= SOON_DAYS -> DeadlineStatus.DUE_SOON
            else -> DeadlineStatus.UPCOMING
        }
    }
}

enum class TaskSort { SMART, DEADLINE, PRIORITY, NEWEST }

enum class TaskSection { OVERDUE, TODAY, UPCOMING, SOMEDAY, COMPLETED }

object TaskOrganizer {
    fun section(task: Task, now: Long, zone: ZoneId): TaskSection = when (Deadlines.status(task, now, zone)) {
        DeadlineStatus.COMPLETED -> TaskSection.COMPLETED
        DeadlineStatus.OVERDUE -> TaskSection.OVERDUE
        DeadlineStatus.DUE_TODAY -> TaskSection.TODAY
        DeadlineStatus.NO_DEADLINE -> TaskSection.SOMEDAY
        else -> TaskSection.UPCOMING
    }

    private val byDeadline = compareBy<Task, Long?>(nullsLast<Long>()) { it.deadline }
    private val byPriorityDesc = compareByDescending<Task> { it.priority.ordinal }
    private val byCreated = compareBy<Task> { it.createdAt }

    fun comparator(sort: TaskSort): Comparator<Task> = when (sort) {
        // Overdue and due-today come first through sections; inside a section: deadline, then priority.
        TaskSort.SMART -> byDeadline.then(byPriorityDesc).then(byCreated)
        TaskSort.DEADLINE -> byDeadline.then(byCreated)
        TaskSort.PRIORITY -> byPriorityDesc.then(byDeadline).then(byCreated)
        TaskSort.NEWEST -> compareByDescending<Task> { it.createdAt }
    }

    /** Groups tasks into non-empty sections, in the order Overdue → Today → Upcoming → No deadline → Completed. */
    fun organize(tasks: List<Task>, sort: TaskSort, now: Long, zone: ZoneId): List<Pair<TaskSection, List<Task>>> {
        val grouped = tasks.groupBy { section(it, now, zone) }
        return TaskSection.entries.mapNotNull { section ->
            val items = grouped[section] ?: return@mapNotNull null
            val sorted = if (section == TaskSection.COMPLETED) {
                items.sortedByDescending { it.completedAt ?: it.updatedAt }
            } else {
                items.sortedWith(comparator(sort))
            }
            section to sorted
        }
    }

    /** Incomplete tasks with a deadline, soonest first (overdue included). */
    fun upcoming(tasks: List<Task>): List<Task> =
        tasks.filter { !it.completed && it.deadline != null }.sortedWith(byDeadline.then(byPriorityDesc))
}

/** Reminder presets offered in the task editor (minutes before the deadline). */
object ReminderPresets {
    val options: List<Int> = listOf(10, 30, 60, 180, 24 * 60, 2 * 24 * 60, 7 * 24 * 60)

    /** Custom reminders may be anything from 1 minute up to this many minutes before the deadline. */
    const val MAX_MINUTES = 365 * 24 * 60
}
