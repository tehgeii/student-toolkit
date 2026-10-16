package com.tehgeii.studenttoolkit.domain

import com.tehgeii.studenttoolkit.domain.search.TextSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextSearchTest {
    @Test
    fun normalizesCaseAccentsAndSpaces() {
        assertEquals("basis data", TextSearch.normalize("  Basís   DATA "))
        assertEquals(listOf("uts", "basis"), TextSearch.terms(" UTS  basis "))
        assertTrue(TextSearch.terms("   ").isEmpty())
    }

    @Test
    fun everyTermMustMatchSomeField() {
        val terms = TextSearch.terms("data uts")
        assertTrue(TextSearch.matches(terms, "Basis Data", "Belajar UTS"))
        assertFalse(TextSearch.matches(terms, "Basis Data", "Belajar UAS"))
        assertFalse(TextSearch.matches(emptyList(), "anything"))
    }

    @Test
    fun snippetCentersOnTheMatch() {
        val text = "a".repeat(200) + " kata kunci " + "b".repeat(200)
        val snippet = TextSearch.snippet(text, listOf("kunci"), length = 40)
        assertTrue(snippet.contains("kunci"))
        assertTrue(snippet.startsWith("…") && snippet.endsWith("…"))
        assertEquals("pendek", TextSearch.snippet("pendek", listOf("x")))
    }
}
