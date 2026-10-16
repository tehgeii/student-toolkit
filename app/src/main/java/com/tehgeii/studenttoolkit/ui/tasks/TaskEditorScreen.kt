@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.tehgeii.studenttoolkit.ui.tasks

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.tehgeii.studenttoolkit.core.Fmt
import com.tehgeii.studenttoolkit.core.newId
import com.tehgeii.studenttoolkit.domain.tasks.Priority
import com.tehgeii.studenttoolkit.domain.tasks.ReminderPresets
import com.tehgeii.studenttoolkit.domain.tasks.Subtask
import com.tehgeii.studenttoolkit.domain.tasks.Task
import com.tehgeii.studenttoolkit.i18n.dateTime
import com.tehgeii.studenttoolkit.i18n.durationWords
import com.tehgeii.studenttoolkit.i18n.priority
import com.tehgeii.studenttoolkit.ui.LocalContainer
import com.tehgeii.studenttoolkit.ui.LocalLang
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

/** Each reminder is one system alarm; a handful per task is plenty. */
private const val MAX_REMINDERS = 8

private fun atEndOfDay(date: LocalDate, zone: ZoneId): Long =
    date.atTime(23, 59).atZone(zone).toInstant().toEpochMilli()

@Composable
fun TaskEditorScreen(idParam: String, nav: Navigator, onBack: () -> Unit) {
    val t = LocalLang.current
    val vm = tasksViewModel()
    val container = LocalContainer.current
    val messenger = LocalMessenger.current
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()
    val isNew = idParam == "new"
    val existing = if (isNew) null else vm.get(idParam)

    if (!isNew && existing == null) {
        AppScaffold(title = t("Task", "Tugas"), onBack = onBack) { padding ->
            FormColumn(padding) { EmptyState("🤔", t("Task not found", "Tugas tidak ditemukan"), t("It may have been deleted.", "Mungkin sudah dihapus.")) }
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
    var customReminder by rememberSaveable { mutableStateOf(false) }

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
        messenger.show(if (existing == null) t("✓ Task added", "✓ Tugas ditambahkan") else t("✓ Task saved", "✓ Tugas disimpan"))
        onBack()
    }

    AppScaffold(
        title = if (isNew) t("New task", "Tugas baru") else t("Edit task", "Ubah tugas"),
        onBack = leave,
        actions = {
            if (existing != null) {
                IconButton(onClick = { askDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = t("Delete task", "Hapus tugas")) }
            }
            TextButton(onClick = ::save) { Text(t("Save", "Simpan")) }
        },
    ) { padding ->
        FormColumn(padding) {
            TextInput(
                form.title,
                {
                    form = form.copy(title = it)
                    titleError = false
                },
                t("Title", "Judul"),
                isError = titleError,
                supportingText = if (titleError) t("Give the task a title", "Isi judul tugasnya") else null,
            )
            TextInput(form.course, { form = form.copy(course = it) }, t("Course (optional)", "Mata kuliah (opsional)"))

            SectionTitle("Deadline")
            val deadline = form.deadline
            if (deadline == null) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val today = LocalDate.now(zone)
                    AssistChip(onClick = { setDeadline(atEndOfDay(today, zone)) }, label = { Text(t("Today 23:59", "Hari ini 23:59")) })
                    AssistChip(onClick = { setDeadline(atEndOfDay(today.plusDays(1), zone)) }, label = { Text(t("Tomorrow", "Besok")) })
                    AssistChip(onClick = { setDeadline(atEndOfDay(today.plusWeeks(1), zone)) }, label = { Text(t("In a week", "Seminggu lagi")) })
                    AssistChip(onClick = { showDate = true }, label = { Text(t("Pick date…", "Pilih tanggal…")) })
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { showDate = true }, modifier = Modifier.weight(1f)) {
                        Text("📅 ${t.dateTime(deadline, zone)}")
                    }
                    IconButton(onClick = { setDeadline(null) }) { Icon(Icons.Filled.Close, contentDescription = t("Remove deadline", "Hapus deadline")) }
                }
                SectionTitle(t("Remind me before", "Ingatkan sebelum"))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Presets plus any custom reminders this task already has.
                    (ReminderPresets.options + form.reminders).distinct().sorted().forEach { minutes ->
                        val selected = minutes in form.reminders
                        FilterChip(
                            selected = selected,
                            onClick = {
                                form = form.copy(reminders = if (selected) form.reminders - minutes else form.reminders + minutes)
                            },
                            label = { Text(t.durationWords(minutes)) },
                        )
                    }
                    AssistChip(
                        onClick = { customReminder = true },
                        enabled = form.reminders.size < MAX_REMINDERS,
                        label = { Text(t("Custom…", "Atur sendiri…")) },
                        leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    )
                }
                if (form.reminders.isNotEmpty()) {
                    NotificationPermissionCard(t("Allow them to get deadline reminders.", "Izinkan agar pengingat deadline bisa muncul."))
                }
            }

            SectionTitle(t("Priority", "Prioritas"))
            Segmented(Priority.entries.toList(), form.priority, { t.priority(it) }, { form = form.copy(priority = it) })

            SectionTitle(t("Estimated time", "Perkiraan waktu"))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField(form.hours, { form = form.copy(hours = it.take(3)) }, t("Hours", "Jam"), Modifier.weight(1f), decimal = false)
                NumberField(
                    form.minutes,
                    { form = form.copy(minutes = (it.toIntOrNull()?.coerceAtMost(59)?.toString() ?: "")) },
                    t("Minutes", "Menit"),
                    Modifier.weight(1f),
                    decimal = false,
                )
            }

            SectionTitle(t("Steps", "Langkah"))
            form.subtasks.forEach { sub ->
                key(sub.id) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = sub.done,
                            onCheckedChange = { done -> form = form.copy(subtasks = form.subtasks.map { if (it.id == sub.id) it.copy(done = done) else it }) },
                            modifier = Modifier.semantics { contentDescription = t("Step done: ${sub.title}", "Langkah selesai: ${sub.title}") },
                        )
                        TextInput(
                            sub.title,
                            { text -> form = form.copy(subtasks = form.subtasks.map { if (it.id == sub.id) it.copy(title = text) else it }) },
                            t("Step", "Langkah"),
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { form = form.copy(subtasks = form.subtasks.filterNot { it.id == sub.id }) }) {
                            Icon(Icons.Filled.Close, contentDescription = t("Remove step", "Hapus langkah"))
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextInput(
                    newSubtask,
                    { newSubtask = it },
                    t("Add a step", "Tambah langkah"),
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
                ) { Icon(Icons.Filled.Add, contentDescription = t("Add step", "Tambah langkah")) }
            }

            TextInput(form.notes, { form = form.copy(notes = it) }, t("Notes", "Catatan"), singleLine = false, minLines = 3)

            if (existing != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t("Completed", "Selesai"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = form.completed, onCheckedChange = { form = form.copy(completed = it) })
                }
            }

            Button(onClick = ::save, modifier = Modifier.fillMaxWidth()) { Text(if (isNew) t("Add task", "Tambah tugas") else t("Save changes", "Simpan perubahan")) }
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
                ) { Text(t("Next", "Lanjut")) }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text(t("Cancel", "Batal")) } },
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
            title = { Text(t("Deadline time", "Jam deadline")) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    val day = pickedDay
                    if (day != null) {
                        val date = LocalDate.ofEpochDay(day)
                        setDeadline(date.atTime(timeState.hour, timeState.minute).atZone(zone).toInstant().toEpochMilli())
                    }
                    showTime = false
                }) { Text(t("Set", "Atur")) }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text(t("Cancel", "Batal")) } },
        )
    }

    if (customReminder) {
        var amount by rememberSaveable { mutableStateOf("") }
        var unit by rememberSaveable { mutableIntStateOf(1) }
        val factor = when (unit) {
            0 -> 1
            1 -> 60
            else -> 24 * 60
        }
        val minutes = amount.toIntOrNull()?.let { it * factor }
        val valid = minutes != null && minutes in 1..ReminderPresets.MAX_MINUTES
        AlertDialog(
            onDismissRequest = { customReminder = false },
            title = { Text(t("Custom reminder", "Pengingat custom")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(
                        amount,
                        { amount = it.take(4) },
                        t("How long before the deadline", "Berapa lama sebelum deadline"),
                        Modifier.fillMaxWidth(),
                        decimal = false,
                        imeAction = ImeAction.Done,
                    )
                    Segmented(
                        listOf(0, 1, 2),
                        unit,
                        {
                            when (it) {
                                0 -> t("Minutes", "Menit")
                                1 -> t("Hours", "Jam")
                                else -> t("Days", "Hari")
                            }
                        },
                        { unit = it },
                    )
                    if (minutes != null && valid) {
                        Text(t("Remind me ", "Ingatkan ") + t.durationWords(minutes) + t(" before the deadline.", " sebelum deadline."))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (minutes != null && valid) form = form.copy(reminders = (form.reminders + minutes).distinct())
                        customReminder = false
                    },
                    enabled = valid,
                ) { Text(t("Add", "Tambah")) }
            },
            dismissButton = { TextButton(onClick = { customReminder = false }) { Text(t("Cancel", "Batal")) } },
        )
    }

    if (askDiscard) {
        ConfirmDialog(
            title = t("Discard changes?", "Buang perubahan?"),
            text = t("Your changes to this task have not been saved.", "Perubahan tugas ini belum disimpan."),
            confirmLabel = t("Discard", "Buang"),
            destructive = true,
            onDismiss = { askDiscard = false },
            onConfirm = onBack,
        )
    }

    if (askDelete && existing != null) {
        ConfirmDialog(
            title = t("Delete task?", "Hapus tugas?"),
            text = t("\"${existing.title}\" will be deleted.", "\"${existing.title}\" akan dihapus."),
            confirmLabel = t("Delete", "Hapus"),
            destructive = true,
            onDismiss = { askDelete = false },
            onConfirm = {
                onBack()
                val removed = vm.delete(existing.id)
                if (removed != null) messenger.show(t("Task deleted", "Tugas dihapus"), t("Undo", "Urungkan")) { vm.restore(removed) }
            },
        )
    }
}
