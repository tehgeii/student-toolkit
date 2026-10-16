package com.tehgeii.studenttoolkit

import android.app.Application
import android.content.Context
import com.tehgeii.studenttoolkit.data.AcademicData
import com.tehgeii.studenttoolkit.data.AcademicRepository
import com.tehgeii.studenttoolkit.data.BackupManager
import com.tehgeii.studenttoolkit.data.Codecs
import com.tehgeii.studenttoolkit.data.FocusRepository
import com.tehgeii.studenttoolkit.data.JsonDocuments
import com.tehgeii.studenttoolkit.data.JsonFileStore
import com.tehgeii.studenttoolkit.data.JsonRepository
import com.tehgeii.studenttoolkit.data.NoteRepository
import com.tehgeii.studenttoolkit.data.SettingsRepository
import com.tehgeii.studenttoolkit.data.TaskRepository
import com.tehgeii.studenttoolkit.domain.focus.DayFocus
import com.tehgeii.studenttoolkit.domain.notes.Note
import com.tehgeii.studenttoolkit.domain.tasks.Task
import com.tehgeii.studenttoolkit.i18n.Lang
import com.tehgeii.studenttoolkit.notify.FocusEngine
import com.tehgeii.studenttoolkit.notify.Notifier
import com.tehgeii.studenttoolkit.notify.ReminderScheduler
import java.io.File
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher

class StudentToolkitApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        Notifier.createChannels(this, container.lang())
    }

    companion object {
        fun container(context: Context): AppContainer = (context.applicationContext as StudentToolkitApp).container
    }
}

/** Manual dependency container: everything is created lazily on first use to keep startup fast. */
class AppContainer(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val writer = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    private fun store(name: String) = JsonFileStore(File(context.filesDir, name))

    val settings: SettingsRepository by lazy { SettingsRepository(context) }

    /** Current UI language, resolved from Settings (or the phone's language). */
    fun lang(): Lang = Lang.resolve(settings.value.language)

    val reminders: ReminderScheduler by lazy { ReminderScheduler(context, ::lang) }

    val tasks: TaskRepository by lazy {
        TaskRepository(
            JsonRepository<List<Task>>(
                store("tasks.json"), scope, writer, JsonDocuments::tasks, { Codecs.encodeTasks(it).toString() }, emptyList(),
            ),
            reminders,
        )
    }

    val notes: NoteRepository by lazy {
        NoteRepository(
            JsonRepository<List<Note>>(
                store("notes.json"), scope, writer, JsonDocuments::notes, { Codecs.encodeNotes(it).toString() }, emptyList(),
            ),
        )
    }

    val academic: AcademicRepository by lazy {
        AcademicRepository(
            JsonRepository<AcademicData>(
                store("academic.json"), scope, writer, JsonDocuments::academic, { Codecs.encodeAcademic(it).toString() }, AcademicData(),
            ),
        )
    }

    val focusStats: FocusRepository by lazy {
        FocusRepository(
            JsonRepository<Map<String, DayFocus>>(
                store("focus.json"), scope, writer, JsonDocuments::focus, { Codecs.encodeFocus(it).toString() }, emptyMap(),
            ),
        )
    }

    val focus: FocusEngine by lazy { FocusEngine(context, focusStats, ::lang) }

    val backup: BackupManager by lazy { BackupManager(this, context) }

    private var appliedLang: Lang? = null

    /**
     * Runs on the main thread when the UI starts and whenever the language changes: refreshes the
     * notification channel names and restores focus alarms. Returns true when reminders should be
     * rescheduled too (see [rescheduleReminders]).
     */
    fun applyLanguage(lang: Lang): Boolean {
        if (appliedLang === lang) return false
        appliedLang = lang
        Notifier.createChannels(context, lang)
        focus.restoreAlarms()
        return true
    }

    /**
     * Re-creates every deadline alarm. Alarms disappear when the app is force-stopped (or killed by
     * some battery savers) and reminder texts follow the language, so this runs on each app start.
     */
    fun rescheduleReminders() = reminders.rescheduleAll(tasks.tasks.value)

    /** Writes any debounced changes immediately. */
    fun flush() {
        tasks.flush()
        notes.flush()
        academic.flush()
        focusStats.flush()
    }
}
