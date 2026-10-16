@file:OptIn(ExperimentalMaterial3Api::class)

package com.tehgeii.studenttoolkit.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tehgeii.studenttoolkit.core.Fmt
import com.tehgeii.studenttoolkit.data.SettingsRepository
import com.tehgeii.studenttoolkit.data.TaskRepository
import com.tehgeii.studenttoolkit.domain.tasks.DeadlineStatus
import com.tehgeii.studenttoolkit.domain.tasks.Deadlines
import com.tehgeii.studenttoolkit.domain.tasks.Priority
import com.tehgeii.studenttoolkit.domain.tasks.Task
import com.tehgeii.studenttoolkit.domain.tasks.TaskOrganizer
import com.tehgeii.studenttoolkit.domain.tasks.TaskSection
import com.tehgeii.studenttoolkit.domain.tasks.TaskSort
import com.tehgeii.studenttoolkit.i18n.Lang
import com.tehgeii.studenttoolkit.i18n.dateTime
import com.tehgeii.studenttoolkit.i18n.dueText
import com.tehgeii.studenttoolkit.i18n.durationShort
import com.tehgeii.studenttoolkit.i18n.priority
import com.tehgeii.studenttoolkit.i18n.status
import com.tehgeii.studenttoolkit.i18n.taskSection
import com.tehgeii.studenttoolkit.i18n.taskSort
import com.tehgeii.studenttoolkit.ui.LocalContainer
import com.tehgeii.studenttoolkit.ui.LocalLang
import com.tehgeii.studenttoolkit.ui.LocalMessenger
import com.tehgeii.studenttoolkit.ui.Navigator
import com.tehgeii.studenttoolkit.ui.Routes
import com.tehgeii.studenttoolkit.ui.components.AppIcons
import com.tehgeii.studenttoolkit.ui.components.AppScaffold
import com.tehgeii.studenttoolkit.ui.components.EmptyState
import com.tehgeii.studenttoolkit.ui.components.FormColumn
import com.tehgeii.studenttoolkit.ui.components.ListColumn
import com.tehgeii.studenttoolkit.ui.components.SectionTitle
import com.tehgeii.studenttoolkit.ui.components.StatusPill
import com.tehgeii.studenttoolkit.ui.components.readable
import com.tehgeii.studenttoolkit.ui.components.rememberWallNow
import com.tehgeii.studenttoolkit.ui.theme.StatusColors
import java.time.ZoneId

class TasksViewModel(private val repo: TaskRepository, private val settingsRepo: SettingsRepository) : ViewModel() {
    val tasks = repo.tasks
    val settings = settingsRepo.state

    fun get(id: String): Task? = repo.get(id)
    fun save(task: Task) = repo.upsert(task)
    fun setCompleted(id: String, completed: Boolean) = repo.setCompleted(id, completed)
    fun delete(id: String): Task? = repo.delete(id)
    fun restore(task: Task) = repo.restore(task)
    fun setSort(sort: TaskSort) = settingsRepo.update { it.copy(taskSort = sort) }
}

@Composable
fun tasksViewModel(): TasksViewModel {
    val container = LocalContainer.current
    return viewModel { TasksViewModel(container.tasks, container.settings) }
}

fun statusColor(status: DeadlineStatus): Color = when (status) {
    DeadlineStatus.OVERDUE, DeadlineStatus.DUE_TODAY -> StatusColors.danger
    DeadlineStatus.ONE_DAY_LEFT, DeadlineStatus.DUE_SOON -> StatusColors.warning
    DeadlineStatus.UPCOMING -> StatusColors.ok
    DeadlineStatus.NO_DEADLINE, DeadlineStatus.COMPLETED -> StatusColors.neutral
}

fun priorityColor(priority: Priority): Color = when (priority) {
    Priority.HIGH -> StatusColors.danger
    Priority.MEDIUM -> StatusColors.warning
    Priority.LOW -> StatusColors.neutral
}

@Composable
fun TasksScreen(nav: Navigator) {
    val t = LocalLang.current
    val vm = tasksViewModel()
    val messenger = LocalMessenger.current
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val now = rememberWallNow()
    val zone = ZoneId.systemDefault()
    var showCompleted by rememberSaveable { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }

    val onToggle: (Task, Boolean) -> Unit = { task, done ->
        vm.setCompleted(task.id, done)
        if (done) messenger.show(t("✓ Task completed", "✓ Tugas selesai"), t("Undo", "Urungkan")) { vm.setCompleted(task.id, false) }
    }

    AppScaffold(
        title = t("Tasks", "Tugas"),
        actions = {
            Box {
                IconButton(onClick = { sortMenu = true }) { Icon(AppIcons.Sort, contentDescription = t("Sort", "Urutkan")) }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    TaskSort.entries.forEach { sort ->
                        DropdownMenuItem(
                            text = { Text((if (sort == settings.taskSort) "✓ " else "    ") + t.taskSort(sort)) },
                            onClick = {
                                sortMenu = false
                                vm.setSort(sort)
                            },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.navigate(Routes.TASK_NEW) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(t("Add task", "Tambah tugas")) },
            )
        },
    ) { padding ->
        if (tasks.isEmpty()) {
            FormColumn(padding) {
                EmptyState(
                    emoji = "✅",
                    title = t("No tasks yet", "Belum ada tugas"),
                    message = t("Add your first task and its deadline. Reminders work offline.", "Tambahkan tugas pertama beserta deadline-nya. Pengingat tetap jalan tanpa internet."),
                    actionLabel = t("Add task", "Tambah tugas"),
                    onAction = { nav.navigate(Routes.TASK_NEW) },
                )
            }
            return@AppScaffold
        }
        val sections = TaskOrganizer.organize(tasks, settings.taskSort, now, zone)
        ListColumn(padding) {
            sections.forEach { (section, sectionTasks) ->
                if (section == TaskSection.COMPLETED) {
                    item(key = "header-completed") {
                        Row(
                            Modifier
                                .readable()
                                .clickable { showCompleted = !showCompleted },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SectionTitle(t.taskSection(section) + " (${sectionTasks.size})", Modifier.weight(1f))
                            Icon(
                                if (showCompleted) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = if (showCompleted) t("Hide completed", "Sembunyikan yang selesai") else t("Show completed", "Tampilkan yang selesai"),
                            )
                        }
                    }
                    if (showCompleted) {
                        items(sectionTasks, key = { it.id }) { task ->
                            TaskRow(task, now, zone, t, onToggle, onOpen = { nav.navigate(Routes.task(task.id)) })
                        }
                    }
                } else {
                    item(key = "header-${section.name}") {
                        SectionTitle(t.taskSection(section) + " (${sectionTasks.size})", Modifier.readable())
                    }
                    items(sectionTasks, key = { it.id }) { task ->
                        TaskRow(task, now, zone, t, onToggle, onOpen = { nav.navigate(Routes.task(task.id)) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(task: Task, now: Long, zone: ZoneId, t: Lang, onToggle: (Task, Boolean) -> Unit, onOpen: () -> Unit) {
    val status = Deadlines.status(task, now, zone)
    Card(onClick = onOpen, modifier = Modifier.readable()) {
        Row(Modifier.padding(end = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.completed, onCheckedChange = { onToggle(task, it) })
            Column(Modifier.weight(1f).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val details = buildList {
                    if (task.course.isNotBlank()) add(task.course)
                    task.deadline?.let { add(if (task.completed) t.dateTime(it, zone) else t.dueText(it, now, zone)) }
                    task.estimateMinutes?.let { add("~" + t.durationShort(it)) }
                    if (task.subtasks.isNotEmpty()) add("${task.subtasks.count { it.done }}/${task.subtasks.size} " + t("steps", "langkah"))
                }
                if (details.isNotEmpty()) {
                    Text(
                        details.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!task.completed) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (status != DeadlineStatus.NO_DEADLINE) StatusPill(t.status(status), statusColor(status))
                        StatusPill(t.priority(task.priority), priorityColor(task.priority))
                    }
                }
            }
        }
    }
}
