package com.tehgeii.studenttoolkit.domain.focus

/**
 * A countdown driven by timestamps instead of per-second ticks, so it stays correct while the
 * app is in the background, the screen is locked, or the process is recreated.
 * [endAt] is a monotonic clock reading (elapsedRealtime) when running.
 */
data class Countdown(
    val durationMs: Long,
    val remainingMs: Long = durationMs,
    val endAt: Long? = null,
) {
    val isRunning: Boolean get() = endAt != null

    /** Not started yet, or reset. */
    val isFresh: Boolean get() = endAt == null && remainingMs == durationMs

    fun remaining(now: Long): Long = endAt?.let { (it - now).coerceAtLeast(0L) } ?: remainingMs

    fun isFinished(now: Long): Boolean = isRunning && remaining(now) == 0L

    fun progress(now: Long): Float =
        if (durationMs <= 0L) 0f else (1f - remaining(now).toFloat() / durationMs).coerceIn(0f, 1f)

    fun start(now: Long): Countdown = if (isRunning || remainingMs <= 0L) this else copy(endAt = now + remainingMs)

    fun pause(now: Long): Countdown = if (!isRunning) this else copy(remainingMs = remaining(now), endAt = null)

    fun reset(): Countdown = Countdown(durationMs)
}

enum class FocusPhase(val label: String) { FOCUS("Focus"), BREAK("Break") }

data class PomodoroState(
    val focusMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val phase: FocusPhase = FocusPhase.FOCUS,
    val session: Int = 1,
    val sessionsPerCycle: Int = 4,
    val countdown: Countdown = Countdown(focusMinutes * 60_000L),
) {
    fun durationOf(p: FocusPhase): Long = (if (p == FocusPhase.FOCUS) focusMinutes else breakMinutes) * 60_000L

    /** Moves to the next phase: Focus → Break → Focus (next session). The new phase waits for Start. */
    fun advance(): PomodoroState = if (phase == FocusPhase.FOCUS) {
        copy(phase = FocusPhase.BREAK, countdown = Countdown(durationOf(FocusPhase.BREAK)))
    } else {
        val next = if (session >= sessionsPerCycle) 1 else session + 1
        copy(phase = FocusPhase.FOCUS, session = next, countdown = Countdown(durationOf(FocusPhase.FOCUS)))
    }

    /** Restarts the current phase; pressing it again on a fresh timer restarts the whole cycle. */
    fun reset(): PomodoroState = if (countdown.isFresh) {
        copy(phase = FocusPhase.FOCUS, session = 1, countdown = Countdown(durationOf(FocusPhase.FOCUS)))
    } else {
        copy(countdown = Countdown(durationOf(phase)))
    }

    fun withDurations(focus: Int, brk: Int): PomodoroState {
        val updated = copy(focusMinutes = focus, breakMinutes = brk)
        return updated.copy(phase = FocusPhase.FOCUS, session = 1, countdown = Countdown(updated.durationOf(FocusPhase.FOCUS)))
    }

    companion object {
        val PRESETS = listOf(25 to 5, 50 to 10, 90 to 15)
    }
}

data class Stopwatch(
    val accumulatedMs: Long = 0L,
    val startedAt: Long? = null,
    /** Cumulative elapsed time at each lap. */
    val laps: List<Long> = emptyList(),
) {
    val isRunning: Boolean get() = startedAt != null

    fun elapsed(now: Long): Long = accumulatedMs + (startedAt?.let { (now - it).coerceAtLeast(0L) } ?: 0L)

    fun start(now: Long): Stopwatch = if (isRunning) this else copy(startedAt = now)

    fun pause(now: Long): Stopwatch = if (!isRunning) this else copy(accumulatedMs = elapsed(now), startedAt = null)

    fun lap(now: Long): Stopwatch = copy(laps = laps + elapsed(now))

    fun reset(): Stopwatch = Stopwatch()
}

/** Focus totals per day, keyed by ISO date (yyyy-MM-dd). */
data class DayFocus(val sessions: Int = 0, val minutes: Int = 0) {
    operator fun plus(other: DayFocus) = DayFocus(sessions + other.sessions, minutes + other.minutes)
}
