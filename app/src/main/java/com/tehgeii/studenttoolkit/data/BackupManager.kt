package com.tehgeii.studenttoolkit.data

import android.content.Context
import android.net.Uri
import com.tehgeii.studenttoolkit.AppContainer
import com.tehgeii.studenttoolkit.BuildConfig
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

/** Export, share and restore of StudentToolkitBackup.json. Nothing is uploaded anywhere automatically. */
class BackupManager(private val container: AppContainer, private val context: Context) {

    fun currentData(): BackupData = BackupData(
        settings = container.settings.value,
        academic = container.academic.data.value,
        tasks = container.tasks.tasks.value,
        notes = container.notes.notes.value,
        focus = container.focusStats.stats.value,
    )

    fun exportJson(): String = BackupCodec.encode(currentData(), BuildConfig.VERSION_NAME, System.currentTimeMillis())

    /** Writes a backup to a document the user picked (Files, Google Drive, …). */
    fun writeTo(uri: Uri) {
        val stream = context.contentResolver.openOutputStream(uri, "w") ?: throw IOException("Could not open the file.")
        stream.use { it.write(exportJson().toByteArray(Charsets.UTF_8)) }
    }

    /** Creates a backup file in the cache for the Android share sheet. */
    fun shareableFile(): File {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        return File(dir, "StudentToolkitBackup.json").apply { writeText(exportJson()) }
    }

    /** Reads and validates a backup; throws [BackupException] without touching current data. */
    fun read(uri: Uri): BackupFile {
        val bytes = try {
            context.contentResolver.openInputStream(uri)?.use(::readLimited)
        } catch (e: IOException) {
            throw BackupException(BackupError.CANNOT_READ, cause = e)
        } catch (e: SecurityException) {
            throw BackupException(BackupError.NOT_ALLOWED, cause = e)
        } ?: throw BackupException(BackupError.CANNOT_OPEN)
        return BackupCodec.decode(String(bytes, Charsets.UTF_8))
    }

    /**
     * Applies a validated backup. A safety copy of the current data is written first, so a
     * restore can always be undone by importing that copy.
     */
    fun restore(file: BackupFile, mode: RestoreMode) {
        val current = currentData()
        saveSafetyCopy()
        val result = when (mode) {
            RestoreMode.MERGE -> BackupCodec.merge(current, file.data)
            RestoreMode.REPLACE -> file.data
        }
        if (mode == RestoreMode.REPLACE) container.settings.update { result.settings }
        container.academic.replaceAll(result.academic)
        container.tasks.replaceAll(result.tasks)
        container.notes.replaceAll(result.notes)
        container.focusStats.replaceAll(result.focus)
    }

    private fun saveSafetyCopy() {
        val dir = File(context.filesDir, "backups").apply { mkdirs() }
        File(dir, "before-restore-${System.currentTimeMillis()}.json").writeText(exportJson())
        dir.listFiles()?.sortedByDescending { it.name }?.drop(3)?.forEach { it.delete() }
    }

    private fun readLimited(input: InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            out.write(buffer, 0, read)
            if (out.size() > BackupCodec.MAX_BYTES) throw BackupException(BackupError.TOO_LARGE)
        }
        return out.toByteArray()
    }
}
