@file:OptIn(ExperimentalLayoutApi::class)

package com.tehgeii.studenttoolkit.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tehgeii.studenttoolkit.AppContainer
import com.tehgeii.studenttoolkit.ui.academic.AcademicScreen
import com.tehgeii.studenttoolkit.ui.academic.GpaGoalScreen
import com.tehgeii.studenttoolkit.ui.academic.GradeCalculatorScreen
import com.tehgeii.studenttoolkit.ui.academic.GradeConverterScreen
import com.tehgeii.studenttoolkit.ui.academic.GradeScaleScreen
import com.tehgeii.studenttoolkit.ui.academic.SemesterScreen
import com.tehgeii.studenttoolkit.ui.components.AppIcons
import com.tehgeii.studenttoolkit.ui.focus.PomodoroScreen
import com.tehgeii.studenttoolkit.ui.focus.StopwatchScreen
import com.tehgeii.studenttoolkit.ui.focus.TimerScreen
import com.tehgeii.studenttoolkit.ui.home.HomeScreen
import com.tehgeii.studenttoolkit.ui.notes.NoteEditorScreen
import com.tehgeii.studenttoolkit.ui.notes.NotesScreen
import com.tehgeii.studenttoolkit.ui.settings.SettingsScreen
import com.tehgeii.studenttoolkit.ui.tasks.TaskEditorScreen
import com.tehgeii.studenttoolkit.ui.tasks.TasksScreen
import com.tehgeii.studenttoolkit.ui.tools.PercentageScreen
import com.tehgeii.studenttoolkit.ui.tools.QrScreen
import com.tehgeii.studenttoolkit.ui.tools.RandomizerScreen
import com.tehgeii.studenttoolkit.ui.tools.ToolsScreen
import com.tehgeii.studenttoolkit.ui.tools.UnitConverterScreen

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val bottomTabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Filled.Home),
    Tab(Routes.ACADEMIC, "Academic", AppIcons.School),
    Tab(Routes.TASKS, "Tasks", Icons.Filled.CheckCircle),
    Tab(Routes.TOOLS, "Tools", Icons.Filled.Build),
)

@Composable
fun AppRoot(container: AppContainer, deepLink: String?, onDeepLinkHandled: () -> Unit) {
    val navigator = rememberNavigator()
    val holder = rememberSaveableStateHolder()
    navigator.onRemoved = { holder.removeState(it) }

    LaunchedEffect(deepLink) {
        if (deepLink != null) {
            navigator.openExternal(deepLink)
            onDeepLinkHandled()
        }
    }

    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val messenger = remember(snackbarHost, scope) { Messenger(snackbarHost, scope) }

    BackHandler(enabled = navigator.canGoBack) { navigator.back() }

    val route = navigator.current
    val showBottomBar = navigator.isTabRoot

    CompositionLocalProvider(LocalContainer provides container, LocalMessenger provides messenger) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            Column(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .then(
                            if (showBottomBar) {
                                Modifier.consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    holder.SaveableStateProvider(route) {
                        Screen(route, navigator)
                    }
                }
                if (showBottomBar) {
                    NavigationBar {
                        bottomTabs.forEach { tab ->
                            NavigationBarItem(
                                selected = route == tab.route,
                                onClick = { navigator.selectTab(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = null) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                }
            }
            SnackbarHost(
                snackbarHost,
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(bottom = if (showBottomBar) 80.dp else 0.dp),
            )
        }
    }
}

@Composable
private fun Screen(route: String, nav: Navigator) {
    val back: () -> Unit = { nav.back() }
    when {
        route == Routes.HOME -> HomeScreen(nav)
        route == Routes.ACADEMIC -> AcademicScreen(nav)
        route == Routes.TASKS -> TasksScreen(nav)
        route == Routes.TOOLS -> ToolsScreen(nav)
        route == Routes.SETTINGS -> SettingsScreen(nav, back)
        route == Routes.GRADE -> GradeCalculatorScreen(nav, back)
        route == Routes.GPA_GOAL -> GpaGoalScreen(back)
        route == Routes.GRADE_CONVERTER -> GradeConverterScreen(nav, back)
        route == Routes.GRADE_SCALE -> GradeScaleScreen(back)
        route == Routes.POMODORO -> PomodoroScreen(back)
        route == Routes.TIMER -> TimerScreen(back)
        route == Routes.STOPWATCH -> StopwatchScreen(back)
        route == Routes.QR -> QrScreen(back)
        route == Routes.UNITS -> UnitConverterScreen(back)
        route == Routes.PERCENT -> PercentageScreen(back)
        route == Routes.NOTES -> NotesScreen(nav, back)
        route.startsWith("note/") -> NoteEditorScreen(route.removePrefix("note/"), back)
        route.startsWith("task/") -> TaskEditorScreen(route.removePrefix("task/"), nav, back)
        route.startsWith("semester/") -> SemesterScreen(route.removePrefix("semester/"), back)
        route.startsWith("random/") -> RandomizerScreen(route.removePrefix("random/").toIntOrNull() ?: 0, back)
        else -> HomeScreen(nav)
    }
}
