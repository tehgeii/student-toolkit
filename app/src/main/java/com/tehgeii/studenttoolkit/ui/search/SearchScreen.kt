@file:OptIn(ExperimentalMaterial3Api::class)

package com.tehgeii.studenttoolkit.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tehgeii.studenttoolkit.domain.search.TextSearch
import com.tehgeii.studenttoolkit.i18n.Lang
import com.tehgeii.studenttoolkit.i18n.dateTime
import com.tehgeii.studenttoolkit.ui.LocalContainer
import com.tehgeii.studenttoolkit.ui.LocalLang
import com.tehgeii.studenttoolkit.ui.Navigator
import com.tehgeii.studenttoolkit.ui.Routes
import com.tehgeii.studenttoolkit.ui.components.AppScaffold
import com.tehgeii.studenttoolkit.ui.components.EmptyState
import com.tehgeii.studenttoolkit.ui.components.ListColumn
import com.tehgeii.studenttoolkit.ui.components.SectionTitle
import com.tehgeii.studenttoolkit.ui.components.readable
import com.tehgeii.studenttoolkit.ui.tools.Text2
import com.tehgeii.studenttoolkit.ui.tools.ToolEntry
import com.tehgeii.studenttoolkit.ui.tools.toolSections

private data class Hit(val key: String, val title: String, val subtitle: String, val route: String)

private const val PER_GROUP = 20

private val extraTools = listOf(
    ToolEntry("📊", Text2("Grade calculator", "Kalkulator nilai"), Text2("Final score, target, scenarios", "Nilai akhir, target, simulasi"), Routes.GRADE),
    ToolEntry("🎯", Text2("GPA target planner", "Target IPK"), Text2("IPK goal", "Target IPK"), Routes.GPA_GOAL),
    ToolEntry("🔁", Text2("Grade converter", "Konversi nilai"), Text2("Score to letter", "Angka ke huruf"), Routes.GRADE_CONVERTER),
    ToolEntry("📏", Text2("Grading scale", "Skala nilai"), Text2("Letters and grade points", "Huruf dan bobot nilai"), Routes.GRADE_SCALE),
    ToolEntry("⚙️", Text2("Settings", "Pengaturan"), Text2("Language, theme, backup", "Bahasa, tema, backup"), Routes.SETTINGS),
)

/** One search box for tasks, notes, courses, grade calculations and tools. Everything runs on the phone. */
@Composable
fun SearchScreen(nav: Navigator, onBack: () -> Unit) {
    val t = LocalLang.current
    val container = LocalContainer.current
    val tasks by container.tasks.tasks.collectAsStateWithLifecycle()
    val notes by container.notes.notes.collectAsStateWithLifecycle()
    val academic by container.academic.data.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    val terms = TextSearch.terms(query)
    val groups: List<Pair<String, List<Hit>>> = if (terms.isEmpty()) {
        emptyList()
    } else {
        listOf(
            t("Tasks", "Tugas") to tasks
                .filter { task -> TextSearch.matches(terms, task.title, task.course, task.notes, task.subtasks.joinToString("\n") { it.title }) }
                .sortedWith(compareBy({ it.completed }, { it.deadline ?: Long.MAX_VALUE }))
                .map { task ->
                    val parts = listOfNotNull(
                        task.course.ifBlank { null },
                        task.deadline?.let { t.dateTime(it) },
                        if (task.completed) t("done", "selesai") else null,
                    )
                    Hit("task-${task.id}", task.title, parts.joinToString(" · "), Routes.task(task.id))
                },
            t("Notes", "Catatan") to notes
                .filter { TextSearch.matches(terms, it.title, it.content) }
                .sortedByDescending { it.updatedAt }
                .map { Hit("note-${it.id}", it.displayTitle, TextSearch.snippet(it.content, terms), Routes.note(it.id)) },
            t("Courses", "Mata kuliah") to academic.semesters.flatMap { semester ->
                semester.courses
                    .filter { TextSearch.matches(terms, it.name, it.code, it.lecturer) }
                    .map { course ->
                        val parts = listOf(semester.name, course.code, course.lecturer).filter { it.isNotBlank() }
                        Hit("course-${course.id}", course.name, parts.joinToString(" · "), Routes.semester(semester.id))
                    }
            },
            t("Grade calculations", "Perhitungan nilai") to academic.calculations
                .filter { calc -> TextSearch.matches(terms, calc.course, calc.rows.joinToString("\n") { it.name }) }
                .map { Hit("calc-${it.id}", it.course.ifBlank { t("Untitled calculation", "Perhitungan tanpa nama") }, it.rows.joinToString(", ") { r -> r.name }, Routes.grade(it.id)) },
            t("Tools", "Alat") to (toolSections.flatMap { it.second } + extraTools)
                .filter { TextSearch.matches(terms, it.title.en, it.title.id, it.subtitle.en, it.subtitle.id) }
                .map { Hit("tool-${it.route}", it.emoji + "  " + t(it.title.en, it.title.id), t(it.subtitle.en, it.subtitle.id), it.route) },
        ).filter { it.second.isNotEmpty() }
    }

    AppScaffold(title = t("Search", "Cari"), onBack = onBack) { padding ->
        ListColumn(padding) {
            item(key = "query") {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(100) },
                    placeholder = { Text(t("Tasks, notes, courses, tools…", "Tugas, catatan, mata kuliah, alat…")) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, contentDescription = t("Clear", "Hapus")) }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                    modifier = Modifier.readable().focusRequester(focus),
                )
            }
            when {
                terms.isEmpty() -> item(key = "hint") {
                    EmptyState(
                        "🔎",
                        t("Search everything", "Cari semuanya"),
                        t("Type a word from a task, note, course, lecturer or tool. Results stay on your phone.", "Ketik kata dari tugas, catatan, mata kuliah, dosen, atau alat. Pencarian hanya di HP-mu."),
                        modifier = Modifier.readable(),
                    )
                }
                groups.isEmpty() -> item(key = "none") {
                    EmptyState("🤷", t("Nothing found", "Tidak ditemukan"), t("Try another word.", "Coba kata lain."), modifier = Modifier.readable())
                }
                else -> groups.forEach { (title, hits) ->
                    item(key = "group-$title") { SectionTitle("$title (${hits.size})", Modifier.readable()) }
                    hits.take(PER_GROUP).forEach { hit ->
                        item(key = hit.key) { HitRow(hit) { nav.navigate(hit.route) } }
                    }
                    if (hits.size > PER_GROUP) {
                        item(key = "more-$title") { MoreLine(hits.size - PER_GROUP, t) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HitRow(hit: Hit, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.readable()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(hit.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (hit.subtitle.isNotBlank()) {
                    Text(
                        hit.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreLine(more: Int, t: Lang) {
    Text(
        t("+$more more — type more words to narrow it down", "+$more lagi — tambah kata untuk mempersempit"),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.readable(),
    )
}
