package com.tehgeii.studenttoolkit.domain.tasks

import com.tehgeii.studenttoolkit.core.Fmt
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class Priority(val label: String) { LOW("Low"), MEDIUM("Medium"), HIGH("High") }

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

enum class DeadlineStatus(val label: String) {
    OVERDUE("Overdue"),
    DUE_TODAY("Due today"),
    ONE_DAY_LEFT("1 day left"),
    DUE_SOON("Due soon"),
    UPCOMING("Upcoming"),
    NO_DEADLINE("No deadline"),
    COMPLETED("Completed"),
}

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

    /** Short relative description, e.g. "Due in 2 days" or "Overdue by 1 day". */
    fun describe(deadline: Long, now: Long, zone: ZoneId): String {
        val days = daysUntil(deadline, now, zone)
        val time = Fmt.time(deadline, zone)
        if (deadline < now) {
            val late = -days
            return when {
                late <= 0L -> "Overdue since $time"
                late == 1L -> "Overdue by 1 day"
                else -> "Overdue by $late days"
            }
        }
        return when {
            days <= 0L -> "Due today, $time"
            days == 1L -> "Due tomorrow, $time"
            else -> "Due in $days days"
        }
    }
}

enum class TaskSort(val label: String) {
    SMART("Smart"),
    DEADLINE("Deadline"),
    PRIORITY("Priority"),
    NEWEST("Recently added"),
}

enum class TaskSection(val label: String) {
    OVERDUE("Overdue"),
    TODAY("Today"),
    UPCOMING("Upcoming"),
    SOMEDAY("No deadline"),
    COMPLETED("Completed"),
}

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

    fun label(minutes: Int): String = when {
        minutes % (7 * 24 * 60) == 0 -> plural(minutes / (7 * 24 * 60), "week")
        minutes % (24 * 60) == 0 -> plural(minutes / (24 * 60), "day")
        minutes % 60 == 0 -> plural(minutes / 60, "hour")
        else -> plural(minutes, "minute")
    }

    private fun plural(n: Int, unit: String) = if (n == 1) "1 $unit" else "$n ${unit}s"
}
