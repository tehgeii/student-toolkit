package com.tehgeii.studenttoolkit.domain.academic

import com.tehgeii.studenttoolkit.core.Fmt

/** A reason a grading scale cannot be used. [detail] carries e.g. the duplicated letters. */
data class ScaleProblem(val kind: Kind, val detail: String = "") {
    enum class Kind { EMPTY, BLANK_LETTER, DUPLICATE_LETTERS, MIN_OUT_OF_RANGE, NEGATIVE_POINTS, DUPLICATE_MIN, LOWEST_NOT_ZERO }
}

/** One row of a grading scale: a score of at least [minScore] earns [letter] worth [points]. */
data class GradeBand(val letter: String, val minScore: Double, val points: Double)

/**
 * A configurable grading scale. Campuses differ, so nothing here assumes a fixed system.
 * Bands are kept sorted from the highest minimum score to the lowest.
 */
class GradeScale(bands: List<GradeBand>) {
    val bands: List<GradeBand> = bands.sortedByDescending { it.minScore }

    val letters: List<String> get() = bands.map { it.letter }

    val maxPoints: Double get() = bands.maxOfOrNull { it.points } ?: 4.0

    /** The band a score falls into, or null when the score is below every band. */
    fun bandFor(score: Double): GradeBand? = bands.firstOrNull { score >= it.minScore }

    fun band(letter: String): GradeBand? =
        bands.firstOrNull { it.letter.equals(letter.trim(), ignoreCase = true) }

    fun pointsFor(letter: String): Double? = band(letter)?.points

    /** Human readable range such as "80–84" (integer scales) or "80–<85". */
    fun rangeLabel(band: GradeBand): String {
        val index = bands.indexOf(band)
        val min = band.minScore
        if (index <= 0) return "${Fmt.number(min)}–100"
        val next = bands[index - 1].minScore
        val integers = min % 1.0 == 0.0 && next % 1.0 == 0.0
        return if (integers) "${Fmt.number(min)}–${Fmt.number(next - 1)}"
        else "${Fmt.number(min)}–<${Fmt.number(next)}"
    }

    /** Lowest letter whose points reach [points], used to translate a GPA target into a grade. */
    fun lowestLetterReaching(points: Double): GradeBand? =
        bands.filter { it.points >= points - 1e-9 }.minByOrNull { it.points }

    /** Returns a list of problems; an empty list means the scale is usable. */
    fun validate(): List<ScaleProblem> {
        val problems = mutableListOf<ScaleProblem>()
        fun add(kind: ScaleProblem.Kind, detail: String = "") { problems += ScaleProblem(kind, detail) }
        if (bands.isEmpty()) add(ScaleProblem.Kind.EMPTY)
        if (bands.any { it.letter.isBlank() }) add(ScaleProblem.Kind.BLANK_LETTER)
        val duplicates = bands.groupBy { it.letter.trim().uppercase() }.filter { it.value.size > 1 }.keys
        if (duplicates.isNotEmpty()) add(ScaleProblem.Kind.DUPLICATE_LETTERS, duplicates.joinToString())
        if (bands.any { it.minScore < 0 || it.minScore > 100 }) add(ScaleProblem.Kind.MIN_OUT_OF_RANGE)
        if (bands.any { it.points < 0 }) add(ScaleProblem.Kind.NEGATIVE_POINTS)
        if (bands.map { it.minScore }.distinct().size != bands.size) add(ScaleProblem.Kind.DUPLICATE_MIN)
        if (bands.isNotEmpty() && bands.last().minScore > 0) add(ScaleProblem.Kind.LOWEST_NOT_ZERO)
        return problems
    }

    override fun equals(other: Any?): Boolean = other is GradeScale && other.bands == bands
    override fun hashCode(): Int = bands.hashCode()

    companion object {
        val DEFAULT = GradeScale(
            listOf(
                GradeBand("A", 85.0, 4.0),
                GradeBand("AB", 80.0, 3.5),
                GradeBand("B", 75.0, 3.0),
                GradeBand("BC", 70.0, 2.5),
                GradeBand("C", 60.0, 2.0),
                GradeBand("D", 50.0, 1.0),
                GradeBand("E", 0.0, 0.0),
            ),
        )
    }
}
