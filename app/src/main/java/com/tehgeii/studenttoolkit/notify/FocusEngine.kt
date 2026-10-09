package com.tehgeii.studenttoolkit.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import com.tehgeii.studenttoolkit.core.Fmt
import com.tehgeii.studenttoolkit.data.FocusRepository
import com.tehgeii.studenttoolkit.domain.focus.Countdown
import com.tehgeii.studenttoolkit.domain.focus.FocusPhase
import com.tehgeii.studenttoolkit.domain.focus.PomodoroState
import com.tehgeii.studenttoolkit.domain.focus.Stopwatch
import com.tehgeii.studenttoolkit.ui.Routes
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

/**
 * Owns the Pomodoro, timer and stopwatch state.
 *
 * Time is measured with timestamps (elapsedRealtime), never by counting UI ticks, and the state
 * is persisted on every change. A running countdown has one AlarmManager alarm at its end, so the
 * user is notified even if the app was closed — without a foreground service or polling.
 */
class FocusEngine(private val context: Context, private val stats: FocusRepository) {
    private val prefs = context.getSharedPreferences("focus_state", Context.MODE_PRIVATE)

    private val _pomodoro: MutableStateFlow<PomodoroState>
    private val _timer: MutableStateFlow<Countdown>
    private val _stopwatch: MutableStateFlow<Stopwatch>

    init {
        val shift = clockShift()
        _pomodoro = MutableStateFlow(readPomodoro(shift))
        _timer = MutableStateFlow(readCountdown(prefs.getString(KEY_TIMER, null), shift) ?: Countdown(5 * 60_000L))
        _stopwatch = MutableStateFlow(readStopwatch(shift))
        if (shift != 0L) save()
    }

    val pomodoro: StateFlow<PomodoroState> = _pomodoro.asStateFlow()
    val timer: StateFlow<Countdown> = _timer.asStateFlow()
    val stopwatch: StateFlow<Stopwatch> = _stopwatch.asStateFlow()

    fun now(): Long = SystemClock.elapsedRealtime()

    // ---- Pomodoro --------------------------------------------------------------------------

    fun startPomodoro() = setPomodoro { it.copy(countdown = it.countdown.start(now())) }
    fun pausePomodoro() = setPomodoro { it.copy(countdown = it.countdown.pause(now())) }
    fun resetPomodoro() = setPomodoro { it.reset() }
    fun skipPomodoro() = setPomodoro { it.advance() }
    fun setPomodoroDurations(focusMinutes: Int, breakMinutes: Int) =
        setPomodoro { it.withDurations(focusMinutes.coerceIn(1, 240), breakMinutes.coerceIn(1, 120)) }

    private fun setPomodoro(transform: (PomodoroState) -> PomodoroState) {
        _pomodoro.update(transform)
        syncAlarm(KIND_POMODORO, _pomodoro.value.countdown)
        save()
    }

    // ---- Timer -----------------------------------------------------------------------------

    fun setTimerDuration(ms: Long) = setTimer { Countdown(ms.coerceIn(1_000L, 24 * 3_600_000L)) }
    fun startTimer() = setTimer { it.start(now()) }
    fun pauseTimer() = setTimer { it.pause(now()) }
    fun resetTimer() = setTimer { it.reset() }

    private fun setTimer(transform: (Countdown) -> Countdown) {
        _timer.update(transform)
        syncAlarm(KIND_TIMER, _timer.value)
        save()
    }

    // ---- Stopwatch -------------------------------------------------------------------------

    fun startStopwatch() = setStopwatch { it.start(now()) }
    fun pauseStopwatch() = setStopwatch { it.pause(now()) }
    fun lapStopwatch() = setStopwatch { it.lap(now()) }
    fun resetStopwatch() = setStopwatch { it.reset() }

    private fun setStopwatch(transform: (Stopwatch) -> Stopwatch) {
        _stopwatch.update(transform)
        save()
    }

    // ---- Completion ------------------------------------------------------------------------

    /** Completes countdowns whose time is up. Safe to call any number of times. */
    fun checkFinished() {
        val n = now()
        val p = _pomodoro.value
        if (p.countdown.isRunning && p.countdown.remaining(n) <= FINISH_TOLERANCE_MS) {
            if (p.phase == FocusPhase.FOCUS) stats.record(LocalDate.now(), p.focusMinutes)
            val next = p.advance()
            setPomodoro { next }
            val title: String
            val text: String
            if (p.phase == FocusPhase.FOCUS) {
                title = "Focus session done 🎉"
                text = "Nice work! Take a ${p.breakMinutes}-minute break."
            } else {
                title = "Break is over"
                text = "Ready for session ${next.session} of ${next.sessionsPerCycle}?"
            }
            Notifier.post(context, NOTIFICATION_POMODORO, Notifier.CHANNEL_FOCUS, title, text, Routes.POMODORO)
        }
        val t = _timer.value
        if (t.isRunning && t.remaining(n) <= FINISH_TOLERANCE_MS) {
            setTimer { it.reset() }
            Notifier.post(
                context, NOTIFICATION_TIMER, Notifier.CHANNEL_FOCUS,
                "Time's up ⏰", "Your ${Fmt.countdown(t.durationMs)} timer has finished.", Routes.TIMER,
            )
        }
    }

    /** Re-creates alarms after a reboot or app update. */
    fun restoreAlarms() {
        syncAlarm(KIND_POMODORO, _pomodoro.value.countdown)
        syncAlarm(KIND_TIMER, _timer.value)
        checkFinished()
    }

    private fun syncAlarm(kind: String, countdown: Countdown) {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_FOCUS_DONE)
            .setData(Uri.parse("studenttoolkit://focus/$kind"))
        val operation = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val endAt = countdown.endAt
        if (endAt != null) {
            Alarms.set(context, AlarmManager.ELAPSED_REALTIME_WAKEUP, endAt, operation)
        } else {
            Alarms.cancel(context, operation)
        }
    }

    // ---- Persistence -----------------------------------------------------------------------

    private fun bootCount(): Int = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)

    /**
     * elapsedRealtime restarts at zero after a reboot. Stored timestamps are then translated
     * through the wall clock captured at save time.
     */
    private fun clockShift(): Long {
        if (!prefs.contains(KEY_CLOCK_ELAPSED)) return 0L
        val savedElapsed = prefs.getLong(KEY_CLOCK_ELAPSED, 0L)
        val savedWall = prefs.getLong(KEY_CLOCK_WALL, 0L)
        val savedBoot = prefs.getInt(KEY_BOOT, -1)
        val nowElapsed = now()
        val rebooted = savedBoot != bootCount() || savedElapsed > nowElapsed
        if (!rebooted) return 0L
        return (savedWall - savedElapsed) + (nowElapsed - System.currentTimeMillis())
    }

    private fun save() {
        val p = _pomodoro.value
        val pomodoroJson = JSONObject()
            .put("focus", p.focusMinutes)
            .put("break", p.breakMinutes)
            .put("phase", p.phase.name)
            .put("session", p.session)
            .put("countdown", countdownJson(p.countdown))
        val s = _stopwatch.value
        val stopwatchJson = JSONObject()
            .put("accumulated", s.accumulatedMs)
            .put("laps", JSONArray().also { array -> s.laps.forEach { array.put(it) } })
        s.startedAt?.let { stopwatchJson.put("startedAt", it) }
        prefs.edit()
            .putString(KEY_POMODORO, pomodoroJson.toString())
            .putString(KEY_TIMER, countdownJson(_timer.value).toString())
            .putString(KEY_STOPWATCH, stopwatchJson.toString())
            .putLong(KEY_CLOCK_ELAPSED, now())
            .putLong(KEY_CLOCK_WALL, System.currentTimeMillis())
            .putInt(KEY_BOOT, bootCount())
            .apply()
    }

    private fun countdownJson(c: Countdown): JSONObject = JSONObject()
        .put("duration", c.durationMs)
        .put("remaining", c.remainingMs)
        .also { json -> c.endAt?.let { json.put("endAt", it) } }

    private fun readCountdown(raw: String?, shift: Long): Countdown? = runCatching {
        if (raw == null) return null
        readCountdown(JSONObject(raw), shift)
    }.getOrNull()

    private fun readCountdown(json: JSONObject, shift: Long): Countdown {
        val duration = json.getLong("duration").coerceAtLeast(1_000L)
        val remaining = json.optLong("remaining", duration).coerceIn(0L, duration)
        val endAt = if (json.has("endAt")) json.getLong("endAt") + shift else null
        return Countdown(duration, remaining, endAt)
    }

    private fun readPomodoro(shift: Long): PomodoroState = runCatching {
        val raw = prefs.getString(KEY_POMODORO, null) ?: return PomodoroState()
        val json = JSONObject(raw)
        val focus = json.optInt("focus", 25).coerceIn(1, 240)
        val brk = json.optInt("break", 5).coerceIn(1, 120)
        val phase = FocusPhase.entries.firstOrNull { it.name == json.optString("phase") } ?: FocusPhase.FOCUS
        PomodoroState(
            focusMinutes = focus,
            breakMinutes = brk,
            phase = phase,
            session = json.optInt("session", 1).coerceIn(1, 4),
            countdown = readCountdown(json.getJSONObject("countdown"), shift),
        )
    }.getOrDefault(PomodoroState())

    private fun readStopwatch(shift: Long): Stopwatch = runCatching {
        val raw = prefs.getString(KEY_STOPWATCH, null) ?: return Stopwatch()
        val json = JSONObject(raw)
        val laps = json.optJSONArray("laps")
        Stopwatch(
            accumulatedMs = json.optLong("accumulated", 0L).coerceAtLeast(0L),
            startedAt = if (json.has("startedAt")) json.getLong("startedAt") + shift else null,
            laps = if (laps == null) emptyList() else (0 until laps.length()).map { laps.getLong(it) },
        )
    }.getOrDefault(Stopwatch())

    private companion object {
        const val FINISH_TOLERANCE_MS = 500L
        const val KIND_POMODORO = "pomodoro"
        const val KIND_TIMER = "timer"
        const val NOTIFICATION_POMODORO = 1001
        const val NOTIFICATION_TIMER = 1002
        const val KEY_POMODORO = "pomodoro"
        const val KEY_TIMER = "timer"
        const val KEY_STOPWATCH = "stopwatch"
        const val KEY_CLOCK_ELAPSED = "clock_elapsed"
        const val KEY_CLOCK_WALL = "clock_wall"
        const val KEY_BOOT = "boot_count"
    }
}
