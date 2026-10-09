package com.tehgeii.studenttoolkit.domain

import com.tehgeii.studenttoolkit.domain.tasks.DeadlineStatus
import com.tehgeii.studenttoolkit.domain.tasks.Deadlines
import com.tehgeii.studenttoolkit.domain.tasks.Priority
import com.tehgeii.studenttoolkit.domain.tasks.ReminderPresets
import com.tehgeii.studenttoolkit.domain.tasks.Task
import com.tehgeii.studenttoolkit.domain.tasks.TaskOrganizer
import com.tehgeii.studenttoolkit.domain.tasks.TaskSection
import com.tehgeii.studenttoolkit.domain.tasks.TaskSort
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class TasksTest {
    private val zone = ZoneId.of("Asia/Jakarta")

    private fun at(y: Int, mo: Int, d: Int, h: Int = 12, mi: Int = 0): Long =
        LocalDateTime.of(y, mo, d, h, mi).atZone(zone).toInstant().toEpochMilli()

    private fun task(id: String, deadline: Long?, priority: Priority = Priority.MEDIUM, completed: Boolean = false, created: Long = 0) =
        Task(id = id, title = id, deadline = deadline, priority = priority, completed = completed, createdAt = created)

    @Test
    fun statuses() {
        val now = at(2026, 10, 8, 14, 0)
        assertEquals(DeadlineStatus.OVERDUE, Deadlines.status(task("a", at(2026, 10, 7)), now, zone))
        assertEquals(DeadlineStatus.OVERDUE, Deadlines.status(task("a", at(2026, 10, 8, 13, 59)), now, zone))
        assertEquals(DeadlineStatus.DUE_TODAY, Deadlines.status(task("a", at(2026, 10, 8, 23, 59)), now, zone))
        assertEquals(DeadlineStatus.ONE_DAY_LEFT, Deadlines.status(task("a", at(2026, 10, 9, 0, 0)), now, zone))
        assertEquals(DeadlineStatus.DUE_SOON, Deadlines.status(task("a", at(2026, 10, 11)), now, zone))
        assertEquals(DeadlineStatus.UPCOMING, Deadlines.status(task("a", at(2026, 10, 15)), now, zone))
        assertEquals(DeadlineStatus.UPCOMING, Deadlines.status(task("a", at(2026, 11, 8)), now, zone))
        assertEquals(DeadlineStatus.UPCOMING, Deadlines.status(task("a", at(2027, 10, 8)), now, zone))
        assertEquals(DeadlineStatus.NO_DEADLINE, Deadlines.status(task("a", null), now, zone))
        assertEquals(DeadlineStatus.COMPLETED, Deadlines.status(task("a", at(2026, 10, 1), completed = true), now, zone))
    }

    @Test
    fun calendarEdges() {
        // Midnight, month change, year change and leap day.
        assertEquals(1, Deadlines.daysUntil(at(2026, 11, 1, 0, 0), at(2026, 10, 31, 23, 59), zone))
        assertEquals(1, Deadlines.daysUntil(at(2027, 1, 1, 0, 1), at(2026, 12, 31, 23, 0), zone))
        assertEquals(2, Deadlines.daysUntil(at(2028, 3, 1), at(2028, 2, 28), zone))
        assertEquals(-1, Deadlines.daysUntil(at(2026, 10, 7), at(2026, 10, 8), zone))
    }

    @Test
    fun descriptions() {
        val now = at(2026, 10, 8, 14, 0)
        assertEquals("Due in 2 days", Deadlines.describe(at(2026, 10, 10), now, zone))
        assertEquals("Due tomorrow, 09:00", Deadlines.describe(at(2026, 10, 9, 9, 0), now, zone))
        assertEquals("Due today, 23:59", Deadlines.describe(at(2026, 10, 8, 23, 59), now, zone))
        assertEquals("Overdue by 1 day", Deadlines.describe(at(2026, 10, 7), now, zone))
        assertEquals("Overdue since 13:00", Deadlines.describe(at(2026, 10, 8, 13, 0), now, zone))
    }

    @Test
    fun smartOrdering() {
        val now = at(2026, 10, 8, 14, 0)
        val tasks = listOf(
            task("later-low", at(2026, 10, 20), Priority.LOW),
            task("someday", null, Priority.HIGH),
            task("overdue", at(2026, 10, 6)),
            task("today", at(2026, 10, 8, 20, 0)),
            task("soon-high", at(2026, 10, 10), Priority.HIGH),
            task("soon-low", at(2026, 10, 10), Priority.LOW),
            task("done", at(2026, 10, 1), completed = true),
        )
        val sections = TaskOrganizer.organize(tasks, TaskSort.SMART, now, zone)
        assertEquals(
            listOf(TaskSection.OVERDUE, TaskSection.TODAY, TaskSection.UPCOMING, TaskSection.SOMEDAY, TaskSection.COMPLETED),
            sections.map { it.first },
        )
        assertEquals(listOf("soon-high", "soon-low", "later-low"), sections[2].second.map { it.id })

        val byPriority = TaskOrganizer.organize(tasks, TaskSort.PRIORITY, now, zone)
        assertEquals(listOf("soon-high", "soon-low", "later-low"), byPriority[2].second.map { it.id })
    }

    @Test
    fun reminderLabels() {
        assertEquals("1 day", ReminderPresets.label(1440))
        assertEquals("3 hours", ReminderPresets.label(180))
        assertEquals("30 minutes", ReminderPresets.label(30))
        assertEquals("1 week", ReminderPresets.label(10080))
    }
}
