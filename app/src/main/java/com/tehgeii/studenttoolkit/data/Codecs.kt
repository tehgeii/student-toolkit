package com.tehgeii.studenttoolkit.data

import com.tehgeii.studenttoolkit.domain.academic.Course
import com.tehgeii.studenttoolkit.domain.academic.GradeBand
import com.tehgeii.studenttoolkit.domain.academic.GradeScale
import com.tehgeii.studenttoolkit.domain.academic.Semester
import com.tehgeii.studenttoolkit.domain.focus.DayFocus
import com.tehgeii.studenttoolkit.domain.notes.Note
import com.tehgeii.studenttoolkit.domain.tasks.Priority
import com.tehgeii.studenttoolkit.domain.tasks.Subtask
import com.tehgeii.studenttoolkit.domain.tasks.Task
import com.tehgeii.studenttoolkit.domain.tasks.TaskSort
import com.tehgeii.studenttoolkit.i18n.AppLanguage
import org.json.JSONArray
import org.json.JSONObject

/**
 * Hand-written JSON codecs (org.json ships with Android, so no serialization library is needed).
 * Decoders treat input as untrusted: wrong types or missing required fields throw
 * [IllegalArgumentException]; nothing in the input is ever executed.
 */
object Codecs {
    private const val MAX_TEXT = 100_000

    // ---- helpers -------------------------------------------------------------------------

    private fun JSONObject.requireString(key: String): String {
        val value = opt(key)
        require(value is String && value.isNotBlank()) { "Missing \"$key\"" }
        return value.take(MAX_TEXT)
    }

    private fun JSONObject.text(key: String): String = (opt(key) as? String)?.take(MAX_TEXT) ?: ""

    private fun JSONObject.longOrNull(key: String): Long? = (opt(key) as? Number)?.toLong()

    private fun JSONObject.intOrNull(key: String): Int? = (opt(key) as? Number)?.toInt()

    private fun JSONObject.doubleOrNull(key: String): Double? =
        (opt(key) as? Number)?.toDouble()?.takeIf { it.isFinite() }

    private fun JSONObject.bool(key: String, default: Boolean = false): Boolean = (opt(key) as? Boolean) ?: default

    private fun JSONObject.putIfNotNull(key: String, value: Any?): JSONObject {
        if (value != null) put(key, value)
        return this
    }

    private inline fun <T> JSONArray?.decodeEach(strict: Boolean, decode: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        val out = ArrayList<T>(length())
        for (i in 0 until length()) {
            val item = optJSONObject(i)
            if (item == null) {
                require(!strict) { "Item ${i + 1} is not an object" }
                continue
            }
            try {
                out += decode(item)
            } catch (e: IllegalArgumentException) {
                if (strict) throw IllegalArgumentException("Item ${i + 1}: ${e.message}", e)
            }
        }
        return out
    }

    private fun <T> List<T>.toJsonArray(encode: (T) -> Any): JSONArray =
        JSONArray().also { array -> forEach { array.put(encode(it)) } }

    private inline fun <reified E : Enum<E>> enumOf(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default

    // ---- tasks ---------------------------------------------------------------------------

    fun encodeTask(t: Task): JSONObject = JSONObject()
        .put("id", t.id)
        .put("title", t.title)
        .put("course", t.course)
        .putIfNotNull("deadline", t.deadline)
        .put("priority", t.priority.name)
        .putIfNotNull("estimateMinutes", t.estimateMinutes)
        .put("notes", t.notes)
        .put("completed", t.completed)
        .putIfNotNull("completedAt", t.completedAt)
        .put("subtasks", t.subtasks.toJsonArray { JSONObject().put("id", it.id).put("title", it.title).put("done", it.done) })
        .put("reminders", t.reminders.toJsonArray { it })
        .put("createdAt", t.createdAt)
        .put("updatedAt", t.updatedAt)

    fun decodeTask(o: JSONObject, strict: Boolean = false): Task {
        val reminders = o.optJSONArray("reminders")
        return Task(
            id = o.requireString("id"),
            title = o.requireString("title"),
            course = o.text("course"),
            deadline = o.longOrNull("deadline"),
            priority = enumOf(o.opt("priority") as? String, Priority.MEDIUM),
            estimateMinutes = o.intOrNull("estimateMinutes")?.takeIf { it > 0 },
            notes = o.text("notes"),
            completed = o.bool("completed"),
            completedAt = o.longOrNull("completedAt"),
            subtasks = o.optJSONArray("subtasks").decodeEach(strict) {
                Subtask(it.requireString("id"), it.text("title"), it.bool("done"))
            },
            reminders = if (reminders == null) emptyList() else (0 until reminders.length())
                .mapNotNull { (reminders.opt(it) as? Number)?.toInt() }
                .filter { it in 1..(366 * 24 * 60) }
                .distinct(),
            createdAt = o.longOrNull("createdAt") ?: 0L,
            updatedAt = o.longOrNull("updatedAt") ?: 0L,
        )
    }

    fun encodeTasks(tasks: List<Task>): JSONArray = tasks.toJsonArray(::encodeTask)

    fun decodeTasks(array: JSONArray?, strict: Boolean = false): List<Task> =
        array.decodeEach(strict) { decodeTask(it, strict) }.distinctBy { it.id }

    // ---- notes ---------------------------------------------------------------------------

    fun encodeNote(n: Note): JSONObject = JSONObject()
        .put("id", n.id)
        .put("title", n.title)
        .put("content", n.content)
        .put("createdAt", n.createdAt)
        .put("updatedAt", n.updatedAt)

    fun decodeNote(o: JSONObject): Note = Note(
        id = o.requireString("id"),
        title = o.text("title"),
        content = (o.opt("content") as? String) ?: "",
        createdAt = o.longOrNull("createdAt") ?: 0L,
        updatedAt = o.longOrNull("updatedAt") ?: 0L,
    )

    fun encodeNotes(notes: List<Note>): JSONArray = notes.toJsonArray(::encodeNote)

    fun decodeNotes(array: JSONArray?, strict: Boolean = false): List<Note> =
        array.decodeEach(strict, ::decodeNote).distinctBy { it.id }

    // ---- academic ------------------------------------------------------------------------

    fun encodeSemester(s: Semester): JSONObject = JSONObject()
        .put("id", s.id)
        .put("name", s.name)
        .put("courses", s.courses.toJsonArray {
            JSONObject().put("id", it.id).put("name", it.name).put("credits", it.credits).put("grade", it.grade)
        })
        .putIfNotNull("manualGpa", s.manualGpa)
        .putIfNotNull("manualCredits", s.manualCredits)
        .put("useManual", s.useManual)
        .put("updatedAt", s.updatedAt)

    fun decodeSemester(o: JSONObject, strict: Boolean = false): Semester = Semester(
        id = o.requireString("id"),
        name = o.text("name"),
        courses = o.optJSONArray("courses").decodeEach(strict) {
            Course(
                id = it.requireString("id"),
                name = it.text("name"),
                credits = (it.intOrNull("credits") ?: 0).coerceIn(0, 99),
                grade = it.text("grade"),
            )
        },
        manualGpa = o.doubleOrNull("manualGpa"),
        manualCredits = o.intOrNull("manualCredits"),
        useManual = o.bool("useManual"),
        updatedAt = o.longOrNull("updatedAt") ?: 0L,
    )

    fun encodeDraft(d: GradeDraft): JSONObject = JSONObject()
        .put("course", d.course)
        .put("rows", d.rows.toJsonArray {
            JSONObject().put("id", it.id).put("name", it.name).put("weight", it.weight).put("score", it.score)
        })

    fun decodeDraft(o: JSONObject?): GradeDraft {
        if (o == null) return GradeDraft()
        val rows = o.optJSONArray("rows").decodeEach(strict = false) {
            GradeDraftRow(it.requireString("id"), it.text("name"), it.text("weight").take(12), it.text("score").take(12))
        }
        return GradeDraft(o.text("course"), rows)
    }

    fun encodeAcademic(a: AcademicData): JSONObject = JSONObject()
        .put("semesters", a.semesters.toJsonArray(::encodeSemester))
        .put("draft", encodeDraft(a.draft))

    fun decodeAcademic(o: JSONObject?, strict: Boolean = false): AcademicData {
        if (o == null) return AcademicData()
        return AcademicData(
            semesters = o.optJSONArray("semesters").decodeEach(strict) { decodeSemester(it, strict) }.distinctBy { it.id },
            draft = decodeDraft(o.optJSONObject("draft")),
        )
    }

    // ---- focus statistics ----------------------------------------------------------------

    private val isoDate = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    fun encodeFocus(stats: Map<String, DayFocus>): JSONObject = JSONObject().also { o ->
        stats.toSortedMap().forEach { (day, f) -> o.put(day, JSONObject().put("sessions", f.sessions).put("minutes", f.minutes)) }
    }

    fun decodeFocus(o: JSONObject?): Map<String, DayFocus> {
        if (o == null) return emptyMap()
        val out = sortedMapOf<String, DayFocus>()
        for (key in o.keys()) {
            if (!isoDate.matches(key)) continue
            val day = o.optJSONObject(key) ?: continue
            val sessions = day.intOrNull("sessions") ?: 0
            val minutes = day.intOrNull("minutes") ?: 0
            if (sessions >= 0 && minutes >= 0) out[key] = DayFocus(sessions, minutes)
        }
        return out
    }

    // ---- settings ------------------------------------------------------------------------

    fun encodeScale(scale: GradeScale): JSONArray = scale.bands.toJsonArray {
        JSONObject().put("letter", it.letter).put("minScore", it.minScore).put("points", it.points)
    }

    fun decodeScale(array: JSONArray?): GradeScale? {
        if (array == null) return null
        val bands = array.decodeEach(strict = true) {
            GradeBand(
                letter = it.requireString("letter").take(4),
                minScore = requireNotNull(it.doubleOrNull("minScore")) { "Missing minScore" },
                points = requireNotNull(it.doubleOrNull("points")) { "Missing points" },
            )
        }
        val scale = GradeScale(bands)
        return if (scale.validate().isEmpty()) scale else null
    }

    fun encodeSettings(s: AppSettings): JSONObject = JSONObject()
        .put("theme", s.theme.name)
        .put("language", s.language.name)
        .put("dynamicColor", s.dynamicColor)
        .put("gpaDecimals", s.gpaDecimals)
        .put("gradeScale", encodeScale(s.gradeScale))
        .put("defaultReminders", s.defaultReminders.toJsonArray { it })
        .put("taskSort", s.taskSort.name)
        .put(
            "profile",
            JSONObject()
                .put("name", s.profile.name)
                .put("major", s.profile.major)
                .put("university", s.profile.university)
                .put("semester", s.profile.semester),
        )

    fun decodeSettings(o: JSONObject?): AppSettings {
        if (o == null) return AppSettings()
        val defaults = AppSettings()
        val reminders = o.optJSONArray("defaultReminders")
        val profile = o.optJSONObject("profile")
        return AppSettings(
            theme = enumOf(o.opt("theme") as? String, defaults.theme),
            language = enumOf(o.opt("language") as? String, defaults.language),
            dynamicColor = o.bool("dynamicColor", defaults.dynamicColor),
            gpaDecimals = (o.intOrNull("gpaDecimals") ?: 2).coerceIn(2, 3),
            gradeScale = runCatching { decodeScale(o.optJSONArray("gradeScale")) }.getOrNull() ?: defaults.gradeScale,
            defaultReminders = if (reminders == null) defaults.defaultReminders else (0 until reminders.length())
                .mapNotNull { (reminders.opt(it) as? Number)?.toInt() }
                .filter { it > 0 }
                .distinct(),
            taskSort = enumOf(o.opt("taskSort") as? String, defaults.taskSort),
            profile = if (profile == null) Profile() else Profile(
                name = profile.text("name").take(80),
                major = profile.text("major").take(80),
                university = profile.text("university").take(120),
                semester = profile.text("semester").take(20),
            ),
        )
    }
}
