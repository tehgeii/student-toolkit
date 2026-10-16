package com.tehgeii.studenttoolkit

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant

/**
 * Keeps the last crash on the phone only (filesDir/crash). Nothing is sent anywhere: the user can
 * read, share or delete the report from Settings. It contains the app and Android version, the
 * device model and the stack trace, never tasks, notes or grades.
 */
object CrashReporter {
    private const val MAX_CHARS = 32_000

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write(app, thread, error) }
            previous?.uncaughtException(thread, error)
        }
    }

    private fun dir(context: Context) = File(context.filesDir, "crash")
    private fun reportFile(context: Context) = File(dir(context), "last-crash.txt")
    private fun unseenMarker(context: Context) = File(dir(context), "unseen")

    private fun write(context: Context, thread: Thread, error: Throwable) {
        val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        val text = buildString {
            appendLine("Student Toolkit crash report")
            appendLine("Time: ${Instant.now()}")
            appendLine("App: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Thread: ${thread.name}")
            appendLine()
            append(trace.take(MAX_CHARS))
        }
        dir(context).mkdirs()
        reportFile(context).writeText(text)
        unseenMarker(context).writeText("1")
    }

    fun report(context: Context): String? = reportFile(context).takeIf { it.isFile }?.let { runCatching { it.readText() }.getOrNull() }

    /** True once after a crash, so the next start can mention the report. */
    fun consumeUnseen(context: Context): Boolean {
        val marker = unseenMarker(context)
        return marker.exists() && marker.delete()
    }

    fun delete(context: Context) {
        dir(context).deleteRecursively()
    }
}
