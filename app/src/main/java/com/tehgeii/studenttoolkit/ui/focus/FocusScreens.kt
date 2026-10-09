@file:OptIn(ExperimentalMaterial3Api::class)

package com.tehgeii.studenttoolkit.ui.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tehgeii.studenttoolkit.core.Fmt
import com.tehgeii.studenttoolkit.data.FocusRepository
import com.tehgeii.studenttoolkit.domain.focus.Countdown
import com.tehgeii.studenttoolkit.domain.focus.DayFocus
import com.tehgeii.studenttoolkit.domain.focus.FocusPhase
import com.tehgeii.studenttoolkit.domain.focus.PomodoroState
import com.tehgeii.studenttoolkit.notify.FocusEngine
import com.tehgeii.studenttoolkit.ui.LocalContainer
import com.tehgeii.studenttoolkit.ui.components.AppIcons
import com.tehgeii.studenttoolkit.ui.components.AppScaffold
import com.tehgeii.studenttoolkit.ui.components.ChoiceChips
import com.tehgeii.studenttoolkit.ui.components.ExactAlarmCard
import com.tehgeii.studenttoolkit.ui.components.FormColumn
import com.tehgeii.studenttoolkit.ui.components.NotificationPermissionCard
import com.tehgeii.studenttoolkit.ui.components.NumberField
import com.tehgeii.studenttoolkit.ui.components.SectionTitle
import com.tehgeii.studenttoolkit.ui.components.rememberElapsedNow
import java.time.LocalDate

class FocusViewModel(private val engine: FocusEngine, focusStats: FocusRepository) : ViewModel() {
    val pomodoro = engine.pomodoro
    val timer = engine.timer
    val stopwatch = engine.stopwatch
    val stats = focusStats.stats

    fun startPomodoro() = engine.startPomodoro()
    fun pausePomodoro() = engine.pausePomodoro()
    fun resetPomodoro() = engine.resetPomodoro()
    fun skipPomodoro() = engine.skipPomodoro()
    fun setPomodoroDurations(focus: Int, brk: Int) = engine.setPomodoroDurations(focus, brk)

    fun setTimer(ms: Long) = engine.setTimerDuration(ms)
    fun startTimer() = engine.startTimer()
    fun pauseTimer() = engine.pauseTimer()
    fun resetTimer() = engine.resetTimer()

    fun startStopwatch() = engine.startStopwatch()
    fun pauseStopwatch() = engine.pauseStopwatch()
    fun lapStopwatch() = engine.lapStopwatch()
    fun resetStopwatch() = engine.resetStopwatch()

    fun checkFinished() = engine.checkFinished()
}

@Composable
private fun focusViewModel(): FocusViewModel {
    val container = LocalContainer.current
    return viewModel { FocusViewModel(container.focus, container.focusStats) }
}

private val tabular = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")

@Composable
private fun Dial(progress: Float, top: String, time: String, bottom: String?) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.size(248.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 10.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(top, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    time,
                    style = MaterialTheme.typography.displayMedium.merge(tabular),
                    fontWeight = FontWeight.Bold,
                )
                if (bottom != null) Text(bottom, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ControlButton(text: String, icon: ImageVector, primary: Boolean, modifier: Modifier, onClick: () -> Unit) {
    if (primary) {
        Button(onClick = onClick, modifier = modifier) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(text)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(text)
        }
    }
}

/** Starts/pauses/resumes a countdown: one button whose label follows the state. */
@Composable
private fun RowScopeStartPause(countdown: Countdown, modifier: Modifier, onStart: () -> Unit, onPause: () -> Unit) {
    when {
        countdown.isRunning -> ControlButton("Pause", AppIcons.Pause, true, modifier, onPause)
        countdown.isFresh -> ControlButton("Start", Icons.Filled.PlayArrow, true, modifier, onStart)
        else -> ControlButton("Resume", Icons.Filled.PlayArrow, true, modifier, onStart)
    }
}

// ---- Pomodoro ---------------------------------------------------------------------------------

@Composable
fun PomodoroScreen(onBack: () -> Unit) {
    val vm = focusViewModel()
    val state by vm.pomodoro.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val countdown = state.countdown
    val running = countdown.isRunning
    val now = rememberElapsedNow(active = running, intervalMs = 250L)
    val remaining = countdown.remaining(now)
    val finished = running && remaining == 0L
    LaunchedEffect(finished) { if (finished) vm.checkFinished() }
    var customDialog by rememberSaveable { mutableStateOf(false) }

    AppScaffold(title = "Pomodoro", onBack = onBack) { padding ->
        FormColumn(padding) {
            if (!running) {
                val presets = PomodoroState.PRESETS.map { "${it.first} / ${it.second}" } + "Custom"
                val current = "${state.focusMinutes} / ${state.breakMinutes}"
                ChoiceChips(
                    options = presets,
                    selected = if (current in presets) current else "Custom",
                    label = { if (it == "Custom" && current !in presets) "Custom ($current)" else it },
                    onSelect = { option ->
                        val preset = PomodoroState.PRESETS.firstOrNull { "${it.first} / ${it.second}" == option }
                        if (preset == null) customDialog = true else vm.setPomodoroDurations(preset.first, preset.second)
                    },
                )
            }
            Dial(
                progress = countdown.progress(now),
                top = if (state.phase == FocusPhase.FOCUS) "FOCUS" else "BREAK",
                time = Fmt.countdown(remaining),
                bottom = "Session ${state.session} of ${state.sessionsPerCycle}",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RowScopeStartPause(countdown, Modifier.weight(1.4f), vm::startPomodoro, vm::pausePomodoro)
                ControlButton("Reset", Icons.Filled.Refresh, false, Modifier.weight(1f), vm::resetPomodoro)
                ControlButton("Skip", AppIcons.SkipNext, false, Modifier.weight(1f), vm::skipPomodoro)
            }
            NotificationPermissionCard("Allow them to hear when a session ends.")
            ExactAlarmCard()
            if (!running) FocusStatsCard(stats)
        }
    }

    if (customDialog) {
        var focus by rememberSaveable { mutableStateOf(state.focusMinutes.toString()) }
        var brk by rememberSaveable { mutableStateOf(state.breakMinutes.toString()) }
        val focusValue = focus.toIntOrNull()
        val breakValue = brk.toIntOrNull()
        val valid = focusValue != null && focusValue in 1..240 && breakValue != null && breakValue in 1..120
        AlertDialog(
            onDismissRequest = { customDialog = false },
            title = { Text("Custom durations") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(focus, { focus = it }, "Focus (1–240)", Modifier.fillMaxWidth(), decimal = false, suffix = "min")
                    NumberField(brk, { brk = it }, "Break (1–120)", Modifier.fillMaxWidth(), decimal = false, suffix = "min")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (focusValue != null && breakValue != null) vm.setPomodoroDurations(focusValue, breakValue)
                        customDialog = false
                    },
                    enabled = valid,
                ) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { customDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun FocusStatsCard(stats: Map<String, DayFocus>) {
    val today = LocalDate.now()
    val todayStats = stats[today.toString()] ?: DayFocus()
    val week = (0L..6L).map { stats[today.minusDays(it).toString()] ?: DayFocus() }.fold(DayFocus()) { a, b -> a + b }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Focus statistics", style = MaterialTheme.typography.titleMedium)
            StatLine("Today", todayStats)
            HorizontalDivider()
            StatLine("Last 7 days", week)
        }
    }
}

@Composable
private fun StatLine(label: String, day: DayFocus) {
    Row {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(
            "${day.sessions} session${if (day.sessions == 1) "" else "s"} · ${Fmt.duration(day.minutes)} focused",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

// ---- Timer ------------------------------------------------------------------------------------

@Composable
fun TimerScreen(onBack: () -> Unit) {
    val vm = focusViewModel()
    val timer by vm.timer.collectAsStateWithLifecycle()
    val running = timer.isRunning
    val now = rememberElapsedNow(active = running, intervalMs = 250L)
    val remaining = timer.remaining(now)
    val finished = running && remaining == 0L
    LaunchedEffect(finished) { if (finished) vm.checkFinished() }

    var hours by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableStateOf("") }
    var seconds by rememberSaveable { mutableStateOf("") }

    AppScaffold(title = "Timer", onBack = onBack) { padding ->
        FormColumn(padding) {
            if (timer.isFresh) {
                val presets = listOf(1, 5, 10, 15, 30, 60)
                ChoiceChips(
                    options = presets,
                    selected = presets.firstOrNull { it * 60_000L == timer.durationMs },
                    label = { if (it == 60) "1 hour" else "$it min" },
                    onSelect = { vm.setTimer(it * 60_000L) },
                )
            }
            Dial(
                progress = timer.progress(now),
                top = if (running) "RUNNING" else if (timer.isFresh) "READY" else "PAUSED",
                time = Fmt.countdown(remaining),
                bottom = null,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RowScopeStartPause(timer, Modifier.weight(1.4f), vm::startTimer, vm::pauseTimer)
                ControlButton("Reset", Icons.Filled.Refresh, false, Modifier.weight(1f), vm::resetTimer)
            }
            if (timer.isFresh) {
                SectionTitle("Custom")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    NumberField(hours, { hours = it.take(2) }, "h", Modifier.weight(1f), decimal = false)
                    NumberField(minutes, { minutes = it.take(2) }, "min", Modifier.weight(1f), decimal = false)
                    NumberField(seconds, { seconds = it.take(2) }, "s", Modifier.weight(1f), decimal = false)
                    val total = ((hours.toLongOrNull() ?: 0) * 3600 + (minutes.toLongOrNull() ?: 0) * 60 + (seconds.toLongOrNull() ?: 0)) * 1000
                    TextButton(onClick = { vm.setTimer(total) }, enabled = total in 1_000L..(24 * 3_600_000L)) { Text("Set") }
                }
            }
            NotificationPermissionCard("Allow them to hear when the timer ends.")
            ExactAlarmCard()
        }
    }
}

// ---- Stopwatch --------------------------------------------------------------------------------

@Composable
fun StopwatchScreen(onBack: () -> Unit) {
    val vm = focusViewModel()
    val sw by vm.stopwatch.collectAsStateWithLifecycle()
    val now = rememberElapsedNow(active = sw.isRunning, intervalMs = 100L)
    val elapsed = sw.elapsed(now)

    AppScaffold(title = "Stopwatch", onBack = onBack) { padding ->
        FormColumn(padding) {
            Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                Text(Fmt.stopwatch(elapsed), style = MaterialTheme.typography.displayLarge.merge(tabular), fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    sw.isRunning -> {
                        ControlButton("Pause", AppIcons.Pause, true, Modifier.weight(1f), vm::pauseStopwatch)
                        ControlButton("Lap", AppIcons.Timer, false, Modifier.weight(1f), vm::lapStopwatch)
                    }
                    elapsed == 0L -> ControlButton("Start", Icons.Filled.PlayArrow, true, Modifier.weight(1f), vm::startStopwatch)
                    else -> {
                        ControlButton("Resume", Icons.Filled.PlayArrow, true, Modifier.weight(1f), vm::startStopwatch)
                        ControlButton("Reset", Icons.Filled.Refresh, false, Modifier.weight(1f), vm::resetStopwatch)
                    }
                }
            }
            if (sw.laps.isNotEmpty()) {
                SectionTitle("Laps")
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        sw.laps.indices.reversed().forEach { i ->
                            val split = sw.laps[i] - (if (i == 0) 0L else sw.laps[i - 1])
                            Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                Text("Lap ${i + 1}", modifier = Modifier.weight(1f))
                                Text("+" + Fmt.stopwatch(split), style = tabular, modifier = Modifier.weight(1f))
                                Text(Fmt.stopwatch(sw.laps[i]), style = tabular, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}
