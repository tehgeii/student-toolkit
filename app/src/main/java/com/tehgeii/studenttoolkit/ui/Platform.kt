package com.tehgeii.studenttoolkit.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.FileProvider
import com.tehgeii.studenttoolkit.AppContainer
import com.tehgeii.studenttoolkit.StudentToolkitApp
import com.tehgeii.studenttoolkit.i18n.Lang
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

val LocalContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }
val LocalMessenger = staticCompositionLocalOf<Messenger> { error("Messenger not provided") }

/** UI language. Use as `val t = LocalLang.current` then `t("English", "Indonesia")`. */
val LocalLang = staticCompositionLocalOf { Lang.EN }

/** App-wide snackbars. Lives above the screens so an Undo survives navigating back. */
class Messenger(private val host: SnackbarHostState, private val scope: CoroutineScope) {
    fun show(message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
        scope.launch {
            host.currentSnackbarData?.dismiss()
            val result = host.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) onAction()
        }
    }
}

/** Android share sheet, clipboard and browser helpers. Nothing depends on a specific app. */
object Platform {
    fun shareText(context: Context, text: String, title: String = "Share") {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        startSafely(context, Intent.createChooser(send, title))
    }

    fun shareFile(context: Context, file: File, mimeType: String, title: String = "Share") {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType(mimeType)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(file.name, uri)
        startSafely(context, Intent.createChooser(send, title))
    }

    /** [sensitive] hides the text from the clipboard preview and keyboard suggestions (e.g. Wi-Fi passwords). */
    fun copy(context: Context, label: String, text: String, sensitive: Boolean = false) {
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
        val clip = ClipData.newPlainText(label, text)
        if (sensitive) {
            val key = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ClipDescription.EXTRA_IS_SENSITIVE
            } else {
                "android.content.extra.IS_SENSITIVE"
            }
            clip.description.extras = PersistableBundle().apply { putBoolean(key, true) }
        }
        clipboard.setPrimaryClip(clip)
    }

    fun openUrl(context: Context, url: String) {
        startSafely(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    fun startSafely(context: Context, intent: Intent) {
        try {
            if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            val t = StudentToolkitApp.container(context).lang()
            Toast.makeText(context, t("No app can handle this action.", "Tidak ada aplikasi yang bisa membuka ini."), Toast.LENGTH_SHORT).show()
        }
    }
}
