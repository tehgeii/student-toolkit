package com.tehgeii.studenttoolkit.data

import android.content.Context
import com.tehgeii.studenttoolkit.core.newId
import com.tehgeii.studenttoolkit.domain.academic.Semester
import com.tehgeii.studenttoolkit.domain.focus.DayFocus
import com.tehgeii.studenttoolkit.domain.notes.Note
import com.tehgeii.studenttoolkit.domain.tasks.Task
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

/** Receives task changes so reminders can follow them. */
fun interface TaskObserver {
    fun onTaskChanged(old: Task?, new: Task?)
}

class TaskRepository(private val repo: JsonRepository<List<Task>>, private val observer: TaskObserver) {
    val tasks: StateFlow<List<Task>> = repo.state

    fun get(id: String): Task? = repo.value.firstOrNull { it.id == id }

    fun upsert(task: Task) {
        val old = get(task.id)
        val saved = task.copy(updatedAt = System.currentTimeMillis())
        repo.update { list -> if (old == null) list + saved else list.map { if (it.id == saved.id) saved else it } }
        observer.onTaskChanged(old, saved)
    }

    fun setCompleted(id: String, completed: Boolean) {
        val task = get(id) ?: return
        upsert(task.copy(completed = completed, completedAt = if (completed) System.currentTimeMillis() else null))
    }

    fun toggleSubtask(taskId: String, subtaskId: String) {
        val task = get(taskId) ?: return
        upsert(task.copy(subtasks = task.subtasks.map { if (it.id == subtaskId) it.copy(done = !it.done) else it }))
    }

    fun delete(id: String): Task? {
        val old = get(id) ?: return null
        repo.update { list -> list.filterNot { it.id == id } }
        observer.onTaskChanged(old, null)
        return old
    }

    /** Puts a deleted task back exactly as it was (used by Undo). */
    fun restore(task: Task) {
        if (get(task.id) != null) return
        repo.update { it + task }
        observer.onTaskChanged(null, task)
    }

    fun replaceAll(tasks: List<Task>) {
        val old = repo.value
        repo.set(tasks)
        old.forEach { observer.onTaskChanged(it, null) }
        tasks.forEach { observer.onTaskChanged(null, it) }
    }

    fun flush() = repo.flush()
}

class NoteRepository(private val repo: JsonRepository<List<Note>>) {
    val notes: StateFlow<List<Note>> = repo.state

    fun get(id: String): Note? = repo.value.firstOrNull { it.id == id }

    /** Autosave entry point: creates the note on first real content, updates it afterwards. */
    fun save(id: String, title: String, content: String) {
        val now = System.currentTimeMillis()
        val existing = get(id)
        if (existing == null) {
            if (title.isBlank() && content.isBlank()) return
            repo.update { it + Note(id, title, content, now, now) }
        } else if (existing.title != title || existing.content != content) {
            repo.update { list -> list.map { if (it.id == id) it.copy(title = title, content = content, updatedAt = now) else it } }
        }
    }

    fun delete(id: String): Note? {
        val old = get(id) ?: return null
        repo.update { list -> list.filterNot { it.id == id } }
        return old
    }

    fun restore(note: Note) {
        if (get(note.id) == null) repo.update { it + note }
    }

    fun replaceAll(notes: List<Note>) = repo.set(notes)

    fun flush() = repo.flush()
}

class AcademicRepository(private val repo: JsonRepository<AcademicData>) {
    val data: StateFlow<AcademicData> = repo.state

    fun semester(id: String): Semester? = repo.value.semesters.firstOrNull { it.id == id }

    fun createSemester(): Semester {
        val count = repo.value.semesters.size
        val semester = Semester(id = newId(), name = "Semester ${count + 1}", updatedAt = System.currentTimeMillis())
        repo.update { it.copy(semesters = it.semesters + semester) }
        return semester
    }

    fun updateSemester(id: String, transform: (Semester) -> Semester) {
        repo.update { data ->
            data.copy(semesters = data.semesters.map {
                if (it.id == id) transform(it).copy(updatedAt = System.currentTimeMillis()) else it
            })
        }
    }

    fun deleteSemester(id: String): Semester? {
        val old = semester(id) ?: return null
        repo.update { data -> data.copy(semesters = data.semesters.filterNot { it.id == id }) }
        return old
    }

    fun restoreSemester(semester: Semester) {
        if (semester(semester.id) == null) repo.update { it.copy(semesters = it.semesters + semester) }
    }

    fun updateDraft(transform: (GradeDraft) -> GradeDraft) {
        repo.update { it.copy(draft = transform(it.draft)) }
    }

    fun replaceAll(data: AcademicData) = repo.set(data)

    fun flush() = repo.flush()
}

class FocusRepository(private val repo: JsonRepository<Map<String, DayFocus>>) {
    val stats: StateFlow<Map<String, DayFocus>> = repo.state

    fun record(day: LocalDate, minutes: Int) {
        val key = day.toString()
        repo.update { stats ->
            val updated = stats + (key to ((stats[key] ?: DayFocus()) + DayFocus(1, minutes)))
            // Keep roughly a year of history; older days are not shown anywhere.
            if (updated.size > 400) updated.toSortedMap().entries.toList().takeLast(370).associate { it.key to it.value } else updated
        }
    }

    fun replaceAll(stats: Map<String, DayFocus>) = repo.set(stats)

    fun flush() = repo.flush()
}

/** Small settings stored as one JSON string in SharedPreferences. */
class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppSettings> = _state.asStateFlow()
    val value: AppSettings get() = _state.value

    private fun load(): AppSettings {
        val raw = prefs.getString(KEY, null) ?: return AppSettings()
        return runCatching { Codecs.decodeSettings(JSONObject(raw)) }.getOrDefault(AppSettings())
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        _state.update(transform)
        prefs.edit().putString(KEY, Codecs.encodeSettings(_state.value).toString()).apply()
    }

    private companion object {
        const val KEY = "settings_json"
    }
}

object JsonDocuments {
    fun tasks(text: String): List<Task> = Codecs.decodeTasks(JSONArray(text))
    fun notes(text: String): List<Note> = Codecs.decodeNotes(JSONArray(text))
    fun academic(text: String): AcademicData = Codecs.decodeAcademic(JSONObject(text))
    fun focus(text: String): Map<String, DayFocus> = Codecs.decodeFocus(JSONObject(text))
}
