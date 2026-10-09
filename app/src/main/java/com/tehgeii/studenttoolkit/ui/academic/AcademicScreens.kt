@file:OptIn(ExperimentalMaterial3Api::class)

package com.tehgeii.studenttoolkit.ui.academic

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tehgeii.studenttoolkit.core.Fmt
import com.tehgeii.studenttoolkit.core.newId
import com.tehgeii.studenttoolkit.core.parseNumber
import com.tehgeii.studenttoolkit.data.AcademicRepository
import com.tehgeii.studenttoolkit.data.GradeDraft
import com.tehgeii.studenttoolkit.data.SettingsRepository
import com.tehgeii.studenttoolkit.domain.academic.Course
import com.tehgeii.studenttoolkit.domain.academic.GpaCalculator
import com.tehgeii.studenttoolkit.domain.academic.GpaPlanner
import com.tehgeii.studenttoolkit.domain.academic.GpaTarget
import com.tehgeii.studenttoolkit.domain.academic.GradeBand
import com.tehgeii.studenttoolkit.domain.academic.GradeScale
import com.tehgeii.studenttoolkit.domain.academic.Semester
import com.tehgeii.studenttoolkit.ui.LocalContainer
import com.tehgeii.studenttoolkit.ui.LocalMessenger
import com.tehgeii.studenttoolkit.ui.Navigator
import com.tehgeii.studenttoolkit.ui.Routes
import com.tehgeii.studenttoolkit.ui.components.AppScaffold
import com.tehgeii.studenttoolkit.ui.components.ConfirmDialog
import com.tehgeii.studenttoolkit.ui.components.DropdownField
import com.tehgeii.studenttoolkit.ui.components.EmptyState
import com.tehgeii.studenttoolkit.ui.components.FormColumn
import com.tehgeii.studenttoolkit.ui.components.InfoCard
import com.tehgeii.studenttoolkit.ui.components.ListColumn
import com.tehgeii.studenttoolkit.ui.components.NumberField
import com.tehgeii.studenttoolkit.ui.components.ResultCard
import com.tehgeii.studenttoolkit.ui.components.SectionTitle
import com.tehgeii.studenttoolkit.ui.components.Segmented
import com.tehgeii.studenttoolkit.ui.components.TextInput
import com.tehgeii.studenttoolkit.ui.components.readable
import java.io.Serializable

class AcademicViewModel(private val repo: AcademicRepository, private val settingsRepo: SettingsRepository) : ViewModel() {
    val data = repo.data
    val settings = settingsRepo.state

    fun createSemester(): String = repo.createSemester().id
    fun updateSemester(id: String, transform: (Semester) -> Semester) = repo.updateSemester(id, transform)
    fun deleteSemester(id: String): Semester? = repo.deleteSemester(id)
    fun restoreSemester(semester: Semester) = repo.restoreSemester(semester)
    fun updateDraft(transform: (GradeDraft) -> GradeDraft) = repo.updateDraft(transform)
    fun updateScale(scale: GradeScale) = settingsRepo.update { it.copy(gradeScale = scale) }
}

@Composable
fun academicViewModel(): AcademicViewModel {
    val container = LocalContainer.current
    return viewModel { AcademicViewModel(container.academic, container.settings) }
}

// ---- Academic tab ------------------------------------------------------------------------------

@Composable
fun AcademicScreen(nav: Navigator) {
    val vm = academicViewModel()
    val data by vm.data.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val scale = settings.gradeScale
    val cumulative = GpaCalculator.cumulative(data.semesters, scale)

    AppScaffold(title = "Academic") { padding ->
        ListColumn(padding) {
            item {
                ResultCard(Modifier.readable()) {
                    Text("Cumulative GPA (IPK)", style = MaterialTheme.typography.labelLarge)
                    Text(
                        cumulative.gpa?.let { Fmt.fixed(it, settings.gpaDecimals) } ?: "—",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (cumulative.totalCredits == 0) "Add a semester to calculate your GPA."
                        else "${cumulative.totalCredits} SKS · ${data.semesters.size} semester(s) · weighted by SKS",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            item { ToolRow("📊", "Grade calculator", "Final score, target score & scenarios") { nav.navigate(Routes.GRADE) } }
            item { ToolRow("🎯", "GPA target planner", "What you need to reach a target IPK") { nav.navigate(Routes.GPA_GOAL) } }
            item { ToolRow("🔁", "Grade converter", "Score → letter using your scale") { nav.navigate(Routes.GRADE_CONVERTER) } }
            item {
                Row(Modifier.readable(), verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Semesters (IPS)", Modifier.weight(1f))
                    TextButton(onClick = { nav.navigate(Routes.semester(vm.createSemester())) }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text("Add")
                    }
                }
            }
            if (data.semesters.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "📚",
                        title = "No semesters yet",
                        message = "Add a semester and its courses to calculate your IPS and IPK.",
                        actionLabel = "Add semester",
                        onAction = { nav.navigate(Routes.semester(vm.createSemester())) },
                        modifier = Modifier.readable(),
                    )
                }
            }
            items(data.semesters, key = { it.id }) { semester ->
                val result = GpaCalculator.semester(semester, scale)
                Card(onClick = { nav.navigate(Routes.semester(semester.id)) }, modifier = Modifier.readable()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(semester.name.ifBlank { "Untitled semester" }, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (result.gpa == null) "Not calculated yet" else "${result.totalCredits} SKS",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            result.gpa?.let { Fmt.fixed(it, settings.gpaDecimals) } ?: "—",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ToolRow(emoji: String, title: String, subtitle: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.readable()) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(emoji, style = MaterialTheme.typography.titleLarge)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

// ---- Semester (IPS calculator) ----------------------------------------------------------------

@Composable
fun SemesterScreen(id: String, onBack: () -> Unit) {
    val vm = academicViewModel()
    val messenger = LocalMessenger.current
    val data by vm.data.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val semester = data.semesters.firstOrNull { it.id == id }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    AppScaffold(
        title = semester?.name?.ifBlank { "Semester" } ?: "Semester",
        onBack = onBack,
        actions = {
            if (semester != null) {
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = "Delete semester") }
            }
        },
    ) { padding ->
        if (semester == null) {
            FormColumn(padding) {
                EmptyState("🤔", "Semester not found", "It may have been deleted.")
            }
            return@AppScaffold
        }
        val scale = settings.gradeScale
        val result = GpaCalculator.semester(semester, scale)
        FormColumn(padding) {
            var name by rememberSaveable(id) { mutableStateOf(semester.name) }
            TextInput(name, { name = it; vm.updateSemester(id) { s -> s.copy(name = it) } }, "Semester name")
            Segmented(
                options = listOf(false, true),
                selected = semester.useManual,
                label = { if (it) "Enter IPS directly" else "Per course" },
                onSelect = { manual -> vm.updateSemester(id) { it.copy(useManual = manual) } },
            )
            if (semester.useManual) {
                var gpaText by rememberSaveable(id) { mutableStateOf(semester.manualGpa?.let { Fmt.number(it, 3) } ?: "") }
                var creditsText by rememberSaveable(id) { mutableStateOf(semester.manualCredits?.toString() ?: "") }
                val gpaValue = parseNumber(gpaText)
                val gpaInvalid = gpaValue != null && (gpaValue < 0 || gpaValue > scale.maxPoints)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(
                        gpaText,
                        {
                            gpaText = it
                            val v = parseNumber(it)
                            vm.updateSemester(id) { s -> s.copy(manualGpa = v?.takeIf { g -> g >= 0 && g <= scale.maxPoints }) }
                        },
                        label = "IPS",
                        isError = gpaInvalid,
                        supportingText = if (gpaInvalid) "0 – ${Fmt.number(scale.maxPoints)}" else null,
                        modifier = Modifier.weight(1f),
                    )
                    NumberField(
                        creditsText,
                        {
                            creditsText = it
                            vm.updateSemester(id) { s -> s.copy(manualCredits = it.toIntOrNull()) }
                        },
                        label = "Total SKS",
                        decimal = false,
                        modifier = Modifier.weight(1f),
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                    )
                }
            } else {
                SectionTitle("Courses")
                if (semester.courses.isEmpty()) {
                    Text(
                        "No courses added. Add a course to calculate your IPS.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                semester.courses.forEach { course ->
                    key(course.id) {
                        CourseRow(
                            course = course,
                            letters = scale.letters,
                            onChange = { updated -> vm.updateSemester(id) { s -> s.copy(courses = s.courses.map { if (it.id == updated.id) updated else it }) } },
                            onDelete = { vm.updateSemester(id) { s -> s.copy(courses = s.courses.filterNot { it.id == course.id }) } },
                        )
                    }
                }
                OutlinedButton(
                    onClick = { vm.updateSemester(id) { s -> s.copy(courses = s.courses + Course(newId(), "", 0, "")) } },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Add course")
                }
            }
            ResultCard {
                Text("IPS (semester GPA)", style = MaterialTheme.typography.labelLarge)
                Text(
                    result.gpa?.let { Fmt.fixed(it, settings.gpaDecimals) } ?: "—",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                Text("Total SKS: ${result.totalCredits}", style = MaterialTheme.typography.bodyMedium)
                if (!semester.useManual) {
                    val incomplete = semester.courses.count { it.credits <= 0 || scale.pointsFor(it.grade) == null }
                    if (incomplete > 0) {
                        Text(
                            "$incomplete course(s) without SKS or grade are not counted.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            Text(
                "IPS = Σ(grade point × SKS) ÷ Σ SKS. Grade points come from your grading scale in Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (confirmDelete && semester != null) {
        ConfirmDialog(
            title = "Delete semester?",
            text = "\"${semester.name}\" and its courses will be removed.",
            confirmLabel = "Delete",
            destructive = true,
            onDismiss = { confirmDelete = false },
            onConfirm = {
                onBack()
                val removed = vm.deleteSemester(id)
                if (removed != null) messenger.show("Semester deleted", "Undo") { vm.restoreSemester(removed) }
            },
        )
    }
}

@Composable
private fun CourseRow(course: Course, letters: List<String>, onChange: (Course) -> Unit, onDelete: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(course.name) }
    var credits by rememberSaveable { mutableStateOf(if (course.credits > 0) course.credits.toString() else "") }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextInput(
                    name,
                    {
                        name = it
                        onChange(course.copy(name = it))
                    },
                    "Course name",
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Close, contentDescription = "Remove course") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                NumberField(
                    credits,
                    {
                        credits = it
                        onChange(course.copy(credits = it.toIntOrNull()?.coerceIn(0, 99) ?: 0))
                    },
                    label = "SKS",
                    decimal = false,
                    modifier = Modifier.weight(1f),
                )
                DropdownField(
                    label = "Grade",
                    options = letters,
                    selected = course.grade.takeIf { it in letters },
                    optionLabel = { it },
                    onSelect = { onChange(course.copy(grade = it)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// ---- GPA target planner -----------------------------------------------------------------------

@Composable
fun GpaGoalScreen(onBack: () -> Unit) {
    val vm = academicViewModel()
    val data by vm.data.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val scale = settings.gradeScale
    val history = GpaCalculator.cumulative(data.semesters, scale)

    var current by rememberSaveable { mutableStateOf("") }
    var completed by rememberSaveable { mutableStateOf("") }
    var remaining by rememberSaveable { mutableStateOf("") }
    var target by rememberSaveable { mutableStateOf("") }

    AppScaffold(title = "GPA target planner", onBack = onBack) { padding ->
        FormColumn(padding) {
            if (history.gpa != null) {
                OutlinedButton(
                    onClick = {
                        current = Fmt.number(history.gpa!!, 4)
                        completed = history.totalCredits.toString()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Use my IPK (${Fmt.fixed(history.gpa!!, settings.gpaDecimals)}, ${history.totalCredits} SKS)") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField(current, { current = it }, "Current GPA", Modifier.weight(1f))
                NumberField(completed, { completed = it }, "Completed SKS", Modifier.weight(1f), decimal = false)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField(target, { target = it }, "Target GPA", Modifier.weight(1f))
                NumberField(
                    remaining, { remaining = it }, "Remaining SKS", Modifier.weight(1f),
                    decimal = false, imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                )
            }
            val c = parseNumber(current)
            val done = completed.toIntOrNull()
            val left = remaining.toIntOrNull()
            val t = parseNumber(target)
            if (c != null && done != null && left != null && t != null) {
                when (val result = GpaPlanner.requiredGpa(c, done, left, t, scale.maxPoints)) {
                    is GpaTarget.Needed -> ResultCard {
                        Text("Average needed on the remaining $left SKS", style = MaterialTheme.typography.labelLarge)
                        Text(Fmt.fixed(result.gpa, settings.gpaDecimals), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                        Text(describePoints(result.gpa, scale), style = MaterialTheme.typography.bodyMedium)
                    }
                    is GpaTarget.Impossible -> ResultCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                        Text("Target cannot be reached", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (result.gpa.isInfinite()) "There are no credits left to improve your GPA."
                            else "It would need an average of ${Fmt.fixed(result.gpa, settings.gpaDecimals)} (max ${Fmt.number(scale.maxPoints)}) under the current assumptions.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    GpaTarget.AlreadySecured -> ResultCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                        Text("✓ Target already secured", style = MaterialTheme.typography.titleMedium)
                        Text("You stay at or above the target whatever grades you get.", style = MaterialTheme.typography.bodyMedium)
                    }
                    is GpaTarget.Invalid -> InfoCard(result.reason, emoji = "⚠️")
                }
            } else {
                InfoCard("Fill in all four fields to see the average grade point you need.")
            }
            Text(
                "Needed = (target × total SKS − current GPA × completed SKS) ÷ remaining SKS",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun describePoints(points: Double, scale: GradeScale): String {
    val exact = scale.bands.firstOrNull { kotlin.math.abs(it.points - points) < 1e-9 }
    if (exact != null) return "That is an average of ${exact.letter}."
    val above = scale.bands.filter { it.points > points }.minByOrNull { it.points }
    val below = scale.bands.filter { it.points < points }.maxByOrNull { it.points }
    return when {
        above != null && below != null -> "Somewhere between ${below.letter} and ${above.letter} on average."
        above != null -> "At least ${above.letter} on average."
        else -> "Any grade keeps you on target."
    }
}

// ---- Grade converter --------------------------------------------------------------------------

@Composable
fun GradeConverterScreen(nav: Navigator, onBack: () -> Unit) {
    val vm = academicViewModel()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val scale = settings.gradeScale
    var score by rememberSaveable { mutableStateOf("") }
    val value = parseNumber(score)
    val band = value?.takeIf { it in 0.0..100.0 }?.let { scale.bandFor(it) }

    AppScaffold(title = "Grade converter", onBack = onBack) { padding ->
        FormColumn(padding) {
            NumberField(
                score, { score = it }, "Score (0–100)", Modifier.fillMaxWidth(),
                isError = value != null && value !in 0.0..100.0,
                supportingText = if (value != null && value !in 0.0..100.0) "Scores go from 0 to 100" else null,
                imeAction = androidx.compose.ui.text.input.ImeAction.Done,
            )
            if (band != null) {
                ResultCard {
                    Text("Grade", style = MaterialTheme.typography.labelLarge)
                    Text(band.letter, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                    Text("Range ${scale.rangeLabel(band)} · ${Fmt.number(band.points)} points", style = MaterialTheme.typography.bodyMedium)
                }
            }
            SectionTitle("Your grading scale")
            ScaleTable(scale, highlight = band)
            OutlinedButton(onClick = { nav.navigate(Routes.GRADE_SCALE) }, modifier = Modifier.fillMaxWidth()) {
                Text("Edit grading scale")
            }
        }
    }
}

@Composable
fun ScaleTable(scale: GradeScale, highlight: GradeBand? = null) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 8.dp)) {
            scale.bands.forEach { band ->
                val selected = band == highlight
                Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Text(
                        band.letter,
                        modifier = Modifier.weight(1f),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(scale.rangeLabel(band), modifier = Modifier.weight(1f))
                    Text(Fmt.fixed(band.points, 2), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ---- Grade scale editor -----------------------------------------------------------------------

private data class ScaleRow(val id: String, val letter: String, val min: String, val points: String) : Serializable

private fun GradeScale.toRows(): List<ScaleRow> =
    bands.map { ScaleRow(newId(), it.letter, Fmt.number(it.minScore), Fmt.number(it.points)) }

@Composable
fun GradeScaleScreen(onBack: () -> Unit) {
    val vm = academicViewModel()
    val messenger = LocalMessenger.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    var rows by rememberSaveable { mutableStateOf(settings.gradeScale.toRows()) }
    var askDiscard by rememberSaveable { mutableStateOf(false) }
    var askReset by rememberSaveable { mutableStateOf(false) }

    val parsed = rows.map { GradeBand(it.letter.trim(), parseNumber(it.min) ?: -1.0, parseNumber(it.points) ?: -1.0) }
    val candidate = GradeScale(parsed)
    val problems = candidate.validate()
    val dirty = candidate != settings.gradeScale
    val leave: () -> Unit = {
        if (dirty) {
            askDiscard = true
        } else {
            onBack()
        }
    }

    BackHandler(enabled = dirty) { askDiscard = true }

    AppScaffold(
        title = "Grading scale",
        onBack = leave,
        actions = {
            IconButton(onClick = { askReset = true }) { Icon(Icons.Filled.Refresh, contentDescription = "Reset to default") }
        },
    ) { padding ->
        FormColumn(padding) {
            InfoCard("Scores at or above the minimum get that grade. Grade points are used for IPS/IPK. Every campus is different, so adjust freely.")
            rows.forEachIndexed { index, row ->
                key(row.id) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextInput(
                            row.letter,
                            { v -> rows = rows.toMutableList().also { it[index] = row.copy(letter = v.take(4)) } },
                            "Grade",
                            modifier = Modifier.weight(1f),
                            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Characters,
                        )
                        NumberField(
                            row.min, { v -> rows = rows.toMutableList().also { it[index] = row.copy(min = v) } },
                            "Min score", Modifier.weight(1f),
                        )
                        NumberField(
                            row.points, { v -> rows = rows.toMutableList().also { it[index] = row.copy(points = v) } },
                            "Points", Modifier.weight(1f),
                        )
                        IconButton(onClick = { rows = rows.filterNot { it.id == row.id } }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove grade")
                        }
                    }
                }
            }
            OutlinedButton(onClick = { rows = rows + ScaleRow(newId(), "", "", "") }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Add grade")
            }
            problems.forEach { InfoCard(it, emoji = "⚠️", container = MaterialTheme.colorScheme.errorContainer) }
            Button(
                onClick = {
                    vm.updateScale(candidate)
                    messenger.show("✓ Grading scale saved")
                    onBack()
                },
                enabled = problems.isEmpty() && dirty,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save scale") }
        }
    }

    if (askDiscard) {
        ConfirmDialog(
            title = "Discard changes?",
            text = "Your edits to the grading scale have not been saved.",
            confirmLabel = "Discard",
            destructive = true,
            onDismiss = { askDiscard = false },
            onConfirm = onBack,
        )
    }
    if (askReset) {
        ConfirmDialog(
            title = "Reset to default?",
            text = "A/AB/B/BC/C/D/E with 85/80/75/70/60/50/0 and 4.0 to 0.0 points.",
            confirmLabel = "Reset",
            onDismiss = { askReset = false },
            onConfirm = { rows = GradeScale.DEFAULT.toRows() },
        )
    }
}
