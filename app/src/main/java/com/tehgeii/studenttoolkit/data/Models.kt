package com.tehgeii.studenttoolkit.data

import com.tehgeii.studenttoolkit.core.newId
import com.tehgeii.studenttoolkit.domain.academic.GradeComponent
import com.tehgeii.studenttoolkit.domain.academic.GradeScale
import com.tehgeii.studenttoolkit.domain.academic.Semester
import com.tehgeii.studenttoolkit.core.parseNumber
import com.tehgeii.studenttoolkit.domain.tasks.TaskSort

enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

/** Optional student profile. Every field may stay empty. */
data class Profile(
    val name: String = "",
    val major: String = "",
    val university: String = "",
    val semester: String = "",
)

data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val gpaDecimals: Int = 2,
    val gradeScale: GradeScale = GradeScale.DEFAULT,
    val defaultReminders: List<Int> = listOf(24 * 60, 3 * 60),
    val taskSort: TaskSort = TaskSort.SMART,
    val profile: Profile = Profile(),
)

/** Grade calculator inputs are kept as typed text so drafts survive exactly as entered. */
data class GradeDraftRow(val id: String, val name: String, val weight: String, val score: String) {
    fun toComponent() = GradeComponent(name, parseNumber(weight), parseNumber(score))
}

data class GradeDraft(val course: String = "", val rows: List<GradeDraftRow> = template()) {
    companion object {
        fun template(): List<GradeDraftRow> = listOf(
            GradeDraftRow(newId(), "Attendance", "10", ""),
            GradeDraftRow(newId(), "Assignment", "20", ""),
            GradeDraftRow(newId(), "Midterm", "30", ""),
            GradeDraftRow(newId(), "Final", "40", ""),
        )
    }
}

data class AcademicData(
    val semesters: List<Semester> = emptyList(),
    val draft: GradeDraft = GradeDraft(),
)
