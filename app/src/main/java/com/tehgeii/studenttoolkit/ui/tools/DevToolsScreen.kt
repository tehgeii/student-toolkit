@file:OptIn(ExperimentalMaterial3Api::class)

package com.tehgeii.studenttoolkit.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.tehgeii.studenttoolkit.domain.tools.JsonFormat
import com.tehgeii.studenttoolkit.domain.tools.JsonResult
import com.tehgeii.studenttoolkit.domain.tools.NumberBase
import com.tehgeii.studenttoolkit.domain.tools.RegexResult
import com.tehgeii.studenttoolkit.domain.tools.RegexTester
import com.tehgeii.studenttoolkit.domain.tools.Rgb
import com.tehgeii.studenttoolkit.domain.tools.Timestamps
import com.tehgeii.studenttoolkit.i18n.Lang
import com.tehgeii.studenttoolkit.ui.LocalLang
import com.tehgeii.studenttoolkit.ui.LocalMessenger
import com.tehgeii.studenttoolkit.ui.Platform
import com.tehgeii.studenttoolkit.ui.components.AppIcons
import com.tehgeii.studenttoolkit.ui.components.AppScaffold
import com.tehgeii.studenttoolkit.ui.components.ChoiceChips
import com.tehgeii.studenttoolkit.ui.components.FormColumn
import com.tehgeii.studenttoolkit.ui.components.InfoCard
import com.tehgeii.studenttoolkit.ui.components.TextInput
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val tabs = listOf(
    "Number base" to "Basis bilangan",
    "Timestamp" to "Timestamp",
    "JSON" to "JSON",
    "Color" to "Warna",
    "Regex" to "Regex",
)

/** Most text a result box renders; Copy always takes the full result. */
private const val SHOWN_CHARS = 20_000

/** Small offline helpers for programming classes: number bases, timestamps, JSON, colors and regex. */
@Composable
fun DevToolsScreen(initialTab: Int, onBack: () -> Unit) {
    val t = LocalLang.current
    var tab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, tabs.lastIndex)) }
    AppScaffold(title = t("Developer tools", "Alat developer"), onBack = onBack) { padding ->
        FormColumn(padding) {
            ChoiceChips(tabs.indices.toList(), tab, { t(tabs[it].first, tabs[it].second) }, { tab = it })
            when (tab) {
                0 -> NumberBaseTool(t)
                1 -> TimestampTool(t)
                2 -> JsonTool(t)
                3 -> ColorTool(t)
                else -> RegexTool(t)
            }
        }
    }
}

@Composable
private fun CopyRow(label: String, value: String, t: Lang) {
    val context = LocalContext.current
    val messenger = LocalMessenger.current
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SelectionContainer {
                    Text(value.take(SHOWN_CHARS), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyLarge)
                }
            }
            IconButton(onClick = {
                Platform.copy(context, label, value)
                messenger.show(t("✓ Copied", "✓ Disalin"))
            }) { Icon(AppIcons.Copy, contentDescription = t("Copy $label", "Salin $label")) }
        }
    }
}

// ---- Number base -----------------------------------------------------------------------------

private val radixes = listOf(2 to "BIN", 8 to "OCT", 10 to "DEC", 16 to "HEX")

@Composable
private fun NumberBaseTool(t: Lang) {
    var input by rememberSaveable { mutableStateOf("") }
    var radix by rememberSaveable { mutableIntStateOf(10) }
    ChoiceChips(radixes.map { it.first }, radix, { r -> radixes.first { it.first == r }.second }, { radix = it })
    TextInput(
        input, { input = it.take(NumberBase.MAX_DIGITS + 4) },
        t("Number in ", "Bilangan dalam ") + radixes.first { it.first == radix }.second,
        capitalization = KeyboardCapitalization.Characters,
        keyboardType = if (radix == 10) KeyboardType.Number else KeyboardType.Ascii,
    )
    if (input.isBlank()) return
    val value = NumberBase.parse(input, radix)
    if (value == null) {
        InfoCard(t("Not a valid number in this base.", "Bukan bilangan yang valid untuk basis ini."), emoji = "⚠️")
        return
    }
    radixes.forEach { (r, name) -> CopyRow("$name ($r)", NumberBase.format(value, r), t) }
}

// ---- Timestamp -------------------------------------------------------------------------------

@Composable
private fun TimestampTool(t: Lang) {
    var input by rememberSaveable { mutableStateOf("") }
    val zone = ZoneId.systemDefault()
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        TextInput(
            input, { input = it.take(40) }, t("Unix time or date", "Waktu Unix atau tanggal"),
            modifier = Modifier.weight(1f),
            placeholder = "1791000000 / 2026-10-16 14:30",
        )
        OutlinedButton(onClick = { input = (System.currentTimeMillis() / 1000).toString() }) { Text(t("Now", "Sekarang")) }
    }
    if (input.isBlank()) {
        Text(
            t(
                "Enter seconds or milliseconds since 1970, or a date like 2026-10-16 14:30 (your time zone: $zone).",
                "Masukkan detik atau milidetik sejak 1970, atau tanggal seperti 2026-10-16 14:30 (zona waktumu: $zone).",
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val instant: Instant? = Timestamps.parseEpoch(input) ?: Timestamps.parseLocal(input, zone)
    if (instant == null) {
        InfoCard(t("Use digits (Unix time) or yyyy-MM-dd HH:mm.", "Gunakan angka (waktu Unix) atau yyyy-MM-dd HH:mm."), emoji = "⚠️")
        return
    }
    CopyRow(t("Unix seconds", "Detik Unix"), instant.epochSecond.toString(), t)
    CopyRow(t("Unix milliseconds", "Milidetik Unix"), instant.toEpochMilli().toString(), t)
    CopyRow(t("Local time", "Waktu lokal") + " ($zone)", Timestamps.local(instant, zone), t)
    CopyRow("UTC (ISO 8601)", Timestamps.iso(instant), t)
}

// ---- JSON ------------------------------------------------------------------------------------

@Composable
private fun JsonTool(t: Lang) {
    var input by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf<JsonResult?>(null) }
    TextInput(
        input,
        {
            input = it.take(JsonFormat.MAX_CHARS)
            result = null
        },
        "JSON",
        singleLine = false,
        minLines = 6,
        capitalization = KeyboardCapitalization.None,
        keyboardType = KeyboardType.Ascii,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { result = JsonFormat.pretty(input) }, enabled = input.isNotBlank()) { Text(t("Format", "Rapikan")) }
        OutlinedButton(onClick = { result = JsonFormat.minify(input) }, enabled = input.isNotBlank()) { Text(t("Minify", "Ringkas")) }
    }
    when (val r = result) {
        null -> Unit
        is JsonResult.Error -> InfoCard(
            t("Invalid JSON — line ${r.line}, column ${r.column}: ", "JSON tidak valid — baris ${r.line}, kolom ${r.column}: ") + r.message,
            emoji = "✕",
            container = MaterialTheme.colorScheme.errorContainer,
        )
        is JsonResult.Ok -> {
            Text(t("✓ Valid JSON", "✓ JSON valid"), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            CopyRow(t("Result", "Hasil"), r.text, t)
            if (r.text.length > SHOWN_CHARS) {
                Text(t("Long result: only the start is shown. Copy takes everything.", "Hasil panjang: hanya awalnya yang ditampilkan. Salin mengambil semuanya."), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ---- Color -----------------------------------------------------------------------------------

@Composable
private fun ColorTool(t: Lang) {
    var hex by rememberSaveable { mutableStateOf("#4F46E5") }
    val color = Rgb.parseHex(hex)
    TextInput(
        hex, { hex = it.take(9) }, "HEX (#RRGGBB)",
        capitalization = KeyboardCapitalization.Characters,
        keyboardType = KeyboardType.Ascii,
        isError = color == null,
        supportingText = if (color == null) t("Use #RGB, #RRGGBB or #AARRGGBB", "Gunakan #RGB, #RRGGBB, atau #AARRGGBB") else null,
    )
    if (color == null) return
    Box(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(Color(color.argb), RoundedCornerShape(12.dp))
            .semantics { contentDescription = t("Color preview", "Pratinjau warna") },
    )
    listOf("R" to color.r, "G" to color.g, "B" to color.b).forEach { (channel, value) ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$channel $value", fontFamily = FontFamily.Monospace, modifier = Modifier.widthIn(min = 64.dp))
            Slider(
                value = value.toFloat(),
                onValueChange = { v ->
                    val n = v.toInt()
                    hex = when (channel) {
                        "R" -> color.copy(r = n)
                        "G" -> color.copy(g = n)
                        else -> color.copy(b = n)
                    }.hex
                },
                valueRange = 0f..255f,
                modifier = Modifier.weight(1f),
            )
        }
    }
    val (h, s, l) = color.hsl()
    CopyRow("HEX", color.hex, t)
    CopyRow("RGB", color.css, t)
    CopyRow("HSL", "hsl($h, $s%, $l%)", t)
}

// ---- Regex -----------------------------------------------------------------------------------

@Composable
private fun RegexTool(t: Lang) {
    var pattern by rememberSaveable { mutableStateOf("") }
    var text by rememberSaveable { mutableStateOf("") }
    var ignoreCase by rememberSaveable { mutableStateOf(false) }
    var multiline by rememberSaveable { mutableStateOf(false) }
    var result by remember { mutableStateOf<RegexResult?>(null) }

    LaunchedEffect(pattern, text, ignoreCase, multiline) {
        delay(250) // waits for typing to pause
        result = withContext(Dispatchers.Default) { RegexTester.run(pattern, text, ignoreCase, multiline) }
    }

    TextInput(pattern, { pattern = it.take(500) }, t("Pattern", "Pola"), capitalization = KeyboardCapitalization.None, keyboardType = KeyboardType.Ascii)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = ignoreCase, onClick = { ignoreCase = !ignoreCase }, label = { Text(t("Ignore case", "Abaikan huruf besar")) })
        FilterChip(selected = multiline, onClick = { multiline = !multiline }, label = { Text(t("Multiline (^ $)", "Multibaris (^ $)")) })
    }
    TextInput(
        text, { text = it.take(RegexTester.MAX_TEXT) }, t("Test text", "Teks uji"),
        singleLine = false, minLines = 4, capitalization = KeyboardCapitalization.None,
    )
    when (val r = result) {
        null -> Unit
        is RegexResult.Invalid -> InfoCard(t("Invalid pattern: ", "Pola tidak valid: ") + r.message, emoji = "✕", container = MaterialTheme.colorScheme.errorContainer)
        RegexResult.TimedOut -> InfoCard(
            t("Stopped: this pattern takes too long (catastrophic backtracking). Try a simpler pattern.", "Dihentikan: pola ini terlalu lama (backtracking berlebihan). Coba pola yang lebih sederhana."),
            emoji = "⏱️",
        )
        is RegexResult.Matches -> if (pattern.isNotEmpty()) {
            Text(
                t.count(r.matches.size, "match", "matches", "kecocokan") + if (r.truncated) t(" (first ${RegexTester.MAX_MATCHES})", " (${RegexTester.MAX_MATCHES} pertama)") else "",
                fontWeight = FontWeight.Medium,
            )
            if (r.matches.isNotEmpty()) {
                val highlight = MaterialTheme.colorScheme.tertiaryContainer
                val shown = text.take(SHOWN_CHARS)
                val annotated = buildAnnotatedString {
                    var last = 0
                    r.matches.filter { it.end <= shown.length && it.start >= last }.forEach { m ->
                        append(shown.substring(last, m.start))
                        withStyle(SpanStyle(background = highlight, fontWeight = FontWeight.Bold)) { append(shown.substring(m.start, m.end)) }
                        last = m.end
                    }
                    append(shown.substring(last))
                }
                Card(Modifier.fillMaxWidth()) {
                    SelectionContainer { Text(annotated, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(12.dp)) }
                }
                r.matches.take(20).forEachIndexed { index, m ->
                    val groups = m.groups.mapIndexed { g, v -> "  $${g + 1} = ${v ?: "—"}" }.joinToString("\n")
                    Text(
                        "#${index + 1} [${m.start}–${m.end}] \"${m.value.take(80)}\"" + if (groups.isEmpty()) "" else "\n$groups",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
