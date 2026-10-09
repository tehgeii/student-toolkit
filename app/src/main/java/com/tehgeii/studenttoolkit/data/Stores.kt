package com.tehgeii.studenttoolkit.data

import android.util.AtomicFile
import android.util.Log
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "StudentToolkit"

/** One JSON document on disk, written atomically so a crash mid-write never corrupts it. */
class JsonFileStore(private val file: File) {
    val name: String get() = file.name

    fun read(): String? = try {
        String(AtomicFile(file).readFully(), Charsets.UTF_8)
    } catch (e: FileNotFoundException) {
        null
    }

    fun write(text: String) {
        val atomic = AtomicFile(file)
        val out = atomic.startWrite()
        try {
            out.write(text.toByteArray(Charsets.UTF_8))
            atomic.finishWrite(out)
        } catch (e: IOException) {
            atomic.failWrite(out)
            throw e
        }
    }

    /** Keeps an unreadable file aside instead of overwriting it, so nothing is silently lost. */
    fun quarantine() {
        runCatching { file.copyTo(File(file.parentFile, "${file.name}.unreadable-${System.currentTimeMillis()}")) }
    }
}

/**
 * In-memory state backed by a JSON file. Reads happen once; writes are debounced and run on a
 * single background thread so the UI never waits for disk.
 */
class JsonRepository<T>(
    private val store: JsonFileStore,
    private val scope: CoroutineScope,
    private val writer: CoroutineDispatcher,
    private val decode: (String) -> T,
    private val encode: (T) -> String,
    empty: T,
) {
    private val _state = MutableStateFlow(load(empty))
    val state: StateFlow<T> = _state.asStateFlow()
    val value: T get() = _state.value

    private val lock = Any()
    private var pending: Job? = null

    private fun load(empty: T): T {
        val text = try {
            store.read()
        } catch (e: IOException) {
            Log.w(TAG, "Could not read ${store.name}", e)
            null
        } ?: return empty
        return try {
            decode(text)
        } catch (e: Exception) {
            Log.e(TAG, "${store.name} is unreadable; a copy was kept", e)
            store.quarantine()
            empty
        }
    }

    fun update(transform: (T) -> T) {
        _state.update(transform)
        schedule(delayMs = 300)
    }

    fun set(value: T) {
        _state.value = value
        schedule(delayMs = 0)
    }

    /** Writes pending changes now (called when the app goes to the background). */
    fun flush() = schedule(delayMs = 0)

    private fun schedule(delayMs: Long) {
        synchronized(lock) {
            pending?.cancel()
            pending = scope.launch(writer) {
                if (delayMs > 0) delay(delayMs)
                try {
                    store.write(encode(_state.value))
                } catch (e: Exception) {
                    Log.e(TAG, "Could not save ${store.name}", e)
                }
            }
        }
    }
}
