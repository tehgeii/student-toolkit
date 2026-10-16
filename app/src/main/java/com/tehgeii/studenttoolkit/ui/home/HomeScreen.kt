@file:OptIn(ExperimentalMaterial3Api::class)

package com.tehgeii.studenttoolkit.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tehgeii.studenttoolkit.AppContainer
import com.tehgeii.studenttoolkit.core.Fmt
import com.tehgeii.studenttoolkit.domain.academic.GpaCalculator
import com.tehgeii.studenttoolkit.domain.focus.FocusPhase
import com.tehgeii.studenttoolkit.domain.tasks.DeadlineStatus
import com.tehgeii.studenttoolkit.domain.tasks.Deadlines
import com.tehgeii.studenttoolkit.domain.tasks.Task
import com.tehgeii.studenttoolkit.domain.tasks.TaskOrganizer
import com.tehgeii.studenttoolkit.i18n.Lang
import com.tehgeii.studenttoolkit.i18n.dueText
import com.tehgeii.studenttoolkit.i18n.durationShort
import com.tehgeii.studenttoolkit.i18n.greeting
import com.tehgeii.studenttoolkit.i18n.longDate
import com.tehgeii.studenttoolkit.ui.LocalContainer
import com.tehgeii.studenttoolkit.ui.LocalLang
import com.tehgeii.studenttoolkit.ui.Navigator
import com.tehgeii.studenttoolkit.ui.Routes
import com.tehgeii.studenttoolkit.ui.components.AppScaffold
import com.tehgeii.studenttoolkit.ui.components.FormColumn
import com.tehgeii.studenttoolkit.ui.components.SectionTitle
import com.tehgeii.studenttoolkit.ui.components.decorative
import com.tehgeii.studenttoolkit.ui.components.rememberWallNow
import java.time.Instant
import java.time.ZoneId

class HomeViewModel(container: AppContainer) : ViewModel() {
    val settings = container.settings.state
    val tasks = container.tasks.tasks
    val academic = container.academic.data
    val focusStats = container.focusStats.stats
    val pomodoro = container.focus.pomodoro
    val timer = container.focus.timer
}

private data class QuickTool(val emoji: String, val en: String, val id: String, val route: String)

private val quickTools = listOf(
    QuickTool("📊", "Grade", "Nilai", Routes.GRADE),
    QuickTool("🎓", "GPA", "IPK", Routes.ACADEMIC),
    QuickTool("➕", "Add task", "Tugas baru", Routes.TASK_NEW),
    QuickTool("🍅", "Pomodoro", "Pomodoro", Routes.POMODORO),
    QuickTool("🎲", "Random", "Acak", Routes.randomizer(0)),
    QuickTool("🔳", "QR", "QR", Routes.QR),
    QuickTool("⏱️", "Timer", "Timer", Routes.TIMER),
    QuickTool("📝", "Notes", "Catatan", Routes.NOTES),
)

@Composable
fun HomeScreen(nav: Navigator) {
    val container = LocalContainer.current
    val vm: HomeViewModel = viewModel { HomeViewModel(container) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val academic by vm.academic.collectAsStateWithLifecycle()
    val focusStats by vm.focusStats.collectAsStateWithLifecycle()
    val pomodoro by vm.pomodoro.collectAsStateWithLifecycle()
    val timer by vm.timer.collectAsStateWithLifecycle()

    val t = LocalLang.current
    val now = rememberWallNow()
    val zone = ZoneId.systemDefault()
    val dateTime = Instant.ofEpochMilli(now).atZone(zone)
    val greeting = t.greeting(dateTime.hour)
    val name = settings.profile.name.trim()

    AppScaffold(
        title = "Student Toolkit",
        actions = {
            IconButton(onClick = { nav.navigate(Routes.SEARCH) }) {
                Icon(Icons.Filled.Search, contentDescription = t("Search", "Cari"))
            }
            IconButton(onClick = { nav.navigate(Routes.SETTINGS) }) {
                Icon(Icons.Filled.Settings, contentDescription = t("Settings", "Pengaturan"))
            }
        },
    ) { padding ->
        FormColumn(padding) {
            Column {
                Text(
                    if (name.isEmpty()) "$greeting 👋" else "$greeting, $name 👋",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    t.longDate(dateTime.toLocalDate()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (pomodoro.countdown.isRunning || timer.isRunning) {
                Card(
                    onClick = { nav.navigate(if (pomodoro.countdown.isRunning) Routes.POMODORO else Routes.TIMER) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        when {
                            pomodoro.countdown.isRunning && pomodoro.phase == FocusPhase.FOCUS ->
                                t(
                                    "⏳ Focus session ${pomodoro.session} of ${pomodoro.sessionsPerCycle} is running",
                                    "⏳ Sesi fokus ${pomodoro.session} dari ${pomodoro.sessionsPerCycle} sedang berjalan",
                                )
                            pomodoro.countdown.isRunning -> t("☕ Break is running", "☕ Istirahat sedang berjalan")
                            else -> t("⏱️ Timer is running", "⏱️ Timer sedang berjalan")
                        },
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            DueSoonCard(tasks, now, zone, nav, t)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val gpa = GpaCalculator.cumulative(academic.semesters, settings.gradeScale)
                StatCard(
                    title = t("GPA (IPK)", "IPK"),
                    value = gpa.gpa?.let { Fmt.fixed(it, settings.gpaDecimals) } ?: "—",
                    caption = if (gpa.totalCredits > 0) "${gpa.totalCredits} SKS" else t("Add semesters", "Tambah semester"),
                    modifier = Modifier.weight(1f),
                    onClick = { nav.selectTab(Routes.ACADEMIC) },
                )
                val today = focusStats[dateTime.toLocalDate().toString()]
                StatCard(
                    title = t("Focus today", "Fokus hari ini"),
                    value = t.durationShort(today?.minutes ?: 0),
                    caption = t.count(today?.sessions ?: 0, "session", "sessions", "sesi"),
                    modifier = Modifier.weight(1f),
                    onClick = { nav.navigate(Routes.POMODORO) },
                )
            }

            SectionTitle(t("Quick tools", "Alat cepat"))
            QuickToolGrid(t) { nav.navigate(it) }
        }
    }
}

@Composable
private fun DueSoonCard(tasks: List<Task>, now: Long, zone: ZoneId, nav: Navigator, t: Lang) {
    val upcoming = TaskOrganizer.upcoming(tasks)
    val thisWeek = upcoming.count { task ->
        val deadline = task.deadline ?: return@count false
        deadline >= now && Deadlines.daysUntil(deadline, now, zone) <= 6
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t("Due soon", "Segera jatuh tempo"), style = MaterialTheme.typography.titleMedium)
                    Text(
                        when (thisWeek) {
                            0 -> t("No deadlines this week", "Tidak ada deadline minggu ini")
                            else -> t.count(thisWeek, "deadline", "deadlines", "deadline") + t(" this week", " minggu ini")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { nav.selectTab(Routes.TASKS) }) { Text(t("All tasks", "Semua tugas")) }
            }
            if (upcoming.isEmpty()) {
                Text(t("Nothing due. Enjoy the free time 🎉", "Tidak ada deadline. Nikmati waktu luangmu 🎉"), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { nav.navigate(Routes.TASK_NEW) }) { Text(t("+ Add task", "+ Tambah tugas")) }
            } else {
                upcoming.take(3).forEachIndexed { index, task ->
                    if (index > 0) HorizontalDivider()
                    val status = Deadlines.status(task, now, zone)
                    val marker = when (status) {
                        DeadlineStatus.OVERDUE, DeadlineStatus.DUE_TODAY, DeadlineStatus.ONE_DAY_LEFT -> "⚠️"
                        DeadlineStatus.DUE_SOON -> "🟡"
                        else -> "🟢"
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { nav.navigate(Routes.task(task.id)) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(marker)
                        Column(Modifier.weight(1f)) {
                            Text(task.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                t.dueText(task.deadline ?: now, now, zone),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, caption: String, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QuickToolGrid(t: Lang, onOpen: (String) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = (maxWidth / 88.dp).toInt().coerceIn(4, 8)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            quickTools.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { tool ->
                        Card(onClick = { onOpen(tool.route) }, modifier = Modifier.weight(1f)) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(tool.emoji, fontSize = 24.sp, modifier = Modifier.decorative())
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    t(tool.en, tool.id),
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
