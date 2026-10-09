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
        Notifier.createChannels(this)
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

    val reminders: ReminderScheduler by lazy { ReminderScheduler(context) }

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

    val focus: FocusEngine by lazy { FocusEngine(context, focusStats) }

    val backup: BackupManager by lazy { BackupManager(this, context) }

    /** Writes any debounced changes immediately. */
    fun flush() {
        tasks.flush()
        notes.flush()
        academic.flush()
        focusStats.flush()
    }
}
