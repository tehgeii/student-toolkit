@file:OptIn(ExperimentalMaterial3Api::class)

package com.tehgeii.studenttoolkit.ui.academic

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tehgeii.studenttoolkit.core.Fmt
import com.tehgeii.studenttoolkit.core.newId
import com.tehgeii.studenttoolkit.data.GradeDraftRow
import com.tehgeii.studenttoolkit.data.GradeTemplate
import com.tehgeii.studenttoolkit.domain.academic.GradeCalculator
import com.tehgeii.studenttoolkit.domain.academic.GradeScale
import com.tehgeii.studenttoolkit.domain.academic.GradeSummary
import com.tehgeii.studenttoolkit.domain.academic.TargetResult
import com.tehgeii.studenttoolkit.domain.academic.WeightState
import com.tehgeii.studenttoolkit.i18n.Lang
import com.tehgeii.studenttoolkit.ui.LocalLang
import com.tehgeii.studenttoolkit.ui.LocalMessenger
import com.tehgeii.studenttoolkit.ui.Navigator
import com.tehgeii.studenttoolkit.ui.Routes
import com.tehgeii.studenttoolkit.ui.components.AppScaffold
import com.tehgeii.studenttoolkit.ui.components.ChoiceChips
import com.tehgeii.studenttoolkit.ui.components.ConfirmDialog
import com.tehgeii.studenttoolkit.ui.components.EmptyState
import com.tehgeii.studenttoolkit.ui.components.FormColumn
import com.tehgeii.studenttoolkit.ui.components.InfoCard
import com.tehgeii.studenttoolkit.ui.components.ListColumn
import com.tehgeii.studenttoolkit.ui.components.NumberField
import com.tehgeii.studenttoolkit.ui.components.ResultCard
import com.tehgeii.studenttoolkit.ui.components.SectionTitle
import com.tehgeii.studenttoolkit.ui.components.TextInput
import com.tehgeii.studenttoolkit.ui.components.readable
import com.tehgeii.studenttoolkit.ui.theme.StatusColors
import kotlin.math.roundToInt

/** A course the user already entered in a semester, offered when starting a calculation. */
private data class CourseOption(val name: String, val code: String)

private fun courseOptions(semesters: List<com.tehgeii.studenttoolkit.domain.academic.Semester>): List<CourseOption> =
    semesters.asReversed()
        .flatMap { s -> s.courses }
        .filter { it.name.isNotBlank() }
        .map { CourseOption(it.name.trim(), it.code.trim()) }
        .distinctBy { it.name.lowercase() }

/** Saved grade calculations, one per course (or as many as the user likes). */
@Composable
fun GradeListScreen(nav: Navigator, onBack: () -> Unit) {
    val t = LocalLang.current
    val vm = academicViewModel()
    val data by vm.data.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val options = courseOptions(data.semesters)
    var choosing by rememberSaveable { mutableStateOf(false) }
    val start: () -> Unit = { if (options.isEmpty()) nav.navigate(Routes.grade(vm.createCalculation("", t.indonesian))) else choosing = true }
    val calculations = data.calculations.sortedByDescending { it.updatedAt }

    AppScaffold(
        title = t("Grade calculator", "Kalkulator nilai"),
        onBack = onBack,
        floatingActionButton = {
            if (calculations.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = start,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(t("New", "Baru")) },
                )
            }
        },
    ) { padding ->
        ListColumn(padding) {
            if (calculations.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "📊",
                        title = t("No calculations yet", "Belum ada perhitungan"),
                        message = t(
                            "Make one calculation per course: final score, the score you still need, and scenarios. Everything is saved.",
                            "Buat satu perhitungan per mata kuliah: nilai akhir, nilai yang masih dibutuhkan, dan simulasi. Semua tersimpan otomatis.",
                        ),
                        actionLabel = t("New calculation", "Perhitungan baru"),
                        onAction = start,
                        modifier = Modifier.readable(),
                    )
                }
            }
            items(calculations, key = { it.id }) { calc ->
                val summary = GradeCalculator.summarize(calc.rows.map { it.toComponent() })
                val final = summary.finalScore
                val band = final?.let { settings.gradeScale.bandFor(it) }
                val decimals = settings.scoreDecimals
                Card(onClick = { nav.navigate(Routes.grade(calc.id)) }, modifier = Modifier.readable().animateItem()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                calc.course.ifBlank { t("Untitled calculation", "Perhitungan tanpa nama") },
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                when {
                                    final != null -> t("Final score", "Nilai akhir") + " " + Fmt.number(final, decimals)
                                    summary.gradedWeight > 0 -> t(
                                        "So far ${Fmt.number(summary.currentPoints, decimals)} · ${Fmt.number(summary.remainingWeight)}% to go",
                                        "Sementara ${Fmt.number(summary.currentPoints, decimals)} · sisa ${Fmt.number(summary.remainingWeight)}%",
                                    )
                                    else -> t("No scores yet", "Belum ada skor")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(band?.letter ?: "—", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (choosing) {
        CoursePickerDialog(
            options = options,
            onDismiss = { choosing = false },
            onPick = { course ->
                choosing = false
                nav.navigate(Routes.grade(vm.createCalculation(course, t.indonesian)))
            },
        )
    }
}

@Composable
private fun CoursePickerDialog(options: List<CourseOption>, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val t = LocalLang.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t("Which course?", "Mata kuliah apa?")) },
        text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                options.forEach { option ->
                    ListItem(
                        headlineContent = { Text(option.name) },
                        supportingContent = if (option.code.isBlank()) null else { { Text(option.code) } },
                        modifier = Modifier.clickable { onPick(option.name) },
                    )
                }
                ListItem(
                    headlineContent = { Text(t("Other / type it later", "Lainnya / ketik nanti")) },
                    modifier = Modifier.clickable { onPick("") },
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(t("Cancel", "Batal")) } },
    )
}

/**
 * Grade calculator, score target planner and scenario simulator on a single screen:
 * enter components → see the final score, or what is still needed for a target grade.
 * Every change is saved, so leaving and coming back keeps the inputs.
 */
@Composable
fun GradeCalculatorScreen(id: String, nav: Navigator, onBack: () -> Unit) {
    val t = LocalLang.current
    val vm = academicViewModel()
    val activity = LocalActivity.current
    // A calculation opened by accident and left empty is removed again (not on rotation).
    DisposableEffect(id) {
        onDispose { if (activity?.isChangingConfigurations != true) vm.removeCalculationIfUntouched(id) }
    }
    val messenger = LocalMessenger.current
    val data by vm.data.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val calc = data.calculations.firstOrNull { it.id == id }
    var askReset by rememberSaveable { mutableStateOf(false) }
    var askDelete by rememberSaveable { mutableStateOf(false) }

    AppScaffold(
        title = calc?.course?.ifBlank { null } ?: t("Grade calculator", "Kalkulator nilai"),
        onBack = onBack,
        actions = {
            if (calc != null) {
                IconButton(onClick = { askReset = true }) { Icon(Icons.Filled.Refresh, contentDescription = t("Start over", "Mulai ulang")) }
                IconButton(onClick = { askDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = t("Delete calculation", "Hapus perhitungan")) }
            }
        },
    ) { padding ->
        if (calc == null) {
            FormColumn(padding) {
                EmptyState("🤔", t("Calculation not found", "Perhitungan tidak ditemukan"), t("It may have been deleted.", "Mungkin sudah dihapus."))
            }
            return@AppScaffold
        }
        val scale = settings.gradeScale
        val summary = GradeCalculator.summarize(calc.rows.map { it.toComponent() })
        FormColumn(padding) {
            var course by rememberSaveable(id) { mutableStateOf(calc.course) }
            TextInput(course, { course = it; vm.updateCalculation(id) { c -> c.copy(course = it) } }, t("Course", "Mata kuliah"))
            val suggestions = courseOptions(data.semesters).filter { it.name != course.trim() && it.name.contains(course.trim(), ignoreCase = true) }.take(8)
            if (suggestions.isNotEmpty() && course.length < 40) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    suggestions.forEach { option ->
                        SuggestionChip(
                            onClick = {
                                course = option.name
                                vm.updateCalculation(id) { c -> c.copy(course = option.name) }
                            },
                            label = { Text(option.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        )
                    }
                }
            }

            SectionTitle(t("Components", "Komponen nilai"))
            calc.rows.forEachIndexed { index, row ->
                key(row.id) {
                    ComponentRow(
                        row = row,
                        invalid = index in summary.invalidRows,
                        onChange = { updated -> vm.updateCalculation(id) { c -> c.copy(rows = c.rows.map { if (it.id == updated.id) updated else it }) } },
                        onDelete = { vm.updateCalculation(id) { c -> c.copy(rows = c.rows.filterNot { it.id == row.id }) } },
                    )
                }
            }
            OutlinedButton(
                onClick = { vm.updateCalculation(id) { c -> c.copy(rows = c.rows + GradeDraftRow(newId(), "", "", "")) } },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(t("Add component", "Tambah komponen"))
            }
            WeightStatusLine(summary, t)
            if (summary.invalidRows.isNotEmpty()) {
                InfoCard(t("Fix the highlighted fields: weight must be 1–100 % and score 0–100.", "Perbaiki kolom yang ditandai: bobot 1–100 % dan skor 0–100."), emoji = "⚠️")
            }
            if (summary.canCalculate) {
                Results(
                    summary = summary,
                    scale = scale,
                    decimals = settings.scoreDecimals,
                    targetLetter = calc.targetLetter,
                    onTarget = { letter -> vm.updateCalculation(id) { c -> c.copy(targetLetter = letter) } },
                    nav = nav,
                    t = t,
                )
            }
            Text(
                t(
                    "Final = Σ(score × weight) ÷ 100. Leave a score empty if it is not graded yet.",
                    "Nilai akhir = Σ(skor × bobot) ÷ 100. Kosongkan skor yang belum keluar.",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (askReset && calc != null) {
        ConfirmDialog(
            title = t("Start over?", "Mulai ulang?"),
            text = t(
                "Components go back to Attendance 10%, Assignment 20%, Midterm 30% and Final 40% with empty scores. The course name stays.",
                "Komponen kembali ke Kehadiran 10%, Tugas 20%, UTS 30%, dan UAS 40% dengan skor kosong. Nama mata kuliah tetap.",
            ),
            confirmLabel = t("Start over", "Mulai ulang"),
            onDismiss = { askReset = false },
            onConfirm = { vm.updateCalculation(id) { it.copy(rows = GradeTemplate.rows(t.indonesian), targetLetter = "") } },
        )
    }
    if (askDelete && calc != null) {
        ConfirmDialog(
            title = t("Delete calculation?", "Hapus perhitungan?"),
            text = t("Its components and scores will be removed.", "Komponen dan skornya akan dihapus."),
            confirmLabel = t("Delete", "Hapus"),
            destructive = true,
            onDismiss = { askDelete = false },
            onConfirm = {
                onBack()
                val removed = vm.deleteCalculation(id)
                if (removed != null) messenger.show(t("Calculation deleted", "Perhitungan dihapus"), t("Undo", "Urungkan")) { vm.restoreCalculation(removed) }
            },
        )
    }
}

@Composable
private fun ComponentRow(row: GradeDraftRow, invalid: Boolean, onChange: (GradeDraftRow) -> Unit, onDelete: () -> Unit) {
    val t = LocalLang.current
    var name by rememberSaveable { mutableStateOf(row.name) }
    var weight by rememberSaveable { mutableStateOf(row.weight) }
    var score by rememberSaveable { mutableStateOf(row.score) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextInput(
                    name,
                    {
                        name = it
                        onChange(row.copy(name = it, weight = weight, score = score))
                    },
                    t("Component", "Komponen"),
                    modifier = Modifier.weight(1f),
                    isError = invalid,
                )
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Close, contentDescription = t("Remove component", "Hapus komponen")) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(
                    weight,
                    {
                        weight = it
                        onChange(row.copy(name = name, weight = it, score = score))
                    },
                    label = t("Weight", "Bobot"),
                    suffix = "%",
                    isError = invalid,
                    modifier = Modifier.weight(1f),
                )
                NumberField(
                    score,
                    {
                        score = it
                        onChange(row.copy(name = name, weight = weight, score = it))
                    },
                    label = t("Score", "Skor"),
                    isError = invalid,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun WeightStatusLine(summary: GradeSummary, t: Lang) {
    val total = Fmt.number(summary.totalWeight)
    val (text, color) = when (summary.weightState) {
        WeightState.VALID -> t("✓ Valid — total weight is 100%", "✓ Valid — total bobot 100%") to StatusColors.ok
        WeightState.UNDER ->
            t("⚠ Total weight is $total%. Missing weight counts as 0.", "⚠ Total bobot $total%. Bobot yang kurang dihitung 0.") to StatusColors.warning
        WeightState.OVER ->
            t("✕ Total weight is $total%. Reduce it to 100% to calculate.", "✕ Total bobot $total%. Kurangi jadi 100% untuk menghitung.") to StatusColors.danger
        WeightState.EMPTY -> t("Add components with their weight.", "Tambahkan komponen beserta bobotnya.") to StatusColors.neutral
    }
    Text(text, color = color, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
}

@Composable
private fun Results(
    summary: GradeSummary,
    scale: GradeScale,
    decimals: Int,
    targetLetter: String,
    onTarget: (String) -> Unit,
    nav: Navigator,
    t: Lang,
) {
    val final = summary.finalScore
    if (final != null) {
        val band = scale.bandFor(final)
        ResultCard {
            Text(t("Final score", "Nilai akhir"), style = MaterialTheme.typography.labelLarge)
            Text(Fmt.number(final, decimals), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            if (band != null) {
                Text(
                    t("Grade", "Nilai") + " ${band.letter} (${scale.rangeLabel(band)}) · ${Fmt.number(band.points)} " + t("points", "poin"),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        OutlinedButton(onClick = { nav.navigate(Routes.GRADE_SCALE) }, modifier = Modifier.fillMaxWidth()) {
            Text(t("Different grading scale? Edit it", "Skala nilai kampusmu beda? Ubah di sini"))
        }
        return
    }

    ResultCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
        Text(t("Score so far", "Nilai sementara"), style = MaterialTheme.typography.labelLarge)
        Text(Fmt.number(summary.currentPoints, decimals), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            t(
                "From ${Fmt.number(summary.gradedWeight)}% graded · ${Fmt.number(summary.remainingWeight)}% still to come",
                "Dari ${Fmt.number(summary.gradedWeight)}% yang sudah dinilai · sisa ${Fmt.number(summary.remainingWeight)}%",
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
    }

    SectionTitle(t("Target grade", "Target nilai"))
    val target = scale.band(targetLetter) ?: scale.bands.first()
    ChoiceChips(
        options = scale.bands.filter { it.minScore > 0 },
        selected = target,
        label = { "${it.letter} ≥ ${Fmt.number(it.minScore)}" },
        onSelect = { onTarget(it.letter) },
    )
    when (val result = GradeCalculator.requiredScore(summary, target.minScore)) {
        is TargetResult.Needed -> ResultCard {
            Text(t("Required average on the remaining ${Fmt.number(summary.remainingWeight)}%", "Rata-rata yang dibutuhkan di sisa ${Fmt.number(summary.remainingWeight)}%"), style = MaterialTheme.typography.labelLarge)
            Text(Fmt.number(result.score, decimals), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text(t("out of 100 to get ${target.letter}", "dari 100 untuk dapat ${target.letter}"), style = MaterialTheme.typography.bodyMedium)
        }
        is TargetResult.Impossible -> ResultCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
            Text(t("Current score requires", "Dengan nilai sekarang, dibutuhkan"), style = MaterialTheme.typography.labelLarge)
            Text("${Fmt.number(result.score, decimals)} / 100", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(t("Target ${target.letter} cannot be reached. Try a lower target.", "Target ${target.letter} tidak bisa dicapai. Coba target yang lebih rendah."), style = MaterialTheme.typography.bodyMedium)
        }
        TargetResult.AlreadySecured -> ResultCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
            Text(t("✓ ${target.letter} is already secured", "✓ ${target.letter} sudah aman"), style = MaterialTheme.typography.titleMedium)
            Text(t("Even a 0 on the remaining components keeps you at ${target.letter}.", "Walau sisa komponen dapat 0, nilaimu tetap ${target.letter}."), style = MaterialTheme.typography.bodyMedium)
        }
        TargetResult.NothingRemaining -> Unit
    }

    SectionTitle(t("Scenario simulator", "Simulasi skenario"))
    var scenario by rememberSaveable { mutableFloatStateOf(80f) }
    val scenarioScore = scenario.roundToInt().toDouble()
    val scenarioFinal = GradeCalculator.scenario(summary, scenarioScore)
    val scenarioBand = scale.bandFor(scenarioFinal)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                t("If the remaining components score ${scenarioScore.toInt()}", "Jika sisa komponen dapat ${scenarioScore.toInt()}"),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                t("Final score", "Nilai akhir") + " ${Fmt.number(scenarioFinal, decimals)}" + (scenarioBand?.let { " · ${it.letter}" } ?: ""),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Slider(value = scenario, onValueChange = { scenario = it }, valueRange = 0f..100f)
            listOf(60, 70, 80, 90, 100).forEach { s ->
                val f = GradeCalculator.scenario(summary, s.toDouble())
                Text(
                    t("Remaining = $s → final ${Fmt.number(f, decimals)}", "Sisa = $s → akhir ${Fmt.number(f, decimals)}") +
                        (scale.bandFor(f)?.let { " (${it.letter})" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
