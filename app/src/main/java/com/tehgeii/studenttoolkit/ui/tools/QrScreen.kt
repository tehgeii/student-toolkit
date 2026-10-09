@file:OptIn(ExperimentalMaterial3Api::class)

package com.tehgeii.studenttoolkit.ui.tools

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tehgeii.studenttoolkit.domain.tools.QrPayload
import com.tehgeii.studenttoolkit.domain.tools.QrPayloadResult
import com.tehgeii.studenttoolkit.domain.tools.QrType
import com.tehgeii.studenttoolkit.domain.tools.WifiSecurity
import com.tehgeii.studenttoolkit.ui.LocalMessenger
import com.tehgeii.studenttoolkit.ui.Platform
import com.tehgeii.studenttoolkit.ui.components.AppIcons
import com.tehgeii.studenttoolkit.ui.components.AppScaffold
import com.tehgeii.studenttoolkit.ui.components.ChoiceChips
import com.tehgeii.studenttoolkit.ui.components.DropdownField
import com.tehgeii.studenttoolkit.ui.components.FormColumn
import com.tehgeii.studenttoolkit.ui.components.InfoCard
import com.tehgeii.studenttoolkit.ui.components.TextInput
import io.nayuki.qrcodegen.DataTooLongException
import io.nayuki.qrcodegen.QrCode
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Renders QR codes on-device with a white quiet zone so they scan in dark mode too. */
object QrRenderer {
    private const val BORDER = 4

    fun render(text: String, targetPx: Int = 1024): Bitmap? {
        val qr = encode(text) ?: return null
        val modules = qr.size + BORDER * 2
        val scale = (targetPx / modules).coerceAtLeast(1)
        val sizePx = modules * scale
        val pixels = IntArray(sizePx * sizePx) { WHITE }
        for (y in 0 until qr.size) {
            for (x in 0 until qr.size) {
                if (!qr.getModule(x, y)) continue
                val left = (x + BORDER) * scale
                val top = (y + BORDER) * scale
                for (dy in 0 until scale) {
                    val row = (top + dy) * sizePx
                    for (dx in 0 until scale) pixels[row + left + dx] = BLACK
                }
            }
        }
        return Bitmap.createBitmap(pixels, sizePx, sizePx, Bitmap.Config.ARGB_8888)
    }

    private fun encode(text: String): QrCode? = try {
        QrCode.encodeText(text, QrCode.Ecc.MEDIUM)
    } catch (e: DataTooLongException) {
        try {
            QrCode.encodeText(text, QrCode.Ecc.LOW)
        } catch (e: DataTooLongException) {
            null
        }
    }

    private const val WHITE = -0x1
    private const val BLACK = -0x1000000
}

@Composable
fun QrScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val messenger = LocalMessenger.current
    val scope = rememberCoroutineScope()

    var type by rememberSaveable { mutableStateOf(QrType.TEXT) }
    var text by rememberSaveable { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("") }
    var ssid by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var security by rememberSaveable { mutableStateOf(WifiSecurity.WPA) }
    var hidden by rememberSaveable { mutableStateOf(false) }
    var emailTo by rememberSaveable { mutableStateOf("") }
    var subject by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }

    val result = when (type) {
        QrType.TEXT -> QrPayload.text(text)
        QrType.URL -> QrPayload.url(url)
        QrType.WIFI -> QrPayload.wifi(ssid, password, security, hidden)
        QrType.EMAIL -> QrPayload.email(emailTo, subject, body)
        QrType.PHONE -> QrPayload.phone(phone)
    }
    val payload = (result as? QrPayloadResult.Ok)?.payload
    // Rendering runs off the main thread; the pair remembers which payload the bitmap belongs to.
    val rendered by produceState<Pair<String, Bitmap?>?>(initialValue = null, payload) {
        value = if (payload == null) null else payload to withContext(Dispatchers.Default) { QrRenderer.render(payload) }
    }
    val current = rendered?.takeIf { it.first == payload }
    val bitmap = current?.second

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        val image = bitmap
        if (uri != null && image != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { image.compress(Bitmap.CompressFormat.PNG, 100, it) } == true
                    }.getOrDefault(false)
                }
                messenger.show(if (ok) "✓ QR image saved" else "Could not save the image. Nothing was changed.")
            }
        }
    }

    AppScaffold(title = "QR generator", onBack = onBack) { padding ->
        FormColumn(padding) {
            ChoiceChips(QrType.entries.toList(), type, { it.label }, { type = it })
            when (type) {
                QrType.TEXT -> TextInput(text, { text = it }, "Text", singleLine = false, minLines = 3)
                QrType.URL -> TextInput(
                    url, { url = it }, "URL", placeholder = "example.com",
                    capitalization = KeyboardCapitalization.None, keyboardType = KeyboardType.Uri,
                )
                QrType.WIFI -> {
                    TextInput(ssid, { ssid = it }, "Network name (SSID)", capitalization = KeyboardCapitalization.None)
                    DropdownField("Security", WifiSecurity.entries.toList(), security, { it.label }, { security = it }, Modifier.fillMaxWidth())
                    if (security != WifiSecurity.NONE) {
                        TextInput(
                            password, { password = it }, "Password",
                            capitalization = KeyboardCapitalization.None, keyboardType = KeyboardType.Password,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = hidden, onCheckedChange = { hidden = it })
                        Text("Hidden network")
                    }
                    InfoCard("This QR code contains your Wi-Fi password. Share carefully.", emoji = "🔐")
                }
                QrType.EMAIL -> {
                    TextInput(
                        emailTo, { emailTo = it }, "To",
                        capitalization = KeyboardCapitalization.None, keyboardType = KeyboardType.Email,
                    )
                    TextInput(subject, { subject = it }, "Subject (optional)")
                    TextInput(body, { body = it }, "Message (optional)", singleLine = false, minLines = 2)
                }
                QrType.PHONE -> TextInput(
                    phone, { phone = it }, "Phone number", placeholder = "+62 812 3456 7890",
                    keyboardType = KeyboardType.Phone,
                )
            }

            when (result) {
                is QrPayloadResult.Error -> Text(result.message, color = MaterialTheme.colorScheme.error)
                QrPayloadResult.Empty -> Text(
                    "Fill in the fields to create a QR code. Everything is generated on your phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                is QrPayloadResult.Ok -> Unit
            }

            val image = bitmap
            if (payload != null && image != null) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Image(
                        bitmap = image.asImageBitmap(),
                        contentDescription = "QR code preview",
                        filterQuality = FilterQuality.None,
                        modifier = Modifier
                            .size(260.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { saveLauncher.launch("qr-code.png") }, modifier = Modifier.weight(1f)) {
                        Icon(AppIcons.Download, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Save")
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val file = withContext(Dispatchers.IO) {
                                    runCatching {
                                        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
                                        File(dir, "qr-code.png").also { f -> f.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) } }
                                    }.getOrNull()
                                }
                                if (file != null) Platform.shareFile(context, file, "image/png", "Share QR code")
                                else messenger.show("Could not prepare the image.")
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Share")
                    }
                    OutlinedButton(
                        onClick = {
                            Platform.copy(context, "QR content", payload)
                            messenger.show("✓ Copied")
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(AppIcons.Copy, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Copy")
                    }
                }
            } else if (current != null) {
                Text("This content is too long for a QR code.", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
