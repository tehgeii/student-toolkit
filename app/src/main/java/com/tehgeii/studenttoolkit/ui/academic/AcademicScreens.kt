@file:OptIn(ExperimentalMaterial3Api::class)

package com.tehgeii.studenttoolkit.ui.academic

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tehgeii.studenttoolkit.core.Fmt
import com.tehgeii.studenttoolkit.core.newId
import com.tehgeii.studenttoolkit.core.parseNumber
import com.tehgeii.studenttoolkit.data.AcademicRepository
import com.tehgeii.studenttoolkit.data.GradeCalc
import com.tehgeii.studenttoolkit.data.SettingsRepository
import com.tehgeii.studenttoolkit.domain.academic.Course
import com.tehgeii.studenttoolkit.domain.academic.GpaCalculator
import com.tehgeii.studenttoolkit.domain.academic.GpaPlanner
import com.tehgeii.studenttoolkit.domain.academic.GpaTarget
import com.tehgeii.studenttoolkit.domain.academic.GradeBand
import com.tehgeii.studenttoolkit.domain.academic.GradeScale
import com.tehgeii.studenttoolkit.domain.academic.Semester
import com.tehgeii.studenttoolkit.i18n.Lang
import com.tehgeii.studenttoolkit.i18n.gpaInvalid
import com.tehgeii.studenttoolkit.i18n.scaleProblem
import com.tehgeii.studenttoolkit.ui.LocalContainer
import com.tehgeii.studenttoolkit.ui.LocalLang
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
import com.tehgeii.studenttoolkit.ui.components.decorative
import com.tehgeii.studenttoolkit.ui.components.readable
import java.io.Serializable

class AcademicViewModel(private val repo: AcademicRepository, private val settingsRepo: SettingsRepository) : ViewModel() {
    val data = repo.data
    val settings = settingsRepo.state

    fun createSemester(): String = repo.createSemester().id
    fun updateSemester(id: String, transform: (Semester) -> Semester) = repo.updateSemester(id, transform)
    fun deleteSemester(id: String): Semester? = repo.deleteSemester(id)
    fun restoreSemester(semester: Semester) = repo.restoreSemester(semester)
    fun removeIfUntouched(id: String) = repo.removeIfUntouched(id)
    fun createCalculation(course: String, indonesian: Boolean): String = repo.createCalculation(course, indonesian).id
    fun updateCalculation(id: String, transform: (GradeCalc) -> GradeCalc) = repo.updateCalculation(id, transform)
    fun deleteCalculation(id: String): GradeCalc? = repo.deleteCalculation(id)
    fun restoreCalculation(calc: GradeCalc) = repo.restoreCalculation(calc)
    fun removeCalculationIfUntouched(id: String) = repo.removeCalculationIfUntouched(id)
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
    val t = LocalLang.current
    val vm = academicViewModel()
    val data by vm.data.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val scale = settings.gradeScale
    val cumulative = GpaCalculator.cumulative(data.semesters, scale)

    AppScaffold(title = t("Academic", "Akademik")) { padding ->
        ListColumn(padding) {
            item {
                ResultCard(Modifier.readable()) {
                    Text(t("Cumulative GPA (IPK)", "Indeks Prestasi Kumulatif (IPK)"), style = MaterialTheme.typography.labelLarge)
                    Text(
                        cumulative.gpa?.let { Fmt.fixed(it, settings.gpaDecimals) } ?: "—",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (cumulative.totalCredits == 0) {
                            t("Add a semester to calculate your GPA.", "Tambahkan semester untuk menghitung IPK.")
                        } else {
                            "${cumulative.totalCredits} SKS · ${data.semesters.size} semester · " + t("weighted by SKS", "dibobot SKS")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            item {
                ToolRow("📊", t("Grade calculator", "Kalkulator nilai"), t("Final score, target score & scenarios", "Nilai akhir, target nilai & simulasi")) {
                    nav.navigate(Routes.GRADE)
                }
            }
            item {
                ToolRow("🎯", t("GPA target planner", "Target IPK"), t("What you need to reach a target IPK", "Nilai yang dibutuhkan untuk mencapai target IPK")) {
                    nav.navigate(Routes.GPA_GOAL)
                }
            }
            item {
                ToolRow("🔁", t("Grade converter", "Konversi nilai"), t("Score → letter using your scale", "Angka → huruf sesuai skala nilaimu")) {
                    nav.navigate(Routes.GRADE_CONVERTER)
                }
            }
            item {
                Row(Modifier.readable(), verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle(t("Semesters (IPS)", "Semester (IPS)"), Modifier.weight(1f))
                    TextButton(onClick = { nav.navigate(Routes.semester(vm.createSemester())) }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(t("Add", "Tambah"))
                    }
                }
            }
            if (data.semesters.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "📚",
                        title = t("No semesters yet", "Belum ada semester"),
                        message = t("Add a semester and its courses to calculate your IPS and IPK.", "Tambahkan semester dan mata kuliahnya untuk menghitung IPS dan IPK."),
                        actionLabel = t("Add semester", "Tambah semester"),
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
                            Text(semester.name.ifBlank { t("Untitled semester", "Semester tanpa nama") }, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (result.gpa == null) t("Not calculated yet", "Belum dihitung") else "${result.totalCredits} SKS",
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
            Text(emoji, style = MaterialTheme.typography.titleLarge, modifier = Modifier.decorative())
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
    val t = LocalLang.current
    val vm = academicViewModel()
    val activity = LocalActivity.current
    // A semester that was added but never filled in is removed again when the screen is left
    // (not on rotation), so tapping "Add" by accident leaves no empty entries behind.
    DisposableEffect(id) {
        onDispose { if (activity?.isChangingConfigurations != true) vm.removeIfUntouched(id) }
    }
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
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = t("Delete semester", "Hapus semester")) }
            }
        },
    ) { padding ->
        if (semester == null) {
            FormColumn(padding) {
                EmptyState("🤔", t("Semester not found", "Semester tidak ditemukan"), t("It may have been deleted.", "Mungkin sudah dihapus."))
            }
            return@AppScaffold
        }
        val scale = settings.gradeScale
        val result = GpaCalculator.semester(semester, scale)
        FormColumn(padding) {
            var name by rememberSaveable(id) { mutableStateOf(semester.name) }
            TextInput(name, { name = it; vm.updateSemester(id) { s -> s.copy(name = it) } }, t("Semester name", "Nama semester"))
            Segmented(
                options = listOf(false, true),
                selected = semester.useManual,
                label = { if (it) t("Enter IPS directly", "Isi IPS langsung") else t("Per course", "Per mata kuliah") },
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
                SectionTitle(t("Courses", "Mata kuliah"))
                if (semester.courses.isEmpty()) {
                    Text(
                        t("No courses added. Add a course to calculate your IPS.", "Belum ada mata kuliah. Tambahkan untuk menghitung IPS."),
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
                    Text(t("Add course", "Tambah mata kuliah"))
                }
            }
            ResultCard {
                Text(t("IPS (semester GPA)", "IPS (indeks prestasi semester)"), style = MaterialTheme.typography.labelLarge)
                Text(
                    result.gpa?.let { Fmt.fixed(it, settings.gpaDecimals) } ?: "—",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(t("Total SKS: ", "Total SKS: ") + result.totalCredits, style = MaterialTheme.typography.bodyMedium)
                if (!semester.useManual) {
                    val incomplete = semester.courses.count { it.credits <= 0 || scale.pointsFor(it.grade) == null }
                    if (incomplete > 0) {
                        Text(
                            t("$incomplete course(s) without SKS or grade are not counted.", "$incomplete mata kuliah tanpa SKS atau nilai tidak dihitung."),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            Text(
                t(
                    "IPS = Σ(grade point × SKS) ÷ Σ SKS. Grade points come from your grading scale in Settings.",
                    "IPS = Σ(bobot nilai × SKS) ÷ Σ SKS. Bobot nilai diambil dari skala nilai di Pengaturan.",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (confirmDelete && semester != null) {
        ConfirmDialog(
            title = t("Delete semester?", "Hapus semester?"),
            text = t("\"${semester.name}\" and its courses will be removed.", "\"${semester.name}\" beserta mata kuliahnya akan dihapus."),
            confirmLabel = t("Delete", "Hapus"),
            destructive = true,
            onDismiss = { confirmDelete = false },
            onConfirm = {
                onBack()
                val removed = vm.deleteSemester(id)
                if (removed != null) messenger.show(t("Semester deleted", "Semester dihapus"), t("Undo", "Urungkan")) { vm.restoreSemester(removed) }
            },
        )
    }
}

@Composable
private fun CourseRow(course: Course, letters: List<String>, onChange: (Course) -> Unit, onDelete: () -> Unit) {
    val t = LocalLang.current
    var name by rememberSaveable { mutableStateOf(course.name) }
    var credits by rememberSaveable { mutableStateOf(if (course.credits > 0) course.credits.toString() else "") }
    var code by rememberSaveable { mutableStateOf(course.code) }
    var lecturer by rememberSaveable { mutableStateOf(course.lecturer) }
    var showDetails by rememberSaveable { mutableStateOf(course.code.isNotBlank() || course.lecturer.isNotBlank()) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextInput(
                    name,
                    {
                        name = it
                        onChange(course.copy(name = it))
                    },
                    t("Course name", "Nama mata kuliah"),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Close, contentDescription = t("Remove course", "Hapus mata kuliah")) }
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
                    label = t("Grade", "Nilai"),
                    options = letters,
                    selected = course.grade.takeIf { it in letters },
                    optionLabel = { it },
                    onSelect = { onChange(course.copy(grade = it)) },
                    modifier = Modifier.weight(1f),
                )
            }
            if (showDetails) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextInput(
                        code,
                        {
                            code = it.take(20)
                            onChange(course.copy(code = code))
                        },
                        t("Code", "Kode"),
                        modifier = Modifier.weight(1f),
                        capitalization = KeyboardCapitalization.Characters,
                    )
                    TextInput(
                        lecturer,
                        {
                            lecturer = it.take(80)
                            onChange(course.copy(lecturer = lecturer))
                        },
                        t("Lecturer", "Dosen"),
                        modifier = Modifier.weight(1.6f),
                        capitalization = KeyboardCapitalization.Words,
                    )
                }
            } else {
                TextButton(onClick = { showDetails = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(t("Course code & lecturer", "Kode mata kuliah & dosen"))
                }
            }
        }
    }
}

// ---- GPA target planner -----------------------------------------------------------------------

@Composable
fun GpaGoalScreen(onBack: () -> Unit) {
    val t = LocalLang.current
    val vm = academicViewModel()
    val data by vm.data.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val scale = settings.gradeScale
    val history = GpaCalculator.cumulative(data.semesters, scale)

    var current by rememberSaveable { mutableStateOf("") }
    var completed by rememberSaveable { mutableStateOf("") }
    var remaining by rememberSaveable { mutableStateOf("") }
    var target by rememberSaveable { mutableStateOf("") }

    AppScaffold(title = t("GPA target planner", "Target IPK"), onBack = onBack) { padding ->
        FormColumn(padding) {
            if (history.gpa != null) {
                OutlinedButton(
                    onClick = {
                        current = Fmt.number(history.gpa!!, 4)
                        completed = history.totalCredits.toString()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(t("Use my IPK", "Pakai IPK-ku") + " (${Fmt.fixed(history.gpa!!, settings.gpaDecimals)}, ${history.totalCredits} SKS)") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField(current, { current = it }, t("Current GPA", "IPK saat ini"), Modifier.weight(1f))
                NumberField(completed, { completed = it }, t("Completed SKS", "SKS ditempuh"), Modifier.weight(1f), decimal = false)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField(target, { target = it }, t("Target GPA", "Target IPK"), Modifier.weight(1f))
                NumberField(
                    remaining, { remaining = it }, t("Remaining SKS", "Sisa SKS"), Modifier.weight(1f),
                    decimal = false, imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                )
            }
            val c = parseNumber(current)
            val done = completed.toIntOrNull()
            val left = remaining.toIntOrNull()
            val targetGpa = parseNumber(target)
            if (c != null && done != null && left != null && targetGpa != null) {
                when (val result = GpaPlanner.requiredGpa(c, done, left, targetGpa, scale.maxPoints)) {
                    is GpaTarget.Needed -> ResultCard {
                        Text(t("Average needed on the remaining $left SKS", "Rata-rata yang dibutuhkan di sisa $left SKS"), style = MaterialTheme.typography.labelLarge)
                        Text(Fmt.fixed(result.gpa, settings.gpaDecimals), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                        Text(describePoints(result.gpa, scale, t), style = MaterialTheme.typography.bodyMedium)
                    }
                    is GpaTarget.Impossible -> ResultCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                        Text(t("Target cannot be reached", "Target tidak bisa dicapai"), style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (result.gpa.isInfinite()) {
                                t("There are no credits left to improve your GPA.", "Tidak ada sisa SKS untuk menaikkan IPK.")
                            } else {
                                t(
                                    "It would need an average of ${Fmt.fixed(result.gpa, settings.gpaDecimals)} (max ${Fmt.number(scale.maxPoints)}) under the current assumptions.",
                                    "Butuh rata-rata ${Fmt.fixed(result.gpa, settings.gpaDecimals)} (maksimal ${Fmt.number(scale.maxPoints)}) dengan asumsi saat ini.",
                                )
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    GpaTarget.AlreadySecured -> ResultCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(t("✓ Target already secured", "✓ Target sudah aman"), style = MaterialTheme.typography.titleMedium)
                        Text(t("You stay at or above the target whatever grades you get.", "Berapa pun nilaimu nanti, IPK tetap di atas target."), style = MaterialTheme.typography.bodyMedium)
                    }
                    is GpaTarget.Invalid -> InfoCard(t.gpaInvalid(result.reason, scale.maxPoints), emoji = "⚠️")
                }
            } else {
                InfoCard(t("Fill in all four fields to see the average grade point you need.", "Isi keempat kolom untuk melihat rata-rata nilai yang dibutuhkan."))
            }
            Text(
                t(
                    "Needed = (target × total SKS − current GPA × completed SKS) ÷ remaining SKS",
                    "Dibutuhkan = (target × total SKS − IPK saat ini × SKS ditempuh) ÷ sisa SKS",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun describePoints(points: Double, scale: GradeScale, t: Lang): String {
    val exact = scale.bands.firstOrNull { kotlin.math.abs(it.points - points) < 1e-9 }
    if (exact != null) return t("That is an average of ${exact.letter}.", "Artinya rata-rata ${exact.letter}.")
    val above = scale.bands.filter { it.points > points }.minByOrNull { it.points }
    val below = scale.bands.filter { it.points < points }.maxByOrNull { it.points }
    return when {
        above != null && below != null -> t("Somewhere between ${below.letter} and ${above.letter} on average.", "Rata-rata antara ${below.letter} dan ${above.letter}.")
        above != null -> t("At least ${above.letter} on average.", "Rata-rata minimal ${above.letter}.")
        else -> t("Any grade keeps you on target.", "Nilai berapa pun tetap sesuai target.")
    }
}

// ---- Grade converter --------------------------------------------------------------------------

@Composable
fun GradeConverterScreen(nav: Navigator, onBack: () -> Unit) {
    val t = LocalLang.current
    val vm = academicViewModel()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val scale = settings.gradeScale
    var score by rememberSaveable { mutableStateOf("") }
    val value = parseNumber(score)
    val band = value?.takeIf { it in 0.0..100.0 }?.let { scale.bandFor(it) }

    AppScaffold(title = t("Grade converter", "Konversi nilai"), onBack = onBack) { padding ->
        FormColumn(padding) {
            NumberField(
                score, { score = it }, t("Score (0–100)", "Skor (0–100)"), Modifier.fillMaxWidth(),
                isError = value != null && value !in 0.0..100.0,
                supportingText = if (value != null && value !in 0.0..100.0) t("Scores go from 0 to 100", "Skor dari 0 sampai 100") else null,
                imeAction = androidx.compose.ui.text.input.ImeAction.Done,
            )
            if (band != null) {
                ResultCard {
                    Text(t("Grade", "Nilai huruf"), style = MaterialTheme.typography.labelLarge)
                    Text(band.letter, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                    Text(t("Range", "Rentang") + " ${scale.rangeLabel(band)} · ${Fmt.number(band.points)} " + t("points", "poin"), style = MaterialTheme.typography.bodyMedium)
                }
            }
            SectionTitle(t("Your grading scale", "Skala nilaimu"))
            ScaleTable(scale, highlight = band)
            OutlinedButton(onClick = { nav.navigate(Routes.GRADE_SCALE) }, modifier = Modifier.fillMaxWidth()) {
                Text(t("Edit grading scale", "Ubah skala nilai"))
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
    val t = LocalLang.current
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
        title = t("Grading scale", "Skala nilai"),
        onBack = leave,
        actions = {
            IconButton(onClick = { askReset = true }) { Icon(Icons.Filled.Refresh, contentDescription = t("Reset to default", "Kembalikan ke bawaan")) }
        },
    ) { padding ->
        FormColumn(padding) {
            InfoCard(
                t(
                    "Scores at or above the minimum get that grade. Grade points are used for IPS/IPK. Every campus is different, so adjust freely.",
                    "Skor di atas atau sama dengan minimum mendapat nilai itu. Bobot dipakai untuk IPS/IPK. Tiap kampus berbeda, jadi sesuaikan saja.",
                ),
            )
            rows.forEachIndexed { index, row ->
                key(row.id) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextInput(
                            row.letter,
                            { v -> rows = rows.toMutableList().also { it[index] = row.copy(letter = v.take(4)) } },
                            t("Grade", "Huruf"),
                            modifier = Modifier.weight(1f),
                            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Characters,
                        )
                        NumberField(
                            row.min, { v -> rows = rows.toMutableList().also { it[index] = row.copy(min = v) } },
                            t("Min score", "Skor min"), Modifier.weight(1f),
                        )
                        NumberField(
                            row.points, { v -> rows = rows.toMutableList().also { it[index] = row.copy(points = v) } },
                            t("Points", "Bobot"), Modifier.weight(1f),
                        )
                        IconButton(onClick = { rows = rows.filterNot { it.id == row.id } }) {
                            Icon(Icons.Filled.Close, contentDescription = t("Remove grade", "Hapus baris"))
                        }
                    }
                }
            }
            OutlinedButton(onClick = { rows = rows + ScaleRow(newId(), "", "", "") }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(t("Add grade", "Tambah nilai"))
            }
            problems.forEach { InfoCard(t.scaleProblem(it), emoji = "⚠️", container = MaterialTheme.colorScheme.errorContainer) }
            Button(
                onClick = {
                    vm.updateScale(candidate)
                    messenger.show(t("✓ Grading scale saved", "✓ Skala nilai disimpan"))
                    onBack()
                },
                enabled = problems.isEmpty() && dirty,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(t("Save scale", "Simpan skala")) }
        }
    }

    if (askDiscard) {
        ConfirmDialog(
            title = t("Discard changes?", "Buang perubahan?"),
            text = t("Your edits to the grading scale have not been saved.", "Perubahan skala nilai belum disimpan."),
            confirmLabel = t("Discard", "Buang"),
            destructive = true,
            onDismiss = { askDiscard = false },
            onConfirm = onBack,
        )
    }
    if (askReset) {
        ConfirmDialog(
            title = t("Reset to default?", "Kembalikan ke bawaan?"),
            text = t("A/AB/B/BC/C/D/E with 85/80/75/70/60/50/0 and 4.0 to 0.0 points.", "A/AB/B/BC/C/D/E dengan skor 85/80/75/70/60/50/0 dan bobot 4,0 sampai 0,0."),
            confirmLabel = t("Reset", "Kembalikan"),
            onDismiss = { askReset = false },
            onConfirm = { rows = GradeScale.DEFAULT.toRows() },
        )
    }
}
