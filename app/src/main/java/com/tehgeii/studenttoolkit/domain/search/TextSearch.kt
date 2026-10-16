package com.tehgeii.studenttoolkit.domain.search

import java.text.Normalizer

/** Forgiving, offline text matching for the global search. */
object TextSearch {
    private val marks = Regex("\\p{Mn}+")
    private val spaces = Regex("\\s+")

    /** Lower case, accents removed, whitespace collapsed: "  Basís   DATA " -> "basis data". */
    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(marks, "")
            .lowercase()
            .replace(spaces, " ")
            .trim()

    /** Words of the query; empty when there is nothing worth searching for. */
    fun terms(query: String): List<String> = normalize(query).split(' ').filter { it.isNotEmpty() }

    /** True when every term appears in at least one of [fields] (order and case do not matter). */
    fun matches(terms: List<String>, vararg fields: String): Boolean {
        if (terms.isEmpty()) return false
        val haystack = fields.joinToString("\n") { normalize(it) }
        return terms.all { haystack.contains(it) }
    }

    /**
     * A short piece of [text] around the first term, for showing why a long note matched.
     * Returns the start of the text when no term is found.
     */
    fun snippet(text: String, terms: List<String>, length: Int = 90): String {
        val flat = text.replace(spaces, " ").trim()
        if (flat.length <= length) return flat
        val lower = flat.lowercase()
        val hit = terms.map { lower.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: 0
        val start = (hit - length / 3).coerceIn(0, flat.length - length)
        val body = flat.substring(start, start + length).trim()
        return (if (start > 0) "…" else "") + body + (if (start + length < flat.length) "…" else "")
    }
}
