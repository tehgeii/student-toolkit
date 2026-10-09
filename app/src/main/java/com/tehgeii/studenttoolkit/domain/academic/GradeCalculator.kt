package com.tehgeii.studenttoolkit.domain.academic

import kotlin.math.abs

/** A grading component such as "Midterm 30%". A null score means "not graded yet". */
data class GradeComponent(val name: String, val weight: Double?, val score: Double?)

enum class WeightState { EMPTY, UNDER, VALID, OVER }

data class GradeSummary(
    val totalWeight: Double,
    val weightState: WeightState,
    /** Weight of components that already have a score. */
    val gradedWeight: Double,
    /** Weight of components still waiting for a score. */
    val remainingWeight: Double,
    /** Σ(score × weight) / 100 over graded components. */
    val currentPoints: Double,
    /** Indexes of rows with invalid input (missing weight, out-of-range values). */
    val invalidRows: Set<Int>,
) {
    /** Calculation is blocked when the weights exceed 100% or an input is invalid. */
    val canCalculate: Boolean
        get() = invalidRows.isEmpty() && weightState != WeightState.OVER && weightState != WeightState.EMPTY

    /** Final score, available once every component has a score. */
    val finalScore: Double?
        get() = if (canCalculate && remainingWeight == 0.0) currentPoints else null
}

sealed interface TargetResult {
    /** Average score needed on the remaining components. */
    data class Needed(val score: Double) : TargetResult

    /** The target is reached even with 0 on everything remaining. */
    data object AlreadySecured : TargetResult

    /** The target needs more than 100 on the remaining components. */
    data class Impossible(val score: Double) : TargetResult

    /** Nothing left to grade, so there is nothing to plan. */
    data object NothingRemaining : TargetResult
}

/**
 * Weighted grade formula, written out explicitly:
 *   Final = Σ(score × weight) / 100
 */
object GradeCalculator {
    private const val EPSILON = 1e-9

    fun summarize(components: List<GradeComponent>): GradeSummary {
        var total = 0.0
        var graded = 0.0
        var points = 0.0
        val invalid = mutableSetOf<Int>()
        components.forEachIndexed { index, c ->
            val blank = c.name.isBlank() && c.weight == null && c.score == null
            if (blank) return@forEachIndexed
            val weight = c.weight
            if (weight == null || weight <= 0.0 || weight > 100.0) {
                invalid += index
                return@forEachIndexed
            }
            if (c.score != null && (c.score < 0.0 || c.score > 100.0)) {
                invalid += index
                return@forEachIndexed
            }
            total += weight
            if (c.score != null) {
                graded += weight
                points += c.score * weight / 100.0
            }
        }
        val state = when {
            total == 0.0 -> WeightState.EMPTY
            abs(total - 100.0) < 1e-6 -> WeightState.VALID
            total < 100.0 -> WeightState.UNDER
            else -> WeightState.OVER
        }
        return GradeSummary(
            totalWeight = total,
            weightState = state,
            gradedWeight = graded,
            remainingWeight = total - graded,
            currentPoints = points,
            invalidRows = invalid,
        )
    }

    /** Average score needed on the remaining components to finish with at least [target]. */
    fun requiredScore(summary: GradeSummary, target: Double): TargetResult {
        if (summary.remainingWeight <= EPSILON) return TargetResult.NothingRemaining
        val needed = (target - summary.currentPoints) / (summary.remainingWeight / 100.0)
        return when {
            needed <= EPSILON -> TargetResult.AlreadySecured
            needed > 100.0 + EPSILON -> TargetResult.Impossible(needed)
            else -> TargetResult.Needed(needed)
        }
    }

    /** Final score if every remaining component scores [remainingScore]. */
    fun scenario(summary: GradeSummary, remainingScore: Double): Double =
        summary.currentPoints + remainingScore * summary.remainingWeight / 100.0
}
