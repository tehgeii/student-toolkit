package com.tehgeii.studenttoolkit.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.RemoteViews
import com.tehgeii.studenttoolkit.MainActivity
import com.tehgeii.studenttoolkit.R
import com.tehgeii.studenttoolkit.StudentToolkitApp
import com.tehgeii.studenttoolkit.i18n.dueText
import com.tehgeii.studenttoolkit.ui.Routes
import java.time.ZoneId

/**
 * Home screen widget with the next three deadlines. It is redrawn when tasks change, when the
 * language changes and every 30 minutes (so "today" / "tomorrow" stay right); there is no service.
 */
class DeadlinesWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        render(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        private val rows = listOf(
            Triple(R.id.widget_row1, R.id.widget_row1_title, R.id.widget_row1_due),
            Triple(R.id.widget_row2, R.id.widget_row2_title, R.id.widget_row2_due),
            Triple(R.id.widget_row3, R.id.widget_row3_title, R.id.widget_row3_due),
        )
        private val handler by lazy { Handler(Looper.getMainLooper()) }
        private var pending: Runnable? = null

        /** Redraws placed widgets shortly after a change; many changes in a row cause one redraw. */
        fun requestRefresh(context: Context) {
            val app = context.applicationContext
            pending?.let(handler::removeCallbacks)
            val task = Runnable {
                pending = null
                refresh(app)
            }
            pending = task
            handler.postDelayed(task, 400)
        }

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, DeadlinesWidget::class.java))
            if (ids.isNotEmpty()) render(context, manager, ids)
        }

        private fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val container = StudentToolkitApp.container(context)
            val t = container.lang()
            val now = System.currentTimeMillis()
            val zone = ZoneId.systemDefault()
            val upcoming = container.tasks.tasks.value
                .filter { !it.completed && it.deadline != null }
                .sortedBy { it.deadline }
                .take(rows.size)

            val views = RemoteViews(context.packageName, R.layout.widget_deadlines)
            views.setTextViewText(R.id.widget_title, t("Deadlines", "Deadline"))
            views.setTextViewText(R.id.widget_empty, t("No upcoming deadlines 🎉", "Tidak ada deadline terdekat 🎉"))
            views.setViewVisibility(R.id.widget_empty, if (upcoming.isEmpty()) View.VISIBLE else View.GONE)
            views.setOnClickPendingIntent(android.R.id.background, open(context, Routes.TASKS, 0))
            rows.forEachIndexed { index, (row, title, due) ->
                val task = upcoming.getOrNull(index)
                if (task == null) {
                    views.setViewVisibility(row, View.GONE)
                } else {
                    views.setViewVisibility(row, View.VISIBLE)
                    views.setTextViewText(title, task.title)
                    val details = t.dueText(task.deadline!!, now, zone) + if (task.course.isBlank()) "" else " · ${task.course}"
                    views.setTextViewText(due, details)
                    views.setOnClickPendingIntent(row, open(context, Routes.task(task.id), index + 1))
                }
            }
            manager.updateAppWidget(ids, views)
        }

        private fun open(context: Context, route: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_ROUTE, route)
            }
            return PendingIntent.getActivity(
                context, REQUEST_BASE + requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

        private const val REQUEST_BASE = 7_100
    }
}
