package com.tehgeii.studenttoolkit.domain.academic

data class Course(
    val id: String,
    val name: String,
    val credits: Int,
    val grade: String,
)

/**
 * A semester is either a list of courses (IPS is computed) or, when the student only knows
 * the result, a manually entered IPS with its total credits (SKS).
 */
data class Semester(
    val id: String,
    val name: String,
    val courses: List<Course> = emptyList(),
    val manualGpa: Double? = null,
    val manualCredits: Int? = null,
    val useManual: Boolean = false,
    val updatedAt: Long = 0L,
)

data class GpaResult(
    val totalCredits: Int,
    /** Σ(gradePoint × credits). */
    val totalPoints: Double,
    /** Courses skipped because their grade is not in the scale. */
    val unknownGrades: Int = 0,
) {
    val gpa: Double? get() = if (totalCredits > 0) totalPoints / totalCredits else null

    operator fun plus(other: GpaResult) = GpaResult(
        totalCredits + other.totalCredits,
        totalPoints + other.totalPoints,
        unknownGrades + other.unknownGrades,
    )

    companion object {
        val EMPTY = GpaResult(0, 0.0)
    }
}

/**
 * GPA = Σ(gradePoint × credits) ÷ Σ(credits)
 *
 * The cumulative GPA (IPK) is weighted by credits, never a plain average of semester GPAs.
 */
object GpaCalculator {
    fun courses(courses: List<Course>, scale: GradeScale): GpaResult {
        var credits = 0
        var points = 0.0
        var unknown = 0
        for (course in courses) {
            if (course.credits <= 0) continue
            val gradePoint = scale.pointsFor(course.grade)
            if (gradePoint == null) {
                unknown++
                continue
            }
            credits += course.credits
            points += gradePoint * course.credits
        }
        return GpaResult(credits, points, unknown)
    }

    fun semester(semester: Semester, scale: GradeScale): GpaResult {
        if (semester.useManual) {
            val gpa = semester.manualGpa
            val credits = semester.manualCredits
            if (gpa == null || credits == null || credits <= 0 || gpa < 0) return GpaResult.EMPTY
            return GpaResult(credits, gpa * credits)
        }
        return courses(semester.courses, scale)
    }

    fun cumulative(semesters: List<Semester>, scale: GradeScale): GpaResult =
        semesters.fold(GpaResult.EMPTY) { acc, s -> acc + semester(s, scale) }
}

sealed interface GpaTarget {
    data class Needed(val gpa: Double) : GpaTarget
    data object AlreadySecured : GpaTarget
    data class Impossible(val gpa: Double) : GpaTarget
    data class Invalid(val reason: String) : GpaTarget
}

object GpaPlanner {
    /**
     * Average grade point needed on the remaining credits:
     *   needed = (target × (completed + remaining) − current × completed) ÷ remaining
     */
    fun requiredGpa(
        currentGpa: Double,
        completedCredits: Int,
        remainingCredits: Int,
        targetGpa: Double,
        maxPoints: Double,
    ): GpaTarget {
        if (currentGpa < 0 || currentGpa > maxPoints) return GpaTarget.Invalid("Current GPA must be between 0 and ${maxPoints}.")
        if (targetGpa < 0 || targetGpa > maxPoints) return GpaTarget.Invalid("Target GPA must be between 0 and ${maxPoints}.")
        if (completedCredits < 0 || remainingCredits < 0) return GpaTarget.Invalid("Credits cannot be negative.")
        if (remainingCredits == 0) {
            return if (currentGpa >= targetGpa - 1e-9) GpaTarget.AlreadySecured else GpaTarget.Impossible(Double.POSITIVE_INFINITY)
        }
        val needed = (targetGpa * (completedCredits + remainingCredits) - currentGpa * completedCredits) / remainingCredits
        return when {
            needed <= 1e-9 -> GpaTarget.AlreadySecured
            needed > maxPoints + 1e-9 -> GpaTarget.Impossible(needed)
            else -> GpaTarget.Needed(needed)
        }
    }
}
