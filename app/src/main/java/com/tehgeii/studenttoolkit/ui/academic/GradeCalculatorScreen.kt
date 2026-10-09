@file:OptIn(ExperimentalMaterial3Api::class)

package com.tehgeii.studenttoolkit.ui.academic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tehgeii.studenttoolkit.core.Fmt
import com.tehgeii.studenttoolkit.core.newId
import com.tehgeii.studenttoolkit.data.GradeDraft
import com.tehgeii.studenttoolkit.data.GradeDraftRow
import com.tehgeii.studenttoolkit.domain.academic.GradeCalculator
import com.tehgeii.studenttoolkit.domain.academic.GradeScale
import com.tehgeii.studenttoolkit.domain.academic.GradeSummary
import com.tehgeii.studenttoolkit.domain.academic.TargetResult
import com.tehgeii.studenttoolkit.domain.academic.WeightState
import com.tehgeii.studenttoolkit.ui.Navigator
import com.tehgeii.studenttoolkit.ui.Routes
import com.tehgeii.studenttoolkit.ui.components.AppScaffold
import com.tehgeii.studenttoolkit.ui.components.ChoiceChips
import com.tehgeii.studenttoolkit.ui.components.ConfirmDialog
import com.tehgeii.studenttoolkit.ui.components.FormColumn
import com.tehgeii.studenttoolkit.ui.components.InfoCard
import com.tehgeii.studenttoolkit.ui.components.NumberField
import com.tehgeii.studenttoolkit.ui.components.ResultCard
import com.tehgeii.studenttoolkit.ui.components.SectionTitle
import com.tehgeii.studenttoolkit.ui.components.TextInput
import com.tehgeii.studenttoolkit.ui.theme.StatusColors
import kotlin.math.roundToInt

/**
 * Grade calculator, score target planner and scenario simulator on a single screen:
 * enter components → see the final score, or what is still needed for a target grade.
 * The draft is saved automatically, so leaving and coming back keeps the inputs.
 */
@Composable
fun GradeCalculatorScreen(nav: Navigator, onBack: () -> Unit) {
    val vm = academicViewModel()
    val data by vm.data.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val draft = data.draft
    val scale = settings.gradeScale
    val summary = GradeCalculator.summarize(draft.rows.map { it.toComponent() })
    var askReset by rememberSaveable { mutableStateOf(false) }

    AppScaffold(
        title = "Grade calculator",
        onBack = onBack,
        actions = {
            IconButton(onClick = { askReset = true }) { Icon(Icons.Filled.Refresh, contentDescription = "Start over") }
        },
    ) { padding ->
        FormColumn(padding) {
            var course by rememberSaveable { mutableStateOf(draft.course) }
            TextInput(course, { course = it; vm.updateDraft { d -> d.copy(course = it) } }, "Course (optional)")

            SectionTitle("Components")
            draft.rows.forEachIndexed { index, row ->
                key(row.id) {
                    ComponentRow(
                        row = row,
                        invalid = index in summary.invalidRows,
                        onChange = { updated -> vm.updateDraft { d -> d.copy(rows = d.rows.map { if (it.id == updated.id) updated else it }) } },
                        onDelete = { vm.updateDraft { d -> d.copy(rows = d.rows.filterNot { it.id == row.id }) } },
                    )
                }
            }
            OutlinedButton(
                onClick = { vm.updateDraft { d -> d.copy(rows = d.rows + GradeDraftRow(newId(), "", "", "")) } },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Add component")
            }
            WeightStatusLine(summary)
            if (summary.invalidRows.isNotEmpty()) {
                InfoCard("Fix the highlighted fields: weight must be 1–100 % and score 0–100.", emoji = "⚠️")
            }
            if (summary.canCalculate) Results(summary, scale, nav)
            Text(
                "Final = Σ(score × weight) ÷ 100. Leave a score empty if it is not graded yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (askReset) {
        ConfirmDialog(
            title = "Start over?",
            text = "Components go back to Attendance 10%, Assignment 20%, Midterm 30% and Final 40% with empty scores.",
            confirmLabel = "Start over",
            onDismiss = { askReset = false },
            onConfirm = { vm.updateDraft { GradeDraft() } },
        )
    }
}

@Composable
private fun ComponentRow(row: GradeDraftRow, invalid: Boolean, onChange: (GradeDraftRow) -> Unit, onDelete: () -> Unit) {
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
                    "Component",
                    modifier = Modifier.weight(1f),
                    isError = invalid,
                )
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Close, contentDescription = "Remove component") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(
                    weight,
                    {
                        weight = it
                        onChange(row.copy(name = name, weight = it, score = score))
                    },
                    label = "Weight",
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
                    label = "Score",
                    isError = invalid,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun WeightStatusLine(summary: GradeSummary) {
    val total = Fmt.number(summary.totalWeight)
    val (text, color) = when (summary.weightState) {
        WeightState.VALID -> "✓ Valid — total weight is 100%" to StatusColors.ok
        WeightState.UNDER -> "⚠ Total weight is $total%. Missing weight counts as 0." to StatusColors.warning
        WeightState.OVER -> "✕ Total weight is $total%. Reduce it to 100% to calculate." to StatusColors.danger
        WeightState.EMPTY -> "Add components with their weight." to StatusColors.neutral
    }
    Text(text, color = color, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
}

@Composable
private fun Results(summary: GradeSummary, scale: GradeScale, nav: Navigator) {
    val final = summary.finalScore
    if (final != null) {
        val band = scale.bandFor(final)
        ResultCard {
            Text("Final score", style = MaterialTheme.typography.labelLarge)
            Text(Fmt.number(final, 2), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            if (band != null) {
                Text(
                    "Grade ${band.letter} (${scale.rangeLabel(band)}) · ${Fmt.number(band.points)} points",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        OutlinedButton(onClick = { nav.navigate(Routes.GRADE_SCALE) }, modifier = Modifier.fillMaxWidth()) {
            Text("Different grading scale? Edit it")
        }
        return
    }

    ResultCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
        Text("Score so far", style = MaterialTheme.typography.labelLarge)
        Text(Fmt.number(summary.currentPoints, 2), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "From ${Fmt.number(summary.gradedWeight)}% graded · ${Fmt.number(summary.remainingWeight)}% still to come",
            style = MaterialTheme.typography.bodyMedium,
        )
    }

    SectionTitle("Target grade")
    var targetLetter by rememberSaveable { mutableStateOf(scale.bands.first().letter) }
    val target = scale.band(targetLetter) ?: scale.bands.first()
    ChoiceChips(
        options = scale.bands.filter { it.minScore > 0 },
        selected = target,
        label = { "${it.letter} ≥ ${Fmt.number(it.minScore)}" },
        onSelect = { targetLetter = it.letter },
    )
    when (val result = GradeCalculator.requiredScore(summary, target.minScore)) {
        is TargetResult.Needed -> ResultCard {
            Text("Required average on the remaining ${Fmt.number(summary.remainingWeight)}%", style = MaterialTheme.typography.labelLarge)
            Text(Fmt.number(result.score, 2), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text("out of 100 to get ${target.letter}", style = MaterialTheme.typography.bodyMedium)
        }
        is TargetResult.Impossible -> ResultCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
            Text("Current score requires", style = MaterialTheme.typography.labelLarge)
            Text("${Fmt.number(result.score, 2)} / 100", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Target ${target.letter} cannot be reached. Try a lower target.", style = MaterialTheme.typography.bodyMedium)
        }
        TargetResult.AlreadySecured -> ResultCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
            Text("✓ ${target.letter} is already secured", style = MaterialTheme.typography.titleMedium)
            Text("Even a 0 on the remaining components keeps you at ${target.letter}.", style = MaterialTheme.typography.bodyMedium)
        }
        TargetResult.NothingRemaining -> Unit
    }

    SectionTitle("Scenario simulator")
    var scenario by rememberSaveable { mutableFloatStateOf(80f) }
    val scenarioScore = scenario.roundToInt().toDouble()
    val scenarioFinal = GradeCalculator.scenario(summary, scenarioScore)
    val scenarioBand = scale.bandFor(scenarioFinal)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "If the remaining components score ${scenarioScore.toInt()}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Final score ${Fmt.number(scenarioFinal, 2)}" + (scenarioBand?.let { " · ${it.letter}" } ?: ""),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Slider(value = scenario, onValueChange = { scenario = it }, valueRange = 0f..100f)
            listOf(60, 70, 80, 90, 100).forEach { s ->
                val f = GradeCalculator.scenario(summary, s.toDouble())
                Text(
                    "Remaining = $s → final ${Fmt.number(f, 2)}" + (scale.bandFor(f)?.let { " (${it.letter})" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
