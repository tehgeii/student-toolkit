@file:OptIn(ExperimentalMaterial3Api::class)

package com.tehgeii.studenttoolkit.ui.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.tehgeii.studenttoolkit.core.Fmt
import com.tehgeii.studenttoolkit.core.parseNumber
import com.tehgeii.studenttoolkit.domain.tools.PercentMode
import com.tehgeii.studenttoolkit.domain.tools.Percentage
import com.tehgeii.studenttoolkit.domain.tools.UnitCategory
import com.tehgeii.studenttoolkit.domain.tools.UnitConverter
import com.tehgeii.studenttoolkit.ui.LocalMessenger
import com.tehgeii.studenttoolkit.ui.Platform
import com.tehgeii.studenttoolkit.ui.components.AppScaffold
import com.tehgeii.studenttoolkit.ui.components.ChoiceChips
import com.tehgeii.studenttoolkit.ui.components.DropdownField
import com.tehgeii.studenttoolkit.ui.components.FormColumn
import com.tehgeii.studenttoolkit.ui.components.InfoCard
import com.tehgeii.studenttoolkit.ui.components.NumberField
import com.tehgeii.studenttoolkit.ui.components.ResultCard
import java.util.Locale
import kotlin.math.abs

/** Readable number for conversions: plain for everyday magnitudes, scientific for extremes. */
private fun smart(value: Double): String {
    if (value == 0.0) return "0"
    val a = abs(value)
    return when {
        a >= 1e12 || a < 1e-6 -> String.format(Locale.US, "%.6e", value)
        a >= 1000 -> Fmt.number(value, 2)
        else -> Fmt.number(value, 6)
    }
}

// ---- Unit converter ---------------------------------------------------------------------------

@Composable
fun UnitConverterScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val messenger = LocalMessenger.current
    var category by rememberSaveable { mutableStateOf(UnitCategory.LENGTH) }
    var fromIndex by rememberSaveable { mutableIntStateOf(category.units.indexOfFirst { it.factor == 1.0 }.coerceAtLeast(0)) }
    var input by rememberSaveable { mutableStateOf("1") }
    val from = category.units[fromIndex.coerceIn(0, category.units.lastIndex)]
    val value = parseNumber(input)

    AppScaffold(title = "Unit converter", onBack = onBack) { padding ->
        FormColumn(padding) {
            ChoiceChips(
                UnitCategory.entries.toList(),
                category,
                { it.label },
                {
                    category = it
                    fromIndex = it.units.indexOfFirst { u -> u.factor == 1.0 }.coerceAtLeast(0)
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                NumberField(input, { input = it }, "Value", Modifier.weight(1f), imeAction = ImeAction.Done)
                DropdownField(
                    "From",
                    category.units,
                    from,
                    { "${it.symbol} — ${it.name}" },
                    { fromIndex = category.units.indexOf(it) },
                    Modifier.weight(1f),
                )
            }
            when {
                value == null -> InfoCard("Enter a number to convert.")
                !UnitConverter.isValid(value, category, from) -> InfoCard("That is below absolute zero.", emoji = "⚠️")
                else -> Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        category.units.filter { it != from }.forEachIndexed { index, unit ->
                            if (index > 0) HorizontalDivider()
                            val converted = smart(UnitConverter.convert(value, category, from, unit))
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        Platform.copy(context, unit.name, converted)
                                        messenger.show("✓ Copied $converted ${unit.symbol}")
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(unit.symbol, style = MaterialTheme.typography.titleSmall)
                                    Text(unit.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(converted, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
            if (category == UnitCategory.STORAGE) {
                InfoCard("SI units use 1000 (1 GB = 1000 MB). IEC units use 1024 (1 GiB = 1024 MiB). Operating systems often show GiB while labelling it GB.")
            }
            Text(
                "Tap a result to copy it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---- Percentage -------------------------------------------------------------------------------

@Composable
fun PercentageScreen(onBack: () -> Unit) {
    var mode by rememberSaveable { mutableStateOf(PercentMode.PERCENT_OF) }
    var first by rememberSaveable { mutableStateOf("") }
    var second by rememberSaveable { mutableStateOf("") }
    val a = parseNumber(first)
    val b = parseNumber(second)

    AppScaffold(title = "Percentage", onBack = onBack) { padding ->
        FormColumn(padding) {
            ChoiceChips(PercentMode.entries.toList(), mode, { it.label }, { mode = it })
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField(first, { first = it }, mode.firstLabel, Modifier.weight(1f))
                NumberField(second, { second = it }, mode.secondLabel, Modifier.weight(1f), imeAction = ImeAction.Done)
            }
            if (a == null || b == null) {
                InfoCard("Fill in both values.")
            } else {
                PercentResult(mode, a, b)
            }
        }
    }
}

@Composable
private fun PercentResult(mode: PercentMode, a: Double, b: Double) {
    val n = { v: Double -> Fmt.number(v, 4) }
    when (mode) {
        PercentMode.PERCENT_OF -> ResultLines(n(Percentage.percentOf(a, b)), "${n(a)}% of ${n(b)} = ${n(b)} × ${n(a)} ÷ 100")
        PercentMode.WHAT_PERCENT -> {
            val r = Percentage.whatPercent(a, b)
            if (r == null) InfoCard("The whole (Y) cannot be 0.", emoji = "⚠️")
            else ResultLines("${n(r)}%", "${n(a)} is ${n(r)}% of ${n(b)} = ${n(a)} ÷ ${n(b)} × 100")
        }
        PercentMode.CHANGE -> {
            val r = Percentage.change(a, b)
            if (r == null) {
                InfoCard("A change from 0 has no percentage.", emoji = "⚠️")
            } else {
                val word = if (r > 0) "increase" else if (r < 0) "decrease" else "no change"
                ResultLines(
                    (if (r > 0) "+" else "") + "${n(r)}%",
                    "${n(a)} → ${n(b)}: $word of ${n(abs(b - a))} (${n(abs(r))}%)",
                )
            }
        }
        PercentMode.DISCOUNT -> {
            val (final, saved) = Percentage.discount(a, b)
            ResultLines(n(final), "You pay ${n(final)} and save ${n(saved)} (${n(b)}% of ${n(a)})")
        }
        PercentMode.INCREASE -> ResultLines(n(Percentage.increase(a, b)), "${n(a)} + ${n(b)}% = ${n(a)} × (1 + ${n(b)} ÷ 100)")
        PercentMode.DECREASE -> ResultLines(n(Percentage.decrease(a, b)), "${n(a)} − ${n(b)}% = ${n(a)} × (1 − ${n(b)} ÷ 100)")
    }
}

@Composable
private fun ResultLines(value: String, explanation: String) {
    ResultCard {
        Text("Result", style = MaterialTheme.typography.labelLarge)
        Text(value, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text(explanation, style = MaterialTheme.typography.bodyMedium)
    }
}
