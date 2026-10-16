package com.tehgeii.studenttoolkit.domain.tools

enum class PercentMode { PERCENT_OF, WHAT_PERCENT, CHANGE, DISCOUNT, INCREASE, DECREASE }


object Percentage {
    fun percentOf(percent: Double, value: Double): Double = value * percent / 100.0

    /** Null when the whole is zero. */
    fun whatPercent(part: Double, whole: Double): Double? = if (whole == 0.0) null else part / whole * 100.0

    /** Relative change from [from] to [to] in percent; null when [from] is zero. */
    fun change(from: Double, to: Double): Double? = if (from == 0.0) null else (to - from) / from * 100.0

    fun increase(value: Double, percent: Double): Double = value * (1.0 + percent / 100.0)

    fun decrease(value: Double, percent: Double): Double = value * (1.0 - percent / 100.0)

    /** Returns final price and the amount saved. */
    fun discount(price: Double, percent: Double): Pair<Double, Double> {
        val saved = percentOf(percent, price)
        return (price - saved) to saved
    }
}
