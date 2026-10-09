@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.tehgeii.studenttoolkit.ui.settings

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tehgeii.studenttoolkit.BuildConfig
import com.tehgeii.studenttoolkit.core.Fmt
import com.tehgeii.studenttoolkit.data.AppSettings
import com.tehgeii.studenttoolkit.data.BackupException
import com.tehgeii.studenttoolkit.data.BackupFile
import com.tehgeii.studenttoolkit.data.Profile
import com.tehgeii.studenttoolkit.data.RestoreMode
import com.tehgeii.studenttoolkit.data.SettingsRepository
import com.tehgeii.studenttoolkit.data.ThemeMode
import com.tehgeii.studenttoolkit.domain.tasks.ReminderPresets
import com.tehgeii.studenttoolkit.ui.LocalContainer
import com.tehgeii.studenttoolkit.ui.LocalMessenger
import com.tehgeii.studenttoolkit.ui.Navigator
import com.tehgeii.studenttoolkit.ui.Platform
import com.tehgeii.studenttoolkit.ui.Routes
import com.tehgeii.studenttoolkit.ui.components.AppScaffold
import com.tehgeii.studenttoolkit.ui.components.ExactAlarmCard
import com.tehgeii.studenttoolkit.ui.components.FormColumn
import com.tehgeii.studenttoolkit.ui.components.InfoCard
import com.tehgeii.studenttoolkit.ui.components.SectionTitle
import com.tehgeii.studenttoolkit.ui.components.Segmented
import com.tehgeii.studenttoolkit.ui.components.TextInput
import com.tehgeii.studenttoolkit.ui.components.rememberNotificationsAllowed
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(private val repo: SettingsRepository) : ViewModel() {
    val settings = repo.state
    fun update(transform: (AppSettings) -> AppSettings) = repo.update(transform)
}

private const val GITHUB_URL = "https://github.com/tehgeii/student-toolkit"
private const val PORTFOLIO_URL = "https://tgopremium.my.id"

@Composable
fun SettingsScreen(nav: Navigator, onBack: () -> Unit) {
    val container = LocalContainer.current
    val vm: SettingsViewModel = viewModel { SettingsViewModel(container.settings) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val messenger = LocalMessenger.current
    val scope = rememberCoroutineScope()
    var pendingRestore by remember { mutableStateOf<BackupFile?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) { runCatching { container.backup.writeTo(uri) }.isSuccess }
            messenger.show(if (ok) "✓ Backup created" else "Could not create the backup. Your data was not changed.")
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { container.backup.read(uri) } }
            result.onSuccess { pendingRestore = it }
            result.onFailure { e ->
                messenger.show((e as? BackupException)?.message ?: "Something went wrong. Your existing data was not changed.")
            }
        }
    }

    AppScaffold(title = "Settings", onBack = onBack) { padding ->
        FormColumn(padding) {
            SectionTitle("Profile (optional)")
            ProfileFields(settings.profile) { profile -> vm.update { it.copy(profile = profile) } }

            SectionTitle("Appearance")
            Segmented(ThemeMode.entries.toList(), settings.theme, { it.label }, { mode -> vm.update { it.copy(theme = mode) } })
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SwitchRow("Use wallpaper colors (Material You)", settings.dynamicColor) { on -> vm.update { it.copy(dynamicColor = on) } }
            }

            SectionTitle("Academic")
            OutlinedButton(onClick = { nav.navigate(Routes.GRADE_SCALE) }, modifier = Modifier.fillMaxWidth()) {
                Text("Grading scale & grade points")
            }
            Text("GPA decimal places", style = MaterialTheme.typography.bodyMedium)
            Segmented(listOf(2, 3), settings.gpaDecimals, { "$it decimals" }, { d -> vm.update { it.copy(gpaDecimals = d) } })

            SectionTitle("Notifications")
            val (notificationsAllowed, requestNotifications) = rememberNotificationsAllowed()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (notificationsAllowed) "Notifications are allowed ✓" else "Notifications are off",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (!notificationsAllowed) TextButton(onClick = requestNotifications) { Text("Allow") }
            }
            ExactAlarmCard()
            Text("Default reminders for new deadlines", style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReminderPresets.options.forEach { minutes ->
                    val selected = minutes in settings.defaultReminders
                    FilterChip(
                        selected = selected,
                        onClick = {
                            vm.update {
                                it.copy(defaultReminders = if (selected) it.defaultReminders - minutes else (it.defaultReminders + minutes).sorted())
                            }
                        },
                        label = { Text(ReminderPresets.label(minutes)) },
                    )
                }
            }

            SectionTitle("Data & backup")
            InfoCard("Your data stays on this phone. Nothing is uploaded automatically; backups go only where you choose.", emoji = "🔒")
            OutlinedButton(
                onClick = { exportLauncher.launch("StudentToolkitBackup-${LocalDate.now()}.json") },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Export backup to a file") }
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val file = withContext(Dispatchers.IO) { runCatching { container.backup.shareableFile() }.getOrNull() }
                        if (file != null) Platform.shareFile(context, file, "application/json", "Share backup")
                        else messenger.show("Could not prepare the backup.")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Share backup (Drive, Telegram, …)") }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Import backup") }

            SectionTitle("About")
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Student Toolkit", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Small tools for your everyday student life.", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Version ${BuildConfig.VERSION_NAME} · Android app · offline-first · no account needed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { Platform.openUrl(context, GITHUB_URL) }) { Text("GitHub") }
                        TextButton(onClick = { Platform.openUrl(context, PORTFOLIO_URL) }) { Text("Portfolio") }
                    }
                }
            }
        }
    }

    pendingRestore?.let { file ->
        val data = file.data
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Restore backup?") },
            text = {
                Text(
                    "Backup schema v${file.schemaVersion} · app ${file.appVersion}" +
                        (if (file.exportedAt > 0) " · exported ${Fmt.dateTime(file.exportedAt)}" else "") +
                        "\n\nContains ${data.tasks.size} tasks, ${data.notes.size} notes and ${data.academic.semesters.size} semesters.\n\n" +
                        "Merge adds what you don't have yet and keeps the newest version of each item.\n" +
                        "Replace overwrites all data on this phone. A safety copy of your current data is kept either way.",
                )
            },
            confirmButton = {
                Row {
                    TextButton(onClick = { restore(file, RestoreMode.REPLACE, container, scope, messenger) { pendingRestore = null } }) {
                        Text("Replace", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { restore(file, RestoreMode.MERGE, container, scope, messenger) { pendingRestore = null } }) {
                        Text("Merge")
                    }
                }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Cancel") } },
        )
    }
}

private fun restore(
    file: BackupFile,
    mode: RestoreMode,
    container: com.tehgeii.studenttoolkit.AppContainer,
    scope: kotlinx.coroutines.CoroutineScope,
    messenger: com.tehgeii.studenttoolkit.ui.Messenger,
    close: () -> Unit,
) {
    close()
    scope.launch {
        val ok = withContext(Dispatchers.IO) { runCatching { container.backup.restore(file, mode) }.isSuccess }
        messenger.show(if (ok) "✓ Backup restored" else "Something went wrong. Your existing data was not changed.")
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ProfileFields(profile: Profile, onChange: (Profile) -> Unit) {
    var name by rememberSaveable { mutableStateOf(profile.name) }
    var major by rememberSaveable { mutableStateOf(profile.major) }
    var university by rememberSaveable { mutableStateOf(profile.university) }
    var semester by rememberSaveable { mutableStateOf(profile.semester) }
    fun push() = onChange(Profile(name.trim(), major.trim(), university.trim(), semester.trim()))
    TextInput(name, { name = it.take(80); push() }, "Name", capitalization = KeyboardCapitalization.Words)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TextInput(major, { major = it.take(80); push() }, "Major", modifier = Modifier.weight(1f), capitalization = KeyboardCapitalization.Words)
        TextInput(semester, { semester = it.take(20); push() }, "Semester", modifier = Modifier.weight(0.6f))
    }
    TextInput(university, { university = it.take(120); push() }, "University", capitalization = KeyboardCapitalization.Words)
    Text(
        "Used only to greet you on Home. Nothing here is required.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
