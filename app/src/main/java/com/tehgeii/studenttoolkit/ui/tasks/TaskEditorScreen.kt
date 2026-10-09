@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.tehgeii.studenttoolkit.ui.tasks

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.tehgeii.studenttoolkit.core.Fmt
import com.tehgeii.studenttoolkit.core.newId
import com.tehgeii.studenttoolkit.domain.tasks.Priority
import com.tehgeii.studenttoolkit.domain.tasks.ReminderPresets
import com.tehgeii.studenttoolkit.domain.tasks.Subtask
import com.tehgeii.studenttoolkit.domain.tasks.Task
import com.tehgeii.studenttoolkit.ui.LocalContainer
import com.tehgeii.studenttoolkit.ui.LocalMessenger
import com.tehgeii.studenttoolkit.ui.Navigator
import com.tehgeii.studenttoolkit.ui.components.AppScaffold
import com.tehgeii.studenttoolkit.ui.components.ConfirmDialog
import com.tehgeii.studenttoolkit.ui.components.EmptyState
import com.tehgeii.studenttoolkit.ui.components.FormColumn
import com.tehgeii.studenttoolkit.ui.components.NotificationPermissionCard
import com.tehgeii.studenttoolkit.ui.components.NumberField
import com.tehgeii.studenttoolkit.ui.components.SectionTitle
import com.tehgeii.studenttoolkit.ui.components.Segmented
import com.tehgeii.studenttoolkit.ui.components.TextInput
import java.io.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

private data class SubtaskForm(val id: String, val title: String, val done: Boolean) : Serializable

/** Everything the editor holds, saveable across rotation and process death. */
private data class TaskForm(
    val title: String = "",
    val course: String = "",
    val deadline: Long? = null,
    val priority: Priority = Priority.MEDIUM,
    val hours: String = "",
    val minutes: String = "",
    val notes: String = "",
    val completed: Boolean = false,
    val subtasks: List<SubtaskForm> = emptyList(),
    val reminders: List<Int> = emptyList(),
) : Serializable

private fun Task.toForm() = TaskForm(
    title = title,
    course = course,
    deadline = deadline,
    priority = priority,
    hours = estimateMinutes?.let { (it / 60).takeIf { h -> h > 0 }?.toString() } ?: "",
    minutes = estimateMinutes?.let { (it % 60).takeIf { m -> m > 0 }?.toString() } ?: "",
    notes = notes,
    completed = completed,
    subtasks = subtasks.map { SubtaskForm(it.id, it.title, it.done) },
    reminders = reminders,
)

private fun atEndOfDay(date: LocalDate, zone: ZoneId): Long =
    date.atTime(23, 59).atZone(zone).toInstant().toEpochMilli()

@Composable
fun TaskEditorScreen(idParam: String, nav: Navigator, onBack: () -> Unit) {
    val vm = tasksViewModel()
    val container = LocalContainer.current
    val messenger = LocalMessenger.current
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()
    val isNew = idParam == "new"
    val existing = if (isNew) null else vm.get(idParam)

    if (!isNew && existing == null) {
        AppScaffold(title = "Task", onBack = onBack) { padding ->
            FormColumn(padding) { EmptyState("🤔", "Task not found", "It may have been deleted.") }
        }
        return
    }

    val original = rememberSaveable(idParam) { existing?.toForm() ?: TaskForm() }
    var form by rememberSaveable(idParam) { mutableStateOf(original) }
    var titleError by rememberSaveable { mutableStateOf(false) }
    var askDiscard by rememberSaveable { mutableStateOf(false) }
    var askDelete by rememberSaveable { mutableStateOf(false) }
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showTime by rememberSaveable { mutableStateOf(false) }
    var pickedDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var newSubtask by rememberSaveable { mutableStateOf("") }

    val dirty = form != original
    val leave: () -> Unit = {
        if (dirty) {
            askDiscard = true
        } else {
            onBack()
        }
    }
    BackHandler(enabled = dirty) { askDiscard = true }

    fun setDeadline(millis: Long?) {
        val firstDeadline = form.deadline == null && millis != null
        form = form.copy(
            deadline = millis,
            reminders = if (firstDeadline && form.reminders.isEmpty()) container.settings.value.defaultReminders else form.reminders,
        )
    }

    fun save() {
        val title = form.title.trim()
        if (title.isEmpty()) {
            titleError = true
            return
        }
        val now = System.currentTimeMillis()
        val estimate = ((form.hours.toIntOrNull() ?: 0) * 60 + (form.minutes.toIntOrNull() ?: 0)).takeIf { it > 0 }
        val base = existing ?: Task(id = newId(), title = title, createdAt = now)
        val task = base.copy(
            title = title,
            course = form.course.trim(),
            deadline = form.deadline,
            priority = form.priority,
            estimateMinutes = estimate,
            notes = form.notes.trim(),
            completed = form.completed,
            completedAt = if (form.completed) (existing?.completedAt ?: now) else null,
            subtasks = form.subtasks.filter { it.title.isNotBlank() }.map { Subtask(it.id, it.title.trim(), it.done) },
            reminders = if (form.deadline != null) form.reminders.distinct().sorted() else emptyList(),
        )
        vm.save(task)
        messenger.show(if (existing == null) "✓ Task added" else "✓ Task saved")
        onBack()
    }

    AppScaffold(
        title = if (isNew) "New task" else "Edit task",
        onBack = leave,
        actions = {
            if (existing != null) {
                IconButton(onClick = { askDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = "Delete task") }
            }
            TextButton(onClick = ::save) { Text("Save") }
        },
    ) { padding ->
        FormColumn(padding) {
            TextInput(
                form.title,
                {
                    form = form.copy(title = it)
                    titleError = false
                },
                "Title",
                isError = titleError,
                supportingText = if (titleError) "Give the task a title" else null,
            )
            TextInput(form.course, { form = form.copy(course = it) }, "Course (optional)")

            SectionTitle("Deadline")
            val deadline = form.deadline
            if (deadline == null) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val today = LocalDate.now(zone)
                    AssistChip(onClick = { setDeadline(atEndOfDay(today, zone)) }, label = { Text("Today 23:59") })
                    AssistChip(onClick = { setDeadline(atEndOfDay(today.plusDays(1), zone)) }, label = { Text("Tomorrow") })
                    AssistChip(onClick = { setDeadline(atEndOfDay(today.plusWeeks(1), zone)) }, label = { Text("In a week") })
                    AssistChip(onClick = { showDate = true }, label = { Text("Pick date…") })
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { showDate = true }, modifier = Modifier.weight(1f)) {
                        Text("📅 ${Fmt.dateTime(deadline, zone)}")
                    }
                    IconButton(onClick = { setDeadline(null) }) { Icon(Icons.Filled.Close, contentDescription = "Remove deadline") }
                }
                SectionTitle("Remind me before")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReminderPresets.options.forEach { minutes ->
                        val selected = minutes in form.reminders
                        FilterChip(
                            selected = selected,
                            onClick = {
                                form = form.copy(reminders = if (selected) form.reminders - minutes else form.reminders + minutes)
                            },
                            label = { Text(ReminderPresets.label(minutes)) },
                        )
                    }
                }
                if (form.reminders.isNotEmpty()) {
                    NotificationPermissionCard("Allow them to get deadline reminders.")
                }
            }

            SectionTitle("Priority")
            Segmented(Priority.entries.toList(), form.priority, { it.label }, { form = form.copy(priority = it) })

            SectionTitle("Estimated time")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField(form.hours, { form = form.copy(hours = it.take(3)) }, "Hours", Modifier.weight(1f), decimal = false)
                NumberField(
                    form.minutes,
                    { form = form.copy(minutes = (it.toIntOrNull()?.coerceAtMost(59)?.toString() ?: "")) },
                    "Minutes",
                    Modifier.weight(1f),
                    decimal = false,
                )
            }

            SectionTitle("Steps")
            form.subtasks.forEach { sub ->
                key(sub.id) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = sub.done,
                            onCheckedChange = { done -> form = form.copy(subtasks = form.subtasks.map { if (it.id == sub.id) it.copy(done = done) else it }) },
                        )
                        TextInput(
                            sub.title,
                            { text -> form = form.copy(subtasks = form.subtasks.map { if (it.id == sub.id) it.copy(title = text) else it }) },
                            "Step",
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { form = form.copy(subtasks = form.subtasks.filterNot { it.id == sub.id }) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove step")
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextInput(
                    newSubtask,
                    { newSubtask = it },
                    "Add a step",
                    modifier = Modifier.weight(1f),
                    imeAction = ImeAction.Done,
                )
                IconButton(
                    onClick = {
                        if (newSubtask.isNotBlank()) {
                            form = form.copy(subtasks = form.subtasks + SubtaskForm(newId(), newSubtask.trim(), false))
                            newSubtask = ""
                        }
                    },
                ) { Icon(Icons.Filled.Add, contentDescription = "Add step") }
            }

            TextInput(form.notes, { form = form.copy(notes = it) }, "Notes", singleLine = false, minLines = 3)

            if (existing != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Completed", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = form.completed, onCheckedChange = { form = form.copy(completed = it) })
                }
            }

            Button(onClick = ::save, modifier = Modifier.fillMaxWidth()) { Text(if (isNew) "Add task" else "Save changes") }
        }
    }

    if (showDate) {
        val initialDate = form.deadline?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() } ?: LocalDate.now(zone)
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickedDay = dateState.selectedDateMillis?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                        }
                        showDate = false
                        if (pickedDay != null) showTime = true
                    },
                    enabled = dateState.selectedDateMillis != null,
                ) { Text("Next") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showTime) {
        val initialTime = form.deadline?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalTime() } ?: LocalTime.of(23, 59)
        val timeState = rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { showTime = false },
            title = { Text("Deadline time") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    val day = pickedDay
                    if (day != null) {
                        val date = LocalDate.ofEpochDay(day)
                        setDeadline(date.atTime(timeState.hour, timeState.minute).atZone(zone).toInstant().toEpochMilli())
                    }
                    showTime = false
                }) { Text("Set") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Cancel") } },
        )
    }

    if (askDiscard) {
        ConfirmDialog(
            title = "Discard changes?",
            text = "Your changes to this task have not been saved.",
            confirmLabel = "Discard",
            destructive = true,
            onDismiss = { askDiscard = false },
            onConfirm = onBack,
        )
    }

    if (askDelete && existing != null) {
        ConfirmDialog(
            title = "Delete task?",
            text = "\"${existing.title}\" will be deleted.",
            confirmLabel = "Delete",
            destructive = true,
            onDismiss = { askDelete = false },
            onConfirm = {
                onBack()
                val removed = vm.delete(existing.id)
                if (removed != null) messenger.show("Task deleted", "Undo") { vm.restore(removed) }
            },
        )
    }
}
