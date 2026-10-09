package com.tehgeii.studenttoolkit.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.tehgeii.studenttoolkit.notify.Alarms
import com.tehgeii.studenttoolkit.notify.Notifier
import com.tehgeii.studenttoolkit.ui.Platform

object PermissionIntents {
    fun notificationSettings(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    fun exactAlarmSettings(context: Context): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
        } else {
            null
        }
}

/** Live notification permission state, re-checked whenever the screen resumes. */
@Composable
fun rememberNotificationsAllowed(): Pair<Boolean, () -> Unit> {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(Notifier.canPost(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { allowed = Notifier.canPost(context) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        allowed = Notifier.canPost(context)
        if (!granted) Platform.startSafely(context, PermissionIntents.notificationSettings(context))
    }
    val request: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notifier.hasPermission(context)) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            Platform.startSafely(context, PermissionIntents.notificationSettings(context))
        }
    }
    return allowed to request
}

/** Shown only when notifications are needed for a feature but currently blocked. */
@Composable
fun NotificationPermissionCard(reason: String) {
    val (allowed, request) = rememberNotificationsAllowed()
    if (allowed) return
    PermissionCard(text = "Notifications are off. $reason", action = "Allow notifications", onAction = request)
}

/** Android 12+ may delay alarms unless the user allows precise ones; explained instead of assumed. */
@Composable
fun ExactAlarmCard() {
    val context = LocalContext.current
    var exact by remember { mutableStateOf(Alarms.canScheduleExact(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { exact = Alarms.canScheduleExact(context) }
    val intent = PermissionIntents.exactAlarmSettings(context)
    if (exact || intent == null) return
    PermissionCard(
        text = "Precise alarms are off, so the end alert may arrive a few minutes late. The timer itself stays accurate.",
        action = "Allow precise alarms",
        onAction = { Platform.startSafely(context, intent) },
    )
}

@Composable
private fun PermissionCard(text: String, action: String, onAction: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("🔔 $text", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}
