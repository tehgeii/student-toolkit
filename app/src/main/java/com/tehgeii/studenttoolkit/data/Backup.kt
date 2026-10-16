package com.tehgeii.studenttoolkit.data

import com.tehgeii.studenttoolkit.domain.academic.Semester
import com.tehgeii.studenttoolkit.domain.focus.DayFocus
import com.tehgeii.studenttoolkit.domain.notes.Note
import com.tehgeii.studenttoolkit.domain.tasks.Task
import org.json.JSONException
import org.json.JSONObject

/** Everything a backup can hold. */
data class BackupData(
    val settings: AppSettings,
    val academic: AcademicData,
    val tasks: List<Task>,
    val notes: List<Note>,
    val focus: Map<String, DayFocus>,
)

data class BackupFile(
    val schemaVersion: Int,
    val appVersion: String,
    val exportedAt: Long,
    val data: BackupData,
)

enum class RestoreMode { MERGE, REPLACE }

/** Why a backup cannot be read. The UI turns [reason] into a message in the user's language. */
enum class BackupError { EMPTY, TOO_LARGE, NOT_JSON, NOT_STUDENT_TOOLKIT, NO_SCHEMA, NEWER_SCHEMA, UNSUPPORTED_SCHEMA, DAMAGED, CANNOT_OPEN, CANNOT_READ, NOT_ALLOWED }

class BackupException(val reason: BackupError, val detail: String = "", cause: Throwable? = null) :
    Exception(if (detail.isEmpty()) reason.name else "${reason.name}: $detail", cause)

/**
 * StudentToolkitBackup.json format:
 * { "app": "StudentToolkit", "schemaVersion": 1, "appVersion": "...", "exportedAt": 0,
 *   "settings": {...}, "academic": {...}, "tasks": [...], "notes": [...], "focus": {...} }
 */
object BackupCodec {
    const val SCHEMA_VERSION = 1
    const val APP_ID = "StudentToolkit"
    const val MAX_BYTES = 20 * 1024 * 1024

    fun encode(data: BackupData, appVersion: String, exportedAt: Long): String = JSONObject()
        .put("app", APP_ID)
        .put("schemaVersion", SCHEMA_VERSION)
        .put("appVersion", appVersion)
        .put("exportedAt", exportedAt)
        .put("settings", Codecs.encodeSettings(data.settings))
        .put("academic", Codecs.encodeAcademic(data.academic))
        .put("tasks", Codecs.encodeTasks(data.tasks))
        .put("notes", Codecs.encodeNotes(data.notes))
        .put("focus", Codecs.encodeFocus(data.focus))
        .toString(2)

    /** Parses and fully validates a backup before anything is changed. */
    fun decode(text: String): BackupFile {
        if (text.isBlank()) throw BackupException(BackupError.EMPTY)
        if (text.length > MAX_BYTES) throw BackupException(BackupError.TOO_LARGE)
        val root = try {
            JSONObject(text)
        } catch (e: JSONException) {
            throw BackupException(BackupError.NOT_JSON, cause = e)
        }
        if (root.optString("app") != APP_ID) throw BackupException(BackupError.NOT_STUDENT_TOOLKIT)
        val version = (root.opt("schemaVersion") as? Number)?.toInt()
            ?: throw BackupException(BackupError.NO_SCHEMA)
        if (version > SCHEMA_VERSION) {
            throw BackupException(BackupError.NEWER_SCHEMA, version.toString())
        }
        if (version < 1) throw BackupException(BackupError.UNSUPPORTED_SCHEMA, version.toString())
        val migrated = migrate(root, version)
        return try {
            BackupFile(
                schemaVersion = version,
                appVersion = migrated.optString("appVersion", "unknown"),
                exportedAt = (migrated.opt("exportedAt") as? Number)?.toLong() ?: 0L,
                data = BackupData(
                    settings = Codecs.decodeSettings(migrated.optJSONObject("settings")),
                    academic = Codecs.decodeAcademic(migrated.optJSONObject("academic"), strict = true),
                    tasks = Codecs.decodeTasks(migrated.optJSONArray("tasks"), strict = true),
                    notes = Codecs.decodeNotes(migrated.optJSONArray("notes"), strict = true),
                    focus = Codecs.decodeFocus(migrated.optJSONObject("focus")),
                ),
            )
        } catch (e: IllegalArgumentException) {
            throw BackupException(BackupError.DAMAGED, e.message ?: "", e)
        } catch (e: JSONException) {
            throw BackupException(BackupError.DAMAGED, cause = e)
        }
    }

    /** Upgrades older schemas step by step (v1 → v2 → …). v1 is current, so this is the identity for now. */
    internal fun migrate(root: JSONObject, fromVersion: Int): JSONObject {
        var json = root
        var version = fromVersion
        while (version < SCHEMA_VERSION) {
            json = when (version) {
                // Future: 1 -> { migrate v1 fields into v2 shape }
                else -> json
            }
            version++
        }
        return json
    }

    /**
     * Merge keeps current settings and grade-calculator draft, adds items that do not exist yet,
     * and for items present on both sides keeps the most recently updated one. Importing the same
     * backup twice therefore never creates duplicates.
     */
    fun merge(current: BackupData, incoming: BackupData): BackupData = current.copy(
        academic = current.academic.copy(
            semesters = mergeById(current.academic.semesters, incoming.academic.semesters, Semester::id, Semester::updatedAt),
        ),
        tasks = mergeById(current.tasks, incoming.tasks, Task::id, Task::updatedAt),
        notes = mergeById(current.notes, incoming.notes, Note::id, Note::updatedAt),
        focus = (current.focus.keys + incoming.focus.keys).associateWith { day ->
            val a = current.focus[day] ?: DayFocus()
            val b = incoming.focus[day] ?: DayFocus()
            DayFocus(maxOf(a.sessions, b.sessions), maxOf(a.minutes, b.minutes))
        },
    )

    private fun <T> mergeById(current: List<T>, incoming: List<T>, id: (T) -> String, updatedAt: (T) -> Long): List<T> {
        val byId = LinkedHashMap<String, T>()
        current.forEach { byId[id(it)] = it }
        incoming.forEach { item ->
            val existing = byId[id(item)]
            if (existing == null || updatedAt(item) > updatedAt(existing)) byId[id(item)] = item
        }
        return byId.values.toList()
    }
}
