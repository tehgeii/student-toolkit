package com.tehgeii.studenttoolkit.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable

object Routes {
    const val HOME = "home"
    const val ACADEMIC = "academic"
    const val TASKS = "tasks"
    const val TOOLS = "tools"
    const val SETTINGS = "settings"
    const val GRADE = "grade"
    const val GPA_GOAL = "gpa-goal"
    const val GRADE_CONVERTER = "grade-converter"
    const val GRADE_SCALE = "grade-scale"
    const val POMODORO = "pomodoro"
    const val TIMER = "timer"
    const val STOPWATCH = "stopwatch"
    const val QR = "qr"
    const val UNITS = "units"
    const val PERCENT = "percent"
    const val NOTES = "notes"
    const val TASK_NEW = "task/new"
    const val SEARCH = "search"

    fun task(id: String) = "task/$id"
    fun note(id: String) = "note/$id"
    fun semester(id: String) = "semester/$id"
    fun grade(id: String) = "grade/$id"
    fun randomizer(tab: Int = 0) = "random/$tab"
    fun devTools(tab: Int = 0) = "dev/$tab"

    val tabs = listOf(HOME, ACADEMIC, TASKS, TOOLS)
}

/**
 * A tiny back stack. It is saved with rememberSaveable, so it survives rotation and process death,
 * and it follows the Android back button naturally: back pops screens, then returns to Home.
 */
@Stable
class Navigator(initial: List<String>) {
    val stack = mutableStateListOf<String>().apply { addAll(initial.ifEmpty { listOf(Routes.HOME) }) }

    /** Called with routes that leave the stack so their saved UI state can be dropped. */
    var onRemoved: (String) -> Unit = {}

    val current: String get() = stack.last()

    val isTabRoot: Boolean get() = stack.size == 1 && current in Routes.tabs

    val canGoBack: Boolean get() = stack.size > 1 || current != Routes.HOME

    fun navigate(route: String) {
        if (current != route) stack.add(route)
    }

    /** Replaces the current screen, e.g. "task/new" becomes "task/<id>" after saving. */
    fun replace(route: String) {
        val removed = stack.removeAt(stack.lastIndex)
        stack.add(route)
        if (removed != route) onRemoved(removed)
    }

    fun back(): Boolean {
        if (stack.size > 1) {
            onRemoved(stack.removeAt(stack.lastIndex))
            return true
        }
        if (current != Routes.HOME) {
            stack[0] = Routes.HOME
            return true
        }
        return false
    }

    fun selectTab(tab: String) {
        val removed = stack.toList()
        stack.clear()
        stack.add(tab)
        removed.filter { it !in Routes.tabs }.forEach { onRemoved(it) }
    }

    /**
     * Opens a route coming from a notification or the widget on top of the matching tab. Only routes the
     * app's own notifications and widget use are accepted; anything else another app might send is ignored.
     */
    fun openExternal(route: String) {
        val root = when {
            route == Routes.TASKS -> Routes.TASKS
            Regex("^task/[A-Za-z0-9-]{1,64}$").matches(route) -> Routes.TASKS
            route == Routes.POMODORO || route == Routes.TIMER -> Routes.TOOLS
            else -> return
        }
        selectTab(root)
        if (route != root) stack.add(route)
    }
}

@Composable
fun rememberNavigator(): Navigator = rememberSaveable(
    saver = listSaver<Navigator, String>(save = { it.stack.toList() }, restore = { Navigator(it) }),
) { Navigator(listOf(Routes.HOME)) }
