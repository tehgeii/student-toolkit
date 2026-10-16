package com.tehgeii.studenttoolkit.i18n

import com.tehgeii.studenttoolkit.domain.tasks.Priority
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class LangTest {
    @Test
    fun resolvesLanguage() {
        assertSame(Lang.EN, Lang.resolve(AppLanguage.ENGLISH, "in"))
        assertSame(Lang.ID, Lang.resolve(AppLanguage.INDONESIAN, "en"))
        assertSame(Lang.ID, Lang.resolve(AppLanguage.SYSTEM, "in"))
        assertSame(Lang.ID, Lang.resolve(AppLanguage.SYSTEM, "id"))
        assertSame(Lang.EN, Lang.resolve(AppLanguage.SYSTEM, "de"))
    }

    @Test
    fun picksTheRightText() {
        assertEquals("Tasks", Lang.EN("Tasks", "Tugas"))
        assertEquals("Tugas", Lang.ID("Tasks", "Tugas"))
        assertEquals("Tinggi", Lang.ID.priority(Priority.HIGH))
        assertEquals("1 task", Lang.EN.count(1, "task", "tasks", "tugas"))
        assertEquals("3 tugas", Lang.ID.count(3, "task", "tasks", "tugas"))
    }

    @Test
    fun localizedDatesAndDurations() {
        val date = LocalDate.of(2026, 10, 9)
        assertEquals("Friday, 9 October 2026", Lang.EN.longDate(date))
        assertEquals("Jumat, 9 Oktober 2026", Lang.ID.longDate(date))
        assertEquals("3h 30m", Lang.EN.durationShort(210))
        assertEquals("3 jam 30 mnt", Lang.ID.durationShort(210))
        assertEquals("45 mnt", Lang.ID.durationShort(45))
    }
}
