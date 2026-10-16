package com.tehgeii.studenttoolkit.ui.tools

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tehgeii.studenttoolkit.ui.LocalLang
import com.tehgeii.studenttoolkit.ui.Navigator
import com.tehgeii.studenttoolkit.ui.Routes
import com.tehgeii.studenttoolkit.ui.academic.ToolRow
import com.tehgeii.studenttoolkit.ui.components.AppScaffold
import com.tehgeii.studenttoolkit.ui.components.ListColumn
import com.tehgeii.studenttoolkit.ui.components.SectionTitle
import com.tehgeii.studenttoolkit.ui.components.readable

class Text2(val en: String, val id: String)

data class ToolEntry(val emoji: String, val title: Text2, val subtitle: Text2, val route: String)

/** Every tool in the Tools tab; the global search uses the same list. */
val toolSections = listOf(
    Text2("Classroom", "Kelas") to listOf(
        ToolEntry("👥", Text2("Group randomizer", "Acak kelompok"), Text2("Split names into fair random groups", "Bagi nama ke kelompok acak yang adil"), Routes.randomizer(0)),
        ToolEntry("🎤", Text2("Presentation order", "Urutan presentasi"), Text2("Who presents first?", "Siapa yang presentasi duluan?"), Routes.randomizer(1)),
        ToolEntry("🧩", Text2("Task assigner", "Pembagian tugas"), Text2("Randomly divide tasks among people", "Bagi tugas ke anggota secara acak"), Routes.randomizer(2)),
    ),
    Text2("Focus", "Fokus") to listOf(
        ToolEntry("🍅", Text2("Pomodoro", "Pomodoro"), Text2("Focus sessions with breaks", "Sesi fokus dengan istirahat"), Routes.POMODORO),
        ToolEntry("⏱️", Text2("Timer", "Timer"), Text2("Countdown with an alert", "Hitung mundur dengan notifikasi"), Routes.TIMER),
        ToolEntry("⏲️", Text2("Stopwatch", "Stopwatch"), Text2("With laps", "Dengan lap"), Routes.STOPWATCH),
    ),
    Text2("Utilities", "Utilitas") to listOf(
        ToolEntry("🔳", Text2("QR generator", "Pembuat QR"), Text2("Text, URL, Wi-Fi, email, phone", "Teks, URL, Wi-Fi, email, telepon"), Routes.QR),
        ToolEntry("📐", Text2("Unit converter", "Konversi satuan"), Text2("Length, storage, temperature and more", "Panjang, penyimpanan, suhu, dan lainnya"), Routes.UNITS),
        ToolEntry("％", Text2("Percentage", "Persentase"), Text2("Percent of, change, discount", "Persen dari, perubahan, diskon"), Routes.PERCENT),
        ToolEntry("📝", Text2("Notes", "Catatan"), Text2("Quick notes with search", "Catatan cepat dengan pencarian"), Routes.NOTES),
    ),
    Text2("Developer", "Developer") to listOf(
        ToolEntry("🔢", Text2("Number base", "Basis bilangan"), Text2("Binary, octal, decimal, hex", "Biner, oktal, desimal, heksadesimal"), Routes.devTools(0)),
        ToolEntry("🕒", Text2("Unix timestamp", "Timestamp Unix"), Text2("Epoch ↔ date and time", "Epoch ↔ tanggal dan jam"), Routes.devTools(1)),
        ToolEntry("{ }", Text2("JSON formatter", "Perapi JSON"), Text2("Validate, format, minify", "Validasi, rapikan, ringkas"), Routes.devTools(2)),
        ToolEntry("🎨", Text2("Color converter", "Konversi warna"), Text2("HEX, RGB, HSL", "HEX, RGB, HSL"), Routes.devTools(3)),
        ToolEntry("🔍", Text2("Regex tester", "Uji regex"), Text2("Matches and groups, highlighted", "Kecocokan dan grup, disorot"), Routes.devTools(4)),
    ),
)

@Composable
fun ToolsScreen(nav: Navigator) {
    val t = LocalLang.current
    AppScaffold(title = t("Tools", "Alat")) { padding ->
        ListColumn(padding) {
            toolSections.forEach { (title, tools) ->
                item(key = "section-${title.en}") { SectionTitle(t(title.en, title.id), Modifier.readable()) }
                tools.forEach { tool ->
                    item(key = tool.route) {
                        ToolRow(tool.emoji, t(tool.title.en, tool.title.id), t(tool.subtitle.en, tool.subtitle.id)) { nav.navigate(tool.route) }
                    }
                }
            }
        }
    }
}
