package com.tehgeii.studenttoolkit.domain

import com.tehgeii.studenttoolkit.core.Fmt
import com.tehgeii.studenttoolkit.domain.academic.Course
import com.tehgeii.studenttoolkit.domain.academic.GpaCalculator
import com.tehgeii.studenttoolkit.domain.academic.GpaPlanner
import com.tehgeii.studenttoolkit.domain.academic.GpaTarget
import com.tehgeii.studenttoolkit.domain.academic.GradeBand
import com.tehgeii.studenttoolkit.domain.academic.GradeCalculator
import com.tehgeii.studenttoolkit.domain.academic.GradeComponent
import com.tehgeii.studenttoolkit.domain.academic.GradeScale
import com.tehgeii.studenttoolkit.domain.academic.Semester
import com.tehgeii.studenttoolkit.domain.academic.TargetResult
import com.tehgeii.studenttoolkit.domain.academic.WeightState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AcademicTest {
    private val scale = GradeScale.DEFAULT

    private fun c(name: String, weight: Double?, score: Double?) = GradeComponent(name, weight, score)

    @Test
    fun weightedFinalScore() {
        val summary = GradeCalculator.summarize(
            listOf(c("Attendance", 10.0, 100.0), c("Assignment", 20.0, 85.0), c("Midterm", 30.0, 78.0), c("Final", 40.0, 90.0)),
        )
        assertEquals(WeightState.VALID, summary.weightState)
        assertEquals(86.4, summary.finalScore!!, 1e-9)
        assertEquals("A", scale.bandFor(summary.finalScore!!)!!.letter)
    }

    @Test
    fun boundaryScores() {
        for (score in listOf(0.0, 25.0, 50.0, 75.0, 100.0, 84.99)) {
            val summary = GradeCalculator.summarize(listOf(c("Only", 100.0, score)))
            assertEquals(score, summary.finalScore!!, 1e-9)
        }
        assertEquals("E", scale.bandFor(0.0)!!.letter)
        assertEquals("BC", scale.bandFor(74.99)!!.letter)
        assertEquals("AB", scale.bandFor(84.99)!!.letter)
        assertEquals("A", scale.bandFor(100.0)!!.letter)
    }

    @Test
    fun invalidInputBlocksCalculation() {
        val negative = GradeCalculator.summarize(listOf(c("A", 50.0, -1.0), c("B", 50.0, 80.0)))
        assertEquals(setOf(0), negative.invalidRows)
        assertFalse(negative.canCalculate)
        assertNull(negative.finalScore)

        val tooHighWeight = GradeCalculator.summarize(listOf(c("A", 120.0, 80.0)))
        assertEquals(setOf(0), tooHighWeight.invalidRows)

        val over = GradeCalculator.summarize(listOf(c("A", 70.0, 80.0), c("B", 50.0, 80.0)))
        assertEquals(WeightState.OVER, over.weightState)
        assertFalse(over.canCalculate)
    }

    @Test
    fun underHundredIsAWarningNotABlock() {
        val summary = GradeCalculator.summarize(listOf(c("A", 40.0, 80.0), c("B", 50.0, 90.0)))
        assertEquals(WeightState.UNDER, summary.weightState)
        assertEquals(90.0, summary.totalWeight, 1e-9)
        assertEquals(77.0, summary.finalScore!!, 1e-9)
    }

    @Test
    fun blankRowsAreIgnored() {
        val summary = GradeCalculator.summarize(listOf(c("A", 100.0, 70.0), c("", null, null)))
        assertTrue(summary.invalidRows.isEmpty())
        assertEquals(70.0, summary.finalScore!!, 1e-9)
    }

    @Test
    fun requiredFinalScore() {
        val summary = GradeCalculator.summarize(listOf(c("Assignment", 20.0, 90.0), c("Midterm", 30.0, 78.0), c("Final", 50.0, null)))
        val result = GradeCalculator.requiredScore(summary, 85.0)
        assertEquals(87.2, (result as TargetResult.Needed).score, 1e-9)
        assertEquals(76.4, GradeCalculator.scenario(summary, 70.0), 1e-9)
    }

    @Test
    fun impossibleAndSecuredTargets() {
        val low = GradeCalculator.summarize(listOf(c("Midterm", 50.0, 40.0), c("Final", 50.0, null)))
        val impossible = GradeCalculator.requiredScore(low, 85.0)
        assertTrue(impossible is TargetResult.Impossible)
        assertEquals(130.0, (impossible as TargetResult.Impossible).score, 1e-9)

        val high = GradeCalculator.summarize(listOf(c("Midterm", 90.0, 100.0), c("Final", 10.0, null)))
        assertEquals(TargetResult.AlreadySecured, GradeCalculator.requiredScore(high, 85.0))

        val done = GradeCalculator.summarize(listOf(c("Only", 100.0, 80.0)))
        assertEquals(TargetResult.NothingRemaining, GradeCalculator.requiredScore(done, 85.0))
    }

    @Test
    fun semesterGpaIsCreditWeighted() {
        assertNull(GpaCalculator.courses(emptyList(), scale).gpa)
        val one = GpaCalculator.courses(listOf(Course("1", "Programming", 3, "A")), scale)
        assertEquals(4.0, one.gpa!!, 1e-9)
        val result = GpaCalculator.courses(
            listOf(Course("1", "Programming", 3, "A"), Course("2", "Database", 3, "AB"), Course("3", "Statistics", 2, "B")),
            scale,
        )
        assertEquals(8, result.totalCredits)
        assertEquals(3.5625, result.gpa!!, 1e-9)
        assertEquals("3.56", Fmt.fixed(result.gpa!!, 2))
    }

    @Test
    fun unknownGradesAreReportedNotCounted() {
        val result = GpaCalculator.courses(listOf(Course("1", "X", 3, "A"), Course("2", "Y", 3, "Z")), scale)
        assertEquals(3, result.totalCredits)
        assertEquals(1, result.unknownGrades)
    }

    @Test
    fun customGradePoints() {
        val custom = GradeScale(listOf(GradeBand("A", 80.0, 4.0), GradeBand("B", 60.0, 3.0), GradeBand("C", 0.0, 2.0)))
        val result = GpaCalculator.courses(listOf(Course("1", "X", 2, "A"), Course("2", "Y", 2, "C")), custom)
        assertEquals(3.0, result.gpa!!, 1e-9)
        assertTrue(custom.validate().isEmpty())
        assertFalse(GradeScale(listOf(GradeBand("A", 80.0, 4.0))).validate().isEmpty())
    }

    @Test
    fun cumulativeGpaUsesCredits() {
        val semesters = listOf(
            Semester("1", "Semester 1", manualGpa = 3.40, manualCredits = 20, useManual = true),
            Semester("2", "Semester 2", manualGpa = 3.70, manualCredits = 22, useManual = true),
            Semester("3", "Semester 3", manualGpa = 3.60, manualCredits = 21, useManual = true),
        )
        val result = GpaCalculator.cumulative(semesters, scale)
        assertEquals(63, result.totalCredits)
        assertEquals("3.57", Fmt.fixed(result.gpa!!, 2))
        // Not the naive average (3.5667 -> 3.57 too here, so check the exact value).
        assertEquals(225.0 / 63.0, result.gpa!!, 1e-12)
    }

    @Test
    fun gpaGoalPlanner() {
        val result = GpaPlanner.requiredGpa(3.42, 80, 60, 3.50, 4.0)
        assertEquals(216.4 / 60.0, (result as GpaTarget.Needed).gpa, 1e-9)
        assertTrue(GpaPlanner.requiredGpa(2.0, 120, 20, 3.5, 4.0) is GpaTarget.Impossible)
        assertEquals(GpaTarget.AlreadySecured, GpaPlanner.requiredGpa(3.9, 120, 4, 3.0, 4.0))
        assertTrue(GpaPlanner.requiredGpa(4.5, 10, 10, 3.0, 4.0) is GpaTarget.Invalid)
    }

    @Test
    fun rangeLabels() {
        assertEquals("85–100", scale.rangeLabel(scale.band("A")!!))
        assertEquals("80–84", scale.rangeLabel(scale.band("AB")!!))
        assertEquals("0–49", scale.rangeLabel(scale.band("E")!!))
        assertEquals("AB", scale.lowestLetterReaching(3.4)!!.letter)
    }

    @Test
    fun displayRounding() {
        assertEquals("84.57", Fmt.number(84.5678, 2))
        assertEquals("84.6", Fmt.number(84.5678, 1))
        assertEquals("3.500", Fmt.fixed(3.5, 3))
        assertEquals("86.4", Fmt.number(86.4, 2))
    }
}
