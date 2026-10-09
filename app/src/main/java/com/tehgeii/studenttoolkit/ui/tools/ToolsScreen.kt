package com.tehgeii.studenttoolkit.ui.tools

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tehgeii.studenttoolkit.ui.Navigator
import com.tehgeii.studenttoolkit.ui.Routes
import com.tehgeii.studenttoolkit.ui.academic.ToolRow
import com.tehgeii.studenttoolkit.ui.components.AppScaffold
import com.tehgeii.studenttoolkit.ui.components.ListColumn
import com.tehgeii.studenttoolkit.ui.components.SectionTitle
import com.tehgeii.studenttoolkit.ui.components.readable

private data class ToolEntry(val emoji: String, val title: String, val subtitle: String, val route: String)

private val sections = listOf(
    "Classroom" to listOf(
        ToolEntry("👥", "Group randomizer", "Split names into fair random groups", Routes.randomizer(0)),
        ToolEntry("🎤", "Presentation order", "Who presents first?", Routes.randomizer(1)),
        ToolEntry("🧩", "Task assigner", "Randomly divide tasks among people", Routes.randomizer(2)),
    ),
    "Focus" to listOf(
        ToolEntry("🍅", "Pomodoro", "Focus sessions with breaks", Routes.POMODORO),
        ToolEntry("⏱️", "Timer", "Countdown with an alert", Routes.TIMER),
        ToolEntry("⏲️", "Stopwatch", "With laps", Routes.STOPWATCH),
    ),
    "Utilities" to listOf(
        ToolEntry("🔳", "QR generator", "Text, URL, Wi-Fi, email, phone", Routes.QR),
        ToolEntry("📐", "Unit converter", "Length, storage, temperature and more", Routes.UNITS),
        ToolEntry("％", "Percentage", "Percent of, change, discount", Routes.PERCENT),
        ToolEntry("📝", "Notes", "Quick notes with search", Routes.NOTES),
    ),
)

@Composable
fun ToolsScreen(nav: Navigator) {
    AppScaffold(title = "Tools") { padding ->
        ListColumn(padding) {
            sections.forEach { (title, tools) ->
                item(key = "section-$title") { SectionTitle(title, Modifier.readable()) }
                tools.forEach { tool ->
                    item(key = tool.route) { ToolRow(tool.emoji, tool.title, tool.subtitle) { nav.navigate(tool.route) } }
                }
            }
        }
    }
}
