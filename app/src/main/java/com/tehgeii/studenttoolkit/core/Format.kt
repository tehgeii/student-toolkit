package com.tehgeii.studenttoolkit.core

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Display formatting. Rounding happens here only, never inside calculations.
 */
object Fmt {
    private val dayFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)
    private val longDayFormat = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)
    private val shortDayFormat = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

    /** Rounds half-up to [decimals] places and trims trailing zeros (84.50 -> 84.5). */
    fun number(value: Double, decimals: Int = 2): String {
        if (value.isNaN() || value.isInfinite()) return "–"
        return BigDecimal.valueOf(value)
            .setScale(decimals, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
    }

    /** Rounds half-up and always shows [decimals] places (3.5 -> 3.50). */
    fun fixed(value: Double, decimals: Int): String {
        if (value.isNaN() || value.isInfinite()) return "–"
        return BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).toPlainString()
    }

    /** 210 -> "3h 30m", 45 -> "45m", 120 -> "2h". */
    fun duration(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h == 0 -> "${m}m"
            m == 0 -> "${h}h"
            else -> "${h}h ${m}m"
        }
    }

    /** Countdown text: "24:32" or "1:05:09". Rounds up so a timer never shows 00:00 while running. */
    fun countdown(ms: Long): String {
        val totalSeconds = (ms + 999) / 1000
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%02d:%02d", m, s)
    }

    /** Stopwatch text with tenths: "01:02.3" or "1:01:02.3". */
    fun stopwatch(ms: Long): String {
        val tenths = (ms / 100) % 10
        val totalSeconds = ms / 1000
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d.%d", h, m, s, tenths)
        else String.format(Locale.US, "%02d:%02d.%d", m, s, tenths)
    }

    fun date(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        dayFormat.format(Instant.ofEpochMilli(epochMillis).atZone(zone))

    fun longDate(date: LocalDate): String = longDayFormat.format(date)

    fun shortDate(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        shortDayFormat.format(Instant.ofEpochMilli(epochMillis).atZone(zone))

    fun time(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        timeFormat.format(Instant.ofEpochMilli(epochMillis).atZone(zone))

    fun dateTime(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        "${date(epochMillis, zone)} · ${time(epochMillis, zone)}"
}

/** Parses user input, accepting both "3.5" and "3,5". Returns null for blank or invalid input. */
fun parseNumber(text: String): Double? =
    text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

/** Keeps only characters that can form a decimal number, so the soft keyboard cannot insert junk. */
fun sanitizeDecimal(text: String, maxLength: Int = 12): String {
    val out = StringBuilder()
    var seenSeparator = false
    for (c in text) {
        when {
            c.isDigit() -> out.append(c)
            (c == '.' || c == ',') && !seenSeparator -> { out.append(c); seenSeparator = true }
            c == '-' && out.isEmpty() -> out.append(c)
        }
        if (out.length >= maxLength) break
    }
    return out.toString()
}

fun sanitizeInt(text: String, maxLength: Int = 6): String =
    text.filter { it.isDigit() }.take(maxLength)

fun newId(): String = java.util.UUID.randomUUID().toString()
