package com.tehgeii.studenttoolkit.data

import com.tehgeii.studenttoolkit.domain.academic.Course
import com.tehgeii.studenttoolkit.domain.academic.GradeBand
import com.tehgeii.studenttoolkit.domain.academic.GradeScale
import com.tehgeii.studenttoolkit.domain.academic.Semester
import com.tehgeii.studenttoolkit.domain.focus.DayFocus
import com.tehgeii.studenttoolkit.domain.notes.Note
import com.tehgeii.studenttoolkit.domain.tasks.Priority
import com.tehgeii.studenttoolkit.domain.tasks.Subtask
import com.tehgeii.studenttoolkit.domain.tasks.Task
import com.tehgeii.studenttoolkit.i18n.AppLanguage
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupTest {
    private val sample = BackupData(
        settings = AppSettings(
            theme = ThemeMode.DARK,
            language = AppLanguage.INDONESIAN,
            gpaDecimals = 3,
            gradeScale = GradeScale(listOf(GradeBand("A", 80.0, 4.0), GradeBand("B", 65.0, 3.0), GradeBand("C", 0.0, 2.0))),
            profile = Profile(name = "Dafi"),
        ),
        academic = AcademicData(
            semesters = listOf(
                Semester("s1", "Semester 1", listOf(Course("c1", "Basis Data", 3, "A")), updatedAt = 5),
                Semester("s2", "Semester 2", manualGpa = 3.7, manualCredits = 22, useManual = true, updatedAt = 6),
            ),
        ),
        tasks = listOf(
            Task(
                id = "t1", title = "Tugas Basis Data", course = "Basis Data", deadline = 1_791_000_000_000,
                priority = Priority.HIGH, estimateMinutes = 210, subtasks = listOf(Subtask("st1", "ERD", true)),
                reminders = listOf(1440, 180), createdAt = 1, updatedAt = 2,
            ),
        ),
        notes = listOf(Note("n1", "Ide", "Isi catatan\nbaris dua", 1, 2)),
        focus = mapOf("2026-10-08" to DayFocus(4, 100)),
    )

    @Test
    fun roundTrip() {
        val text = BackupCodec.encode(sample, "0.9.0", 123)
        val file = BackupCodec.decode(text)
        assertEquals(1, file.schemaVersion)
        assertEquals("0.9.0", file.appVersion)
        assertEquals(123L, file.exportedAt)
        assertEquals(sample.copy(academic = sample.academic.copy(draft = file.data.academic.draft)), file.data)
    }

    private fun assertRejected(text: String, reason: BackupError) {
        try {
            BackupCodec.decode(text)
            fail("Expected rejection")
        } catch (e: BackupException) {
            assertEquals(reason, e.reason)
        }
    }

    @Test
    fun rejectsBadFiles() {
        assertRejected("", BackupError.EMPTY)
        assertRejected("{ not json", BackupError.NOT_JSON)
        assertRejected("{\"app\":\"Other\",\"schemaVersion\":1}", BackupError.NOT_STUDENT_TOOLKIT)
        assertRejected("{\"app\":\"StudentToolkit\"}", BackupError.NO_SCHEMA)
        assertRejected("{\"app\":\"StudentToolkit\",\"schemaVersion\":99}", BackupError.NEWER_SCHEMA)
        assertRejected("{\"app\":\"StudentToolkit\",\"schemaVersion\":0}", BackupError.UNSUPPORTED_SCHEMA)
        assertRejected("{\"app\":\"StudentToolkit\",\"schemaVersion\":1,\"tasks\":[{\"title\":\"no id\"}]}", BackupError.DAMAGED)
        assertRejected("{\"app\":\"StudentToolkit\",\"schemaVersion\":1,\"tasks\":[42]}", BackupError.DAMAGED)
    }

    @Test
    fun minimalBackupUsesDefaults() {
        val file = BackupCodec.decode("{\"app\":\"StudentToolkit\",\"schemaVersion\":1}")
        assertEquals(AppSettings(), file.data.settings)
        assertTrue(file.data.tasks.isEmpty())
    }

    @Test
    fun invalidGradeScaleFallsBackToDefault() {
        val json = JSONObject(BackupCodec.encode(sample, "x", 0))
        json.getJSONObject("settings").put("gradeScale", org.json.JSONArray().put(JSONObject().put("letter", "A").put("minScore", 50).put("points", 4)))
        val file = BackupCodec.decode(json.toString())
        assertEquals(GradeScale.DEFAULT, file.data.settings.gradeScale)
    }

    @Test
    fun mergingTheSameBackupTwiceAddsNothing() {
        val once = BackupCodec.merge(sample, sample)
        val twice = BackupCodec.merge(once, sample)
        assertEquals(sample.tasks, twice.tasks)
        assertEquals(sample.notes, twice.notes)
        assertEquals(sample.academic.semesters, twice.academic.semesters)
        assertEquals(sample.focus, twice.focus)
    }

    @Test
    fun mergeKeepsNewestAndAddsMissing() {
        val current = sample.copy(tasks = listOf(sample.tasks[0].copy(title = "Edited later", updatedAt = 50)))
        val incoming = sample.copy(tasks = sample.tasks + Task(id = "t2", title = "New", createdAt = 3, updatedAt = 3))
        val merged = BackupCodec.merge(current, incoming)
        assertEquals(listOf("Edited later", "New"), merged.tasks.map { it.title })
    }

    @Test
    fun largeDataset() {
        val tasks = (1..5000).map { Task(id = "t$it", title = "Task $it", createdAt = it.toLong(), updatedAt = it.toLong()) }
        val notes = (1..2000).map { Note("n$it", "Note $it", "x".repeat(200), 0, 0) }
        val big = sample.copy(tasks = tasks, notes = notes)
        val file = BackupCodec.decode(BackupCodec.encode(big, "x", 0))
        assertEquals(5000, file.data.tasks.size)
        assertEquals(2000, file.data.notes.size)
    }

    @Test
    fun lenientFileLoadingSkipsBrokenItems() {
        val array = org.json.JSONArray()
            .put(Codecs.encodeTask(sample.tasks[0]))
            .put(JSONObject().put("title", "missing id"))
        assertEquals(1, Codecs.decodeTasks(array).size)
    }
}
