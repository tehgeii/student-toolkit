package com.tehgeii.studenttoolkit.domain.academic

import com.tehgeii.studenttoolkit.core.Fmt

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
    fun validate(): List<String> {
        val problems = mutableListOf<String>()
        if (bands.isEmpty()) problems += "Add at least one grade."
        if (bands.any { it.letter.isBlank() }) problems += "Every grade needs a letter."
        val duplicates = bands.groupBy { it.letter.trim().uppercase() }.filter { it.value.size > 1 }.keys
        if (duplicates.isNotEmpty()) problems += "Duplicate letters: ${duplicates.joinToString()}."
        if (bands.any { it.minScore < 0 || it.minScore > 100 }) problems += "Minimum scores must be between 0 and 100."
        if (bands.any { it.points < 0 }) problems += "Grade points cannot be negative."
        if (bands.map { it.minScore }.distinct().size != bands.size) problems += "Two grades share the same minimum score."
        if (bands.isNotEmpty() && bands.last().minScore > 0) problems += "The lowest grade should start at 0 so every score gets a grade."
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
