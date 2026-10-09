package com.tehgeii.studenttoolkit.domain.notes

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val isBlank: Boolean get() = title.isBlank() && content.isBlank()

    val displayTitle: String
        get() = title.ifBlank { content.lineSequence().firstOrNull { it.isNotBlank() }?.trim()?.take(60) ?: "Untitled" }

    fun matches(query: String): Boolean {
        val q = query.trim()
        return q.isEmpty() || title.contains(q, ignoreCase = true) || content.contains(q, ignoreCase = true)
    }
}
