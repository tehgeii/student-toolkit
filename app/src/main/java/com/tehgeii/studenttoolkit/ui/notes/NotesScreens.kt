@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.tehgeii.studenttoolkit.ui.notes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tehgeii.studenttoolkit.core.Fmt
import com.tehgeii.studenttoolkit.core.newId
import com.tehgeii.studenttoolkit.data.NoteRepository
import com.tehgeii.studenttoolkit.domain.notes.Note
import com.tehgeii.studenttoolkit.ui.LocalContainer
import com.tehgeii.studenttoolkit.ui.LocalMessenger
import com.tehgeii.studenttoolkit.ui.Navigator
import com.tehgeii.studenttoolkit.ui.Platform
import com.tehgeii.studenttoolkit.ui.Routes
import com.tehgeii.studenttoolkit.ui.components.AppScaffold
import com.tehgeii.studenttoolkit.ui.components.ConfirmDialog
import com.tehgeii.studenttoolkit.ui.components.EmptyState
import com.tehgeii.studenttoolkit.ui.components.ListColumn
import com.tehgeii.studenttoolkit.ui.components.readable
import kotlinx.coroutines.delay

class NotesViewModel(private val repo: NoteRepository) : ViewModel() {
    val notes = repo.notes

    fun get(id: String): Note? = repo.get(id)
    fun save(id: String, title: String, content: String) = repo.save(id, title, content)
    fun delete(id: String): Note? = repo.delete(id)
    fun restore(note: Note) = repo.restore(note)
}

@Composable
private fun notesViewModel(): NotesViewModel {
    val container = LocalContainer.current
    return viewModel { NotesViewModel(container.notes) }
}

@Composable
fun NotesScreen(nav: Navigator, onBack: () -> Unit) {
    val vm = notesViewModel()
    val notes by vm.notes.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val visible = notes.filter { it.matches(query) }.sortedByDescending { it.updatedAt }

    AppScaffold(
        title = "Notes",
        onBack = onBack,
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate(Routes.note(newId())) }) {
                Icon(Icons.Filled.Add, contentDescription = "New note")
            }
        },
    ) { padding ->
        ListColumn(padding) {
            if (notes.isNotEmpty()) {
                item(key = "search") {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search notes") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, contentDescription = "Clear search") }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        modifier = Modifier.readable(),
                    )
                }
            }
            if (notes.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        emoji = "📝",
                        title = "No notes yet",
                        message = "Jot something down quickly. Notes save automatically.",
                        actionLabel = "New note",
                        onAction = { nav.navigate(Routes.note(newId())) },
                        modifier = Modifier.readable(),
                    )
                }
            } else if (visible.isEmpty()) {
                item(key = "no-match") {
                    EmptyState("🔍", "No matching notes", "Nothing contains \"$query\".", modifier = Modifier.readable())
                }
            }
            items(visible, key = { it.id }) { note ->
                Card(onClick = { nav.navigate(Routes.note(note.id)) }, modifier = Modifier.readable()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(note.displayTitle, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (note.content.isNotBlank() && note.title.isNotBlank()) {
                            Text(
                                note.content.trim(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            "Edited ${Fmt.dateTime(note.updatedAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Note editor with autosave: changes are saved shortly after typing stops and again when the
 * editor closes, so the back button never loses text.
 */
@Composable
fun NoteEditorScreen(id: String, onBack: () -> Unit) {
    val vm = notesViewModel()
    val context = LocalContext.current
    val messenger = LocalMessenger.current
    val notes by vm.notes.collectAsStateWithLifecycle()
    val stored = notes.firstOrNull { it.id == id }
    var title by rememberSaveable(id) { mutableStateOf(stored?.title ?: "") }
    var content by rememberSaveable(id) { mutableStateOf(stored?.content ?: "") }
    var askDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(title, content) {
        delay(400)
        vm.save(id, title, content)
    }
    val latestTitle by rememberUpdatedState(title)
    val latestContent by rememberUpdatedState(content)
    var deleted by rememberSaveable { mutableStateOf(false) }
    DisposableEffect(id) {
        onDispose { if (!deleted) vm.save(id, latestTitle, latestContent) }
    }

    AppScaffold(
        title = if (stored == null) "New note" else "Note",
        onBack = onBack,
        actions = {
            IconButton(
                onClick = { Platform.shareText(context, listOf(title, content).filter { it.isNotBlank() }.joinToString("\n\n")) },
                enabled = title.isNotBlank() || content.isNotBlank(),
            ) { Icon(Icons.Filled.Share, contentDescription = "Share note") }
            if (stored != null) {
                IconButton(onClick = { askDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = "Delete note") }
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier
                    .readable()
                    .padding(horizontal = 8.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                val colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                )
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Title", style = MaterialTheme.typography.titleLarge) },
                    textStyle = MaterialTheme.typography.titleLarge,
                    singleLine = true,
                    colors = colors,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("Start typing…") },
                    colors = colors,
                    minLines = 8,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (stored != null) {
                    Text(
                        "Created ${Fmt.dateTime(stored.createdAt)} · Edited ${Fmt.dateTime(stored.updatedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }

    if (askDelete && stored != null) {
        ConfirmDialog(
            title = "Delete note?",
            text = "\"${stored.displayTitle}\" will be deleted.",
            confirmLabel = "Delete",
            destructive = true,
            onDismiss = { askDelete = false },
            onConfirm = {
                deleted = true
                onBack()
                val removed = vm.delete(id)
                if (removed != null) messenger.show("Note deleted", "Undo") { vm.restore(removed) }
            },
        )
    }
}
