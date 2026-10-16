@file:OptIn(ExperimentalMaterial3Api::class)

package com.tehgeii.studenttoolkit.ui.tools

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tehgeii.studenttoolkit.domain.classroom.AssignRule
import com.tehgeii.studenttoolkit.domain.classroom.AssignmentResult
import com.tehgeii.studenttoolkit.domain.classroom.GroupMode
import com.tehgeii.studenttoolkit.domain.classroom.Randomizer
import com.tehgeii.studenttoolkit.i18n.assignRule
import com.tehgeii.studenttoolkit.ui.LocalLang
import com.tehgeii.studenttoolkit.ui.LocalMessenger
import com.tehgeii.studenttoolkit.ui.Platform
import com.tehgeii.studenttoolkit.ui.components.AppIcons
import com.tehgeii.studenttoolkit.ui.components.AppScaffold
import com.tehgeii.studenttoolkit.ui.components.FormColumn
import com.tehgeii.studenttoolkit.ui.components.InfoCard
import com.tehgeii.studenttoolkit.ui.components.NumberField
import com.tehgeii.studenttoolkit.ui.components.SectionTitle
import com.tehgeii.studenttoolkit.ui.components.Segmented
import com.tehgeii.studenttoolkit.ui.components.TextInput

/** Keeps the name lists between visits (stored locally) and the latest results while the app runs. */
class RandomizerViewModel(private val prefs: SharedPreferences) : ViewModel() {
    var namesText by mutableStateOf(prefs.getString(KEY_NAMES, "") ?: "")
        private set
    var tasksText by mutableStateOf(prefs.getString(KEY_TASKS, "") ?: "")
        private set
    var bySize by mutableStateOf(false)
    var groupNumber by mutableStateOf("2")
    var rule by mutableStateOf<AssignRule?>(null)

    var groups by mutableStateOf<List<List<String>>>(emptyList())
        private set
    var order by mutableStateOf<List<String>>(emptyList())
        private set
    var assignment by mutableStateOf<AssignmentResult?>(null)
        private set

    fun updateNames(text: String) {
        namesText = text
        prefs.edit { putString(KEY_NAMES, text) }
    }

    fun updateTasks(text: String) {
        tasksText = text
        prefs.edit { putString(KEY_TASKS, text) }
    }

    fun groupMode(): GroupMode? {
        val n = groupNumber.toIntOrNull()?.takeIf { it > 0 } ?: return null
        return if (bySize) GroupMode.BySize(n) else GroupMode.ByCount(n)
    }

    fun randomizeGroups() {
        val mode = groupMode() ?: return
        groups = Randomizer.makeGroups(Randomizer.parseNames(namesText), mode)
    }

    fun randomizeOrder() {
        order = Randomizer.presentationOrder(Randomizer.parseNames(namesText))
    }

    fun assignTasks() {
        val people = Randomizer.parseNames(namesText)
        val tasks = Randomizer.parseNames(tasksText)
        if (people.isEmpty() || tasks.isEmpty()) return
        val chosen = rule?.takeIf { it in Randomizer.rulesFor(people.size, tasks.size) }
        if (Randomizer.needsRule(people.size, tasks.size) && chosen == null) return
        assignment = Randomizer.assignTasks(people, tasks, chosen)
    }

    private companion object {
        const val KEY_NAMES = "names"
        const val KEY_TASKS = "tasks"
    }
}

private val tabLabels = listOf("Groups" to "Kelompok", "Order" to "Urutan", "Tasks" to "Tugas")

@Composable
fun RandomizerScreen(initialTab: Int, onBack: () -> Unit) {
    val t = LocalLang.current
    val context = LocalContext.current
    val vm: RandomizerViewModel = viewModel {
        RandomizerViewModel(context.applicationContext.getSharedPreferences("randomizer", android.content.Context.MODE_PRIVATE))
    }
    var tab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    val names = Randomizer.parseNames(vm.namesText)
    val duplicates = Randomizer.duplicateNames(names)

    AppScaffold(title = t("Randomizer", "Pengacak"), onBack = onBack) { padding ->
        FormColumn(padding) {
            Segmented(listOf(0, 1, 2), tab, { t(tabLabels[it].first, tabLabels[it].second) }, { tab = it })
            TextInput(
                vm.namesText,
                vm::updateNames,
                if (tab == 2) t("People — one per line", "Anggota — satu per baris") else t("Names — one per line", "Nama — satu per baris"),
                singleLine = false,
                minLines = 4,
                capitalization = KeyboardCapitalization.Words,
            )
            Text(
                buildString {
                    append(t.count(names.size, "person", "people", "orang"))
                    if (duplicates.isNotEmpty()) {
                        append(t(" · duplicate names kept as different people: ", " · nama kembar tetap dihitung orang berbeda: "))
                        append(duplicates.joinToString())
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when (tab) {
                0 -> GroupsTab(vm, names.size)
                1 -> OrderTab(vm, names.size)
                else -> TasksTab(vm, names.size)
            }
        }
    }
}

@Composable
private fun ResultActions(text: String) {
    val context = LocalContext.current
    val messenger = LocalMessenger.current
    val t = LocalLang.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = {
            Platform.copy(context, t("Randomizer result", "Hasil acak"), text)
            messenger.show(t("✓ Copied", "✓ Disalin"))
        }, modifier = Modifier.weight(1f)) {
            Icon(AppIcons.Copy, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(t("Copy", "Salin"))
        }
        OutlinedButton(onClick = { Platform.shareText(context, text) }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Share, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(t("Share", "Bagikan"))
        }
    }
}

@Composable
private fun RandomizeButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Icon(AppIcons.Shuffle, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
private fun GroupsTab(vm: RandomizerViewModel, people: Int) {
    val t = LocalLang.current
    Segmented(
        listOf(false, true),
        vm.bySize,
        { if (it) t("People per group", "Orang per kelompok") else t("Number of groups", "Jumlah kelompok") },
        { vm.bySize = it },
    )
    NumberField(
        vm.groupNumber,
        { vm.groupNumber = it.take(4) },
        if (vm.bySize) t("People per group", "Orang per kelompok") else t("Number of groups", "Jumlah kelompok"),
        Modifier.fillMaxWidth(),
        decimal = false,
    )
    val mode = vm.groupMode()
    if (mode != null && people > 0) {
        val count = Randomizer.groupCount(people, mode)
        val sizes = Randomizer.groupSizes(people, count)
        InfoCard(t.count(people, "person", "people", "orang") + " → " + t.count(count, "group", "groups", "kelompok") + " (${sizes.joinToString(" + ")})")
    }
    RandomizeButton(if (vm.groups.isEmpty()) t("Randomize", "Acak") else t("Randomize again", "Acak lagi"), mode != null && people > 0) { vm.randomizeGroups() }
    if (vm.groups.isNotEmpty()) {
        vm.groups.forEachIndexed { index, group ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(t("Group", "Kelompok") + " ${index + 1} · ${group.size}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    group.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
                }
            }
        }
        ResultActions(vm.groups.mapIndexed { i, g -> t("Group", "Kelompok") + " ${i + 1}\n" + g.joinToString("\n") }.joinToString("\n\n"))
    }
}

@Composable
private fun OrderTab(vm: RandomizerViewModel, people: Int) {
    val t = LocalLang.current
    RandomizeButton(if (vm.order.isEmpty()) t("Randomize order", "Acak urutan") else t("Randomize again", "Acak lagi"), people > 0) { vm.randomizeOrder() }
    if (vm.order.isNotEmpty()) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                vm.order.forEachIndexed { index, name ->
                    Row {
                        Text("${index + 1}.", modifier = Modifier.width(36.dp), fontWeight = FontWeight.Bold)
                        Text(name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        ResultActions(vm.order.mapIndexed { i, n -> "${i + 1}. $n" }.joinToString("\n"))
    }
}

@Composable
private fun TasksTab(vm: RandomizerViewModel, people: Int) {
    val t = LocalLang.current
    TextInput(
        vm.tasksText,
        vm::updateTasks,
        t("Tasks — one per line", "Tugas — satu per baris"),
        singleLine = false,
        minLines = 3,
    )
    val tasks = Randomizer.parseNames(vm.tasksText).size
    Text(t.count(tasks, "task", "tasks", "tugas"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val needsRule = Randomizer.needsRule(people, tasks)
    if (needsRule) {
        SectionTitle(t("$people people but $tasks tasks — choose a rule", "$people orang tapi $tasks tugas — pilih aturannya"))
        Column(Modifier.selectableGroup()) {
            Randomizer.rulesFor(people, tasks).forEach { option ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(selected = vm.rule == option, onClick = { vm.rule = option }, role = Role.RadioButton)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = vm.rule == option, onClick = null)
                    Spacer(Modifier.width(8.dp))
                    Text(t.assignRule(option))
                }
            }
        }
    }
    val ruleOk = !needsRule || (vm.rule != null && vm.rule in Randomizer.rulesFor(people, tasks))
    RandomizeButton(
        if (vm.assignment == null) t("Assign tasks", "Bagi tugas") else t("Randomize again", "Acak lagi"),
        people > 0 && tasks > 0 && ruleOk,
    ) { vm.assignTasks() }
    val result = vm.assignment
    if (result != null) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                result.assignments.forEach { a ->
                    Text("${a.person} → ${a.tasks.joinToString(", ")}", style = MaterialTheme.typography.bodyLarge)
                }
                if (result.unassignedPeople.isNotEmpty()) {
                    Text(t("Unassigned people: ", "Tidak dapat tugas: ") + result.unassignedPeople.joinToString(), style = MaterialTheme.typography.bodyMedium)
                }
                if (result.unassignedTasks.isNotEmpty()) {
                    Text(t("Unassigned tasks: ", "Tugas tersisa: ") + result.unassignedTasks.joinToString(), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        ResultActions(
            buildString {
                result.assignments.forEach { append("${it.person} → ${it.tasks.joinToString(", ")}\n") }
                if (result.unassignedPeople.isNotEmpty()) append(t("Unassigned people: ", "Tidak dapat tugas: ") + result.unassignedPeople.joinToString() + "\n")
                if (result.unassignedTasks.isNotEmpty()) append(t("Unassigned tasks: ", "Tugas tersisa: ") + result.unassignedTasks.joinToString() + "\n")
            }.trim(),
        )
    }
}
