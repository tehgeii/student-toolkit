package com.tehgeii.studenttoolkit.domain

import com.tehgeii.studenttoolkit.domain.focus.Countdown
import com.tehgeii.studenttoolkit.domain.focus.FocusPhase
import com.tehgeii.studenttoolkit.domain.focus.PomodoroState
import com.tehgeii.studenttoolkit.domain.focus.Stopwatch
import com.tehgeii.studenttoolkit.domain.tools.Percentage
import com.tehgeii.studenttoolkit.domain.tools.QrPayload
import com.tehgeii.studenttoolkit.domain.tools.QrPayloadResult
import com.tehgeii.studenttoolkit.domain.tools.UnitCategory
import com.tehgeii.studenttoolkit.domain.tools.UnitConverter
import com.tehgeii.studenttoolkit.domain.tools.WifiSecurity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolsTest {
    private fun unit(category: UnitCategory, symbol: String) = category.units.first { it.symbol == symbol }

    @Test
    fun storageUsesCorrectPrefixes() {
        val s = UnitCategory.STORAGE
        assertEquals(1000.0, UnitConverter.convert(1.0, s, unit(s, "GB"), unit(s, "MB")), 1e-9)
        assertEquals(1024.0, UnitConverter.convert(1.0, s, unit(s, "GiB"), unit(s, "MiB")), 1e-9)
        assertEquals(8.0, UnitConverter.convert(1.0, s, unit(s, "B"), unit(s, "bit")), 1e-9)
    }

    @Test
    fun temperature() {
        val t = UnitCategory.TEMPERATURE
        assertEquals(212.0, UnitConverter.convert(100.0, t, unit(t, "°C"), unit(t, "°F")), 1e-9)
        assertEquals(273.15, UnitConverter.convert(0.0, t, unit(t, "°C"), unit(t, "K")), 1e-9)
        assertEquals(0.0, UnitConverter.convert(32.0, t, unit(t, "°F"), unit(t, "°C")), 1e-9)
        assertFalse(UnitConverter.isValid(-300.0, t, unit(t, "°C")))
    }

    @Test
    fun lengthAndSpeed() {
        val l = UnitCategory.LENGTH
        assertEquals(1.609344, UnitConverter.convert(1.0, l, unit(l, "mi"), unit(l, "km")), 1e-9)
        val v = UnitCategory.SPEED
        assertEquals(36.0, UnitConverter.convert(10.0, v, unit(v, "m/s"), unit(v, "km/h")), 1e-9)
    }

    @Test
    fun percentages() {
        assertEquals(30.0, Percentage.percentOf(20.0, 150.0), 1e-9)
        assertEquals(78.0, Percentage.whatPercent(78.0, 100.0)!!, 1e-9)
        assertNull(Percentage.whatPercent(1.0, 0.0))
        assertEquals(-25.0, Percentage.change(80.0, 60.0)!!, 1e-9)
        assertEquals(120.0 to 30.0, Percentage.discount(150.0, 20.0))
        assertEquals(110.0, Percentage.increase(100.0, 10.0), 1e-9)
        assertEquals(90.0, Percentage.decrease(100.0, 10.0), 1e-9)
    }

    @Test
    fun wifiPayloadEscapesSpecialCharacters() {
        val result = QrPayload.wifi("Kos;Wi:Fi", "pa\\ss,word\"", WifiSecurity.WPA, hidden = true)
        assertEquals(QrPayloadResult.Ok("WIFI:T:WPA;S:Kos\\;Wi\\:Fi;P:pa\\\\ss\\,word\\\";H:true;;"), result)
        assertEquals(QrPayloadResult.Ok("WIFI:T:nopass;S:Open;;"), QrPayload.wifi("Open", "", WifiSecurity.NONE, false))
        assertTrue(QrPayload.wifi("Net", "short", WifiSecurity.WPA, false) is QrPayloadResult.Error)
    }

    @Test
    fun otherPayloads() {
        assertEquals(QrPayloadResult.Ok("https://example.com/a"), QrPayload.url("example.com/a"))
        assertTrue(QrPayload.url("not a url") is QrPayloadResult.Error)
        assertEquals(QrPayloadResult.Ok("tel:+6281234567890"), QrPayload.phone("+62 812-3456-7890"))
        assertEquals(
            QrPayloadResult.Ok("mailto:dosen@kampus.ac.id?subject=Tugas%201"),
            QrPayload.email("dosen@kampus.ac.id", "Tugas 1", ""),
        )
        assertEquals(QrPayloadResult.Empty, QrPayload.text("   "))
        assertTrue(QrPayload.text("x".repeat(QrPayload.MAX_BYTES + 1)) is QrPayloadResult.Error)
    }

    @Test
    fun countdownUsesElapsedTime() {
        var c = Countdown(60_000)
        c = c.start(1_000)
        assertEquals(30_000, c.remaining(31_000))
        c = c.pause(31_000)
        assertEquals(30_000, c.remaining(999_999))
        c = c.start(100_000)
        assertTrue(c.isFinished(130_000))
        assertEquals(0, c.remaining(500_000))
        assertTrue(c.reset().isFresh)
    }

    @Test
    fun pomodoroCycle() {
        var p = PomodoroState()
        assertEquals(FocusPhase.FOCUS, p.phase)
        p = p.advance()
        assertEquals(FocusPhase.BREAK, p.phase)
        assertEquals(5 * 60_000L, p.countdown.durationMs)
        repeat(3) { p = p.advance().advance() }
        assertEquals(4, p.session)
        assertEquals(FocusPhase.BREAK, p.phase)
        p = p.advance()
        assertEquals(1, p.session)
        assertEquals(FocusPhase.FOCUS, p.phase)

        val custom = p.withDurations(50, 10)
        assertEquals(50 * 60_000L, custom.countdown.durationMs)
        val started = custom.copy(countdown = custom.countdown.start(0))
        assertTrue(started.reset().countdown.isFresh)
    }

    @Test
    fun stopwatchLaps() {
        var s = Stopwatch().start(1_000)
        s = s.lap(3_000)
        s = s.pause(5_000)
        assertEquals(4_000, s.elapsed(99_000))
        s = s.start(10_000)
        assertEquals(6_000, s.elapsed(12_000))
        assertEquals(listOf(2_000L), s.laps)
    }
}
