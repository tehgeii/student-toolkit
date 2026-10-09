package com.tehgeii.studenttoolkit

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tehgeii.studenttoolkit.ui.AppRoot
import com.tehgeii.studenttoolkit.ui.theme.StudentToolkitTheme

class MainActivity : ComponentActivity() {
    private val deepLink = mutableStateOf<String?>(null)
    private val container by lazy { StudentToolkitApp.container(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) deepLink.value = intent?.getStringExtra(EXTRA_ROUTE)
        setContent {
            val settings by container.settings.state.collectAsStateWithLifecycle()
            StudentToolkitTheme(settings.theme, settings.dynamicColor) {
                AppRoot(container, deepLink.value, onDeepLinkHandled = { deepLink.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_ROUTE)?.let { deepLink.value = it }
    }

    override fun onStart() {
        super.onStart()
        // Completes a timer that ended while the app was away (inexact alarms can be late).
        container.focus.checkFinished()
    }

    override fun onStop() {
        super.onStop()
        container.flush()
    }

    companion object {
        const val EXTRA_ROUTE = "route"
    }
}
