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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tehgeii.studenttoolkit.AppContainer
import com.tehgeii.studenttoolkit.BuildConfig
import com.tehgeii.studenttoolkit.data.AppSettings
import com.tehgeii.studenttoolkit.data.BackupException
import com.tehgeii.studenttoolkit.data.BackupFile
import com.tehgeii.studenttoolkit.data.Profile
import com.tehgeii.studenttoolkit.data.RestoreMode
import com.tehgeii.studenttoolkit.data.SettingsRepository
import com.tehgeii.studenttoolkit.data.ThemeMode
import com.tehgeii.studenttoolkit.domain.tasks.ReminderPresets
import com.tehgeii.studenttoolkit.i18n.AppLanguage
import com.tehgeii.studenttoolkit.i18n.Lang
import com.tehgeii.studenttoolkit.i18n.appLanguage
import com.tehgeii.studenttoolkit.i18n.backupError
import com.tehgeii.studenttoolkit.i18n.dateTime
import com.tehgeii.studenttoolkit.i18n.durationWords
import com.tehgeii.studenttoolkit.i18n.themeMode
import com.tehgeii.studenttoolkit.ui.LocalContainer
import com.tehgeii.studenttoolkit.ui.LocalLang
import com.tehgeii.studenttoolkit.ui.LocalMessenger
import com.tehgeii.studenttoolkit.ui.Messenger
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Settings are saved immediately and permanently (SharedPreferences), so they survive restarts. */
class SettingsViewModel(private val repo: SettingsRepository) : ViewModel() {
    val settings = repo.state
    fun update(transform: (AppSettings) -> AppSettings) = repo.update(transform)
}

private const val GITHUB_URL = "https://github.com/tehgeii/student-toolkit"
private const val PORTFOLIO_URL = "https://tgopremium.my.id"

@Composable
fun SettingsScreen(nav: Navigator, onBack: () -> Unit) {
    val t = LocalLang.current
    val container = LocalContainer.current
    val vm: SettingsViewModel = viewModel { SettingsViewModel(container.settings) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val messenger = LocalMessenger.current
    val scope = rememberCoroutineScope()
    var pendingRestore by remember { mutableStateOf<BackupFile?>(null) }
    var showLicenses by rememberSaveable { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) { runCatching { container.backup.writeTo(uri) }.isSuccess }
            messenger.show(
                if (ok) t("✓ Backup created", "✓ Backup dibuat")
                else t("Could not create the backup. Your data was not changed.", "Backup gagal dibuat. Datamu tidak berubah."),
            )
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { container.backup.read(uri) } }
            result.onSuccess { pendingRestore = it }
            result.onFailure { e ->
                messenger.show(
                    (e as? BackupException)?.let { t.backupError(it) }
                        ?: t("Something went wrong. Your existing data was not changed.", "Terjadi kesalahan. Data lamamu tidak berubah."),
                )
            }
        }
    }

    AppScaffold(title = t("Settings", "Pengaturan"), onBack = onBack) { padding ->
        FormColumn(padding) {
            SectionTitle(t("Language", "Bahasa"))
            Column(Modifier.selectableGroup()) {
                AppLanguage.entries.forEach { language ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = settings.language == language,
                                onClick = { vm.update { it.copy(language = language) } },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = settings.language == language, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(t.appLanguage(language), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            SectionTitle(t("Profile (optional)", "Profil (opsional)"))
            ProfileFields(settings.profile, t) { profile -> vm.update { it.copy(profile = profile) } }

            SectionTitle(t("Appearance", "Tampilan"))
            Segmented(ThemeMode.entries.toList(), settings.theme, { t.themeMode(it) }, { mode -> vm.update { it.copy(theme = mode) } })
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SwitchRow(t("Use wallpaper colors (Material You)", "Pakai warna wallpaper (Material You)"), settings.dynamicColor) { on ->
                    vm.update { it.copy(dynamicColor = on) }
                }
            }

            SectionTitle(t("Academic", "Akademik"))
            OutlinedButton(onClick = { nav.navigate(Routes.GRADE_SCALE) }, modifier = Modifier.fillMaxWidth()) {
                Text(t("Grading scale & grade points", "Skala nilai & bobot"))
            }
            Text(t("GPA decimal places", "Jumlah desimal IPK"), style = MaterialTheme.typography.bodyMedium)
            Segmented(listOf(2, 3), settings.gpaDecimals, { "$it " + t("decimals", "desimal") }, { d -> vm.update { it.copy(gpaDecimals = d) } })

            SectionTitle(t("Notifications", "Notifikasi"))
            val (notificationsAllowed, requestNotifications) = rememberNotificationsAllowed()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (notificationsAllowed) t("Notifications are allowed ✓", "Notifikasi diizinkan ✓") else t("Notifications are off", "Notifikasi mati"),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (!notificationsAllowed) TextButton(onClick = requestNotifications) { Text(t("Allow", "Izinkan")) }
            }
            ExactAlarmCard()
            Text(t("Default reminders for new deadlines", "Pengingat bawaan untuk deadline baru"), style = MaterialTheme.typography.bodyMedium)
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
                        label = { Text(t.durationWords(minutes)) },
                    )
                }
            }

            SectionTitle(t("Data & backup", "Data & backup"))
            InfoCard(
                t(
                    "Your data stays on this phone. Nothing is uploaded automatically; backups go only where you choose.",
                    "Datamu tetap di HP ini. Tidak ada yang diunggah otomatis; backup hanya disimpan ke tempat yang kamu pilih.",
                ),
                emoji = "🔒",
            )
            OutlinedButton(
                onClick = { exportLauncher.launch("StudentToolkitBackup-${LocalDate.now()}.json") },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(t("Export backup to a file", "Ekspor backup ke file")) }
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val file = withContext(Dispatchers.IO) { runCatching { container.backup.shareableFile() }.getOrNull() }
                        if (file != null) {
                            Platform.shareFile(context, file, "application/json", t("Share backup", "Bagikan backup"))
                        } else {
                            messenger.show(t("Could not prepare the backup.", "Backup gagal disiapkan."))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(t("Share backup (Drive, Telegram, …)", "Bagikan backup (Drive, Telegram, …)")) }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(t("Import backup", "Impor backup")) }

            SectionTitle(t("About", "Tentang"))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Student Toolkit", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(t("Small tools for your everyday student life.", "Alat kecil untuk kehidupan mahasiswa sehari-hari."), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        t("Version", "Versi") + " ${BuildConfig.VERSION_NAME} · " +
                            t("Android app · offline-first · no account needed", "Aplikasi Android · offline · tanpa akun"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "© 2026–present tehgeii (TGI). " + t("All rights reserved.", "Hak cipta dilindungi."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { Platform.openUrl(context, GITHUB_URL) }) { Text("GitHub") }
                        TextButton(onClick = { Platform.openUrl(context, PORTFOLIO_URL) }) { Text(t("Portfolio", "Portofolio")) }
                        TextButton(onClick = { showLicenses = true }) { Text(t("Open-source licenses", "Lisensi open source")) }
                    }
                }
            }
        }
    }

    if (showLicenses) {
        AlertDialog(
            onDismissRequest = { showLicenses = false },
            title = { Text(t("Open-source licenses", "Lisensi open source")) },
            text = {
                Column(
                    Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(OPEN_SOURCE_NOTICES, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { showLicenses = false }) { Text(t("Close", "Tutup")) } },
        )
    }

    pendingRestore?.let { file ->
        val data = file.data
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text(t("Restore backup?", "Pulihkan backup?")) },
            text = {
                val exported = if (file.exportedAt > 0) " · " + t("exported ", "diekspor ") + t.dateTime(file.exportedAt) else ""
                Text(
                    t("Backup schema v${file.schemaVersion} · app ${file.appVersion}", "Skema backup v${file.schemaVersion} · aplikasi ${file.appVersion}") +
                        exported + "\n\n" +
                        t(
                            "Contains ${data.tasks.size} tasks, ${data.notes.size} notes and ${data.academic.semesters.size} semesters.",
                            "Berisi ${data.tasks.size} tugas, ${data.notes.size} catatan, dan ${data.academic.semesters.size} semester.",
                        ) + "\n\n" +
                        t(
                            "Merge adds what you don't have yet and keeps the newest version of each item.\n" +
                                "Replace overwrites all data on this phone. A safety copy of your current data is kept either way.",
                            "Gabung menambahkan yang belum ada dan menyimpan versi terbaru tiap item.\n" +
                                "Ganti menimpa semua data di HP ini. Salinan aman data saat ini tetap disimpan.",
                        ),
                )
            },
            confirmButton = {
                Row {
                    TextButton(onClick = { restore(file, RestoreMode.REPLACE, container, scope, messenger, t) { pendingRestore = null } }) {
                        Text(t("Replace", "Ganti"), color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { restore(file, RestoreMode.MERGE, container, scope, messenger, t) { pendingRestore = null } }) {
                        Text(t("Merge", "Gabung"))
                    }
                }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text(t("Cancel", "Batal")) } },
        )
    }
}

private fun restore(
    file: BackupFile,
    mode: RestoreMode,
    container: AppContainer,
    scope: CoroutineScope,
    messenger: Messenger,
    t: Lang,
    close: () -> Unit,
) {
    close()
    scope.launch {
        val ok = withContext(Dispatchers.IO) { runCatching { container.backup.restore(file, mode) }.isSuccess }
        messenger.show(
            if (ok) t("✓ Backup restored", "✓ Backup dipulihkan")
            else t("Something went wrong. Your existing data was not changed.", "Terjadi kesalahan. Data lamamu tidak berubah."),
        )
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
private fun ProfileFields(profile: Profile, t: Lang, onChange: (Profile) -> Unit) {
    var name by rememberSaveable { mutableStateOf(profile.name) }
    var major by rememberSaveable { mutableStateOf(profile.major) }
    var university by rememberSaveable { mutableStateOf(profile.university) }
    var semester by rememberSaveable { mutableStateOf(profile.semester) }
    fun push() = onChange(Profile(name.trim(), major.trim(), university.trim(), semester.trim()))
    TextInput(name, { name = it.take(80); push() }, t("Name", "Nama"), capitalization = KeyboardCapitalization.Words)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TextInput(major, { major = it.take(80); push() }, t("Major", "Jurusan"), modifier = Modifier.weight(1f), capitalization = KeyboardCapitalization.Words)
        TextInput(semester, { semester = it.take(20); push() }, "Semester", modifier = Modifier.weight(0.6f))
    }
    TextInput(university, { university = it.take(120); push() }, t("University", "Kampus"), capitalization = KeyboardCapitalization.Words)
    Text(
        t("Used only to greet you on Home. Nothing here is required.", "Hanya dipakai untuk menyapamu di Beranda. Semuanya boleh dikosongkan."),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Third-party components shipped inside the app and the notices their licenses require. */
private const val OPEN_SOURCE_NOTICES = """Student Toolkit uses these open-source components:

• Jetpack Compose, AndroidX Activity, Lifecycle and Core — © The Android Open Source Project. Apache License 2.0.
• Kotlin standard library and kotlinx.coroutines — © JetBrains s.r.o. and contributors. Apache License 2.0.
• Material Icons — © Google LLC. Apache License 2.0.
  Full text: https://www.apache.org/licenses/LICENSE-2.0

• QR Code generator library (Java) — Copyright © Project Nayuki. MIT License.
  https://www.nayuki.io/page/qr-code-generator-library

  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
  - The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
  - The Software is provided "as is", without warranty of any kind, express or implied, including but not limited to the warranties of merchantability, fitness for a particular purpose and noninfringement. In no event shall the authors or copyright holders be liable for any claim, damages or other liability, whether in an action of contract, tort or otherwise, arising from, out of or in connection with the Software or the use or other dealings in the Software.

These licenses apply only to the components above. Student Toolkit itself is © 2026–present tehgeii (TGI), all rights reserved; see the LICENSE file in the GitHub repository."""
