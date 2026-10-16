package com.tehgeii.studenttoolkit.notify

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.tehgeii.studenttoolkit.R
import com.tehgeii.studenttoolkit.StudentToolkitApp
import com.tehgeii.studenttoolkit.core.Fmt
import com.tehgeii.studenttoolkit.domain.focus.FocusPhase

/**
 * Quick Settings tile: tap to start, pause or resume the Pomodoro without opening the app.
 * The tile only refreshes while the panel is open, so it shows the end time instead of a ticking clock.
 */
class FocusTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        StudentToolkitApp.container(this).focus.checkFinished()
        render()
    }

    override fun onClick() {
        super.onClick()
        val focus = StudentToolkitApp.container(this).focus
        if (focus.pomodoro.value.countdown.isRunning) focus.pausePomodoro() else focus.startPomodoro()
        render()
    }

    private fun render() {
        val tile = qsTile ?: return
        val container = StudentToolkitApp.container(this)
        val t = container.lang()
        val state = container.focus.pomodoro.value
        val countdown = state.countdown
        val remaining = countdown.remaining(container.focus.now())
        val phase = if (state.phase == FocusPhase.FOCUS) t("Focus", "Fokus") else t("Break", "Istirahat")
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_focus)
        tile.label = "Pomodoro"
        tile.state = if (countdown.isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        val subtitle = when {
            countdown.isRunning -> {
                val endsAt = System.currentTimeMillis() + remaining
                "$phase · " + t("until ", "sampai ") + Fmt.time(endsAt)
            }
            countdown.isFresh -> "$phase ${state.durationOf(state.phase) / 60_000} " + t("min", "mnt")
            else -> t("Paused", "Jeda") + " · " + Fmt.countdown(remaining)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = subtitle
        } else {
            tile.label = "Pomodoro · $subtitle"
        }
        tile.contentDescription = "Pomodoro, $subtitle"
        tile.updateTile()
    }
}
