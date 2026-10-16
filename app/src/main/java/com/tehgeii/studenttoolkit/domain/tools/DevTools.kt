package com.tehgeii.studenttoolkit.domain.tools

import java.math.BigInteger
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

// ---- Number bases ---------------------------------------------------------------------------------

object NumberBase {
    const val MAX_DIGITS = 256

    /** Parses [text] in [radix]; accepts a leading "-", spaces/underscores and 0x / 0o / 0b prefixes. */
    fun parse(text: String, radix: Int): BigInteger? {
        var s = text.trim().replace("_", "").replace(" ", "")
        if (s.isEmpty() || s.length > MAX_DIGITS) return null
        val negative = s.startsWith("-")
        if (negative) s = s.drop(1)
        val prefix = when (radix) {
            16 -> "0x"
            8 -> "0o"
            2 -> "0b"
            else -> null
        }
        if (prefix != null && s.lowercase().startsWith(prefix)) s = s.drop(2)
        if (s.isEmpty()) return null
        val value = try {
            BigInteger(s, radix)
        } catch (e: NumberFormatException) {
            return null
        }
        return if (negative) value.negate() else value
    }

    fun format(value: BigInteger, radix: Int): String {
        val text = value.abs().toString(radix).uppercase(Locale.ROOT)
        return if (value.signum() < 0) "-$text" else text
    }
}

// ---- Unix timestamps ------------------------------------------------------------------------------

object Timestamps {
    private val localFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
    private val inputFormats = listOf("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm")
        .map { DateTimeFormatter.ofPattern(it, Locale.ROOT) }

    /** Values with 12 or more digits are treated as milliseconds, shorter ones as seconds. */
    fun parseEpoch(text: String): Instant? {
        val n = text.trim().toLongOrNull() ?: return null
        return try {
            if (kotlin.math.abs(n) >= 100_000_000_000L) Instant.ofEpochMilli(n) else Instant.ofEpochSecond(n)
        } catch (e: Exception) {
            null
        }
    }

    /** "2026-10-16 14:30" (or with seconds / a "T") in [zone]. */
    fun parseLocal(text: String, zone: ZoneId): Instant? {
        val s = text.trim()
        for (format in inputFormats) {
            try {
                return LocalDateTime.parse(s, format).atZone(zone).toInstant()
            } catch (e: DateTimeParseException) {
                // try the next pattern
            }
        }
        return null
    }

    fun local(instant: Instant, zone: ZoneId): String = localFormat.format(instant.atZone(zone))

    fun iso(instant: Instant): String = DateTimeFormatter.ISO_INSTANT.format(instant)
}

// ---- JSON formatter -------------------------------------------------------------------------------

sealed interface JsonResult {
    data class Ok(val text: String) : JsonResult

    /** [line] and [column] are 1-based. */
    data class Error(val message: String, val line: Int, val column: Int) : JsonResult
}

/**
 * Validates and re-indents (or minifies) JSON without changing any value: numbers and strings are
 * copied exactly as written, and key order is kept.
 */
object JsonFormat {
    const val MAX_CHARS = 500_000
    private const val MAX_DEPTH = 200

    fun pretty(text: String, indent: Int = 2): JsonResult = run(text, indent.coerceIn(1, 8))

    fun minify(text: String): JsonResult = run(text, 0)

    private fun run(text: String, indent: Int): JsonResult {
        if (text.length > MAX_CHARS) return JsonResult.Error("Too long (max ${MAX_CHARS / 1000}k characters)", 1, 1)
        val parser = Parser(text, indent)
        return try {
            parser.skipSpace()
            if (parser.atEnd()) throw parser.fail("Empty input")
            parser.value(0)
            parser.skipSpace()
            if (!parser.atEnd()) throw parser.fail("Unexpected text after the end")
            JsonResult.Ok(parser.out.toString())
        } catch (e: JsonFail) {
            val before = text.substring(0, e.pos.coerceAtMost(text.length))
            JsonResult.Error(e.message ?: "Invalid JSON", before.count { it == '\n' } + 1, e.pos - before.lastIndexOf('\n'))
        }
    }

    private class JsonFail(message: String, val pos: Int) : Exception(message)

    private class Parser(val s: String, val indent: Int) {
        var i = 0
        val out = StringBuilder()

        fun atEnd() = i >= s.length
        fun fail(message: String) = JsonFail(message, i)

        fun skipSpace() {
            while (i < s.length && (s[i] == ' ' || s[i] == '\n' || s[i] == '\r' || s[i] == '\t')) i++
        }

        private fun newline(depth: Int) {
            if (indent == 0) return
            out.append('\n')
            repeat(depth * indent) { out.append(' ') }
        }

        fun value(depth: Int) {
            if (depth > MAX_DEPTH) throw fail("Nested too deeply")
            skipSpace()
            if (atEnd()) throw fail("Unexpected end of input")
            when (val c = s[i]) {
                '{' -> container(depth, '}', isObject = true)
                '[' -> container(depth, ']', isObject = false)
                '"' -> string()
                't' -> literal("true")
                'f' -> literal("false")
                'n' -> literal("null")
                else -> if (c == '-' || c in '0'..'9') number() else throw fail("Unexpected character '$c'")
            }
        }

        private fun container(depth: Int, close: Char, isObject: Boolean) {
            out.append(s[i++])
            skipSpace()
            if (i < s.length && s[i] == close) {
                out.append(s[i++])
                return
            }
            while (true) {
                newline(depth + 1)
                skipSpace()
                if (isObject) {
                    if (atEnd() || s[i] != '"') throw fail("Expected a \"key\"")
                    string()
                    skipSpace()
                    if (atEnd() || s[i] != ':') throw fail("Expected ':'")
                    i++
                    out.append(if (indent == 0) ":" else ": ")
                }
                value(depth + 1)
                skipSpace()
                if (atEnd()) throw fail("Missing '$close'")
                when (s[i]) {
                    ',' -> {
                        out.append(',')
                        i++
                    }
                    close -> {
                        newline(depth)
                        out.append(s[i++])
                        return
                    }
                    else -> throw fail("Expected ',' or '$close'")
                }
            }
        }

        private fun string() {
            val start = i
            i++ // opening quote
            while (true) {
                if (atEnd()) throw JsonFail("Unterminated string", start)
                val c = s[i]
                when {
                    c == '"' -> {
                        i++
                        out.append(s, start, i)
                        return
                    }
                    c == '\\' -> {
                        if (i + 1 >= s.length) throw fail("Unterminated string")
                        when (s[i + 1]) {
                            '"', '\\', '/', 'b', 'f', 'n', 'r', 't' -> i += 2
                            'u' -> {
                                if (i + 6 > s.length || !s.substring(i + 2, i + 6).all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
                                    throw fail("Invalid \\u escape")
                                }
                                i += 6
                            }
                            else -> throw fail("Invalid escape '\\${s[i + 1]}'")
                        }
                    }
                    c < ' ' -> throw fail("Control character in string (use \\n)")
                    else -> i++
                }
            }
        }

        private fun number() {
            val start = i
            if (s[i] == '-') i++
            if (atEnd() || s[i] !in '0'..'9') throw fail("Invalid number")
            if (s[i] == '0') i++ else while (i < s.length && s[i] in '0'..'9') i++
            if (i < s.length && s[i] == '.') {
                i++
                if (atEnd() || s[i] !in '0'..'9') throw fail("Invalid number")
                while (i < s.length && s[i] in '0'..'9') i++
            }
            if (i < s.length && (s[i] == 'e' || s[i] == 'E')) {
                i++
                if (i < s.length && (s[i] == '+' || s[i] == '-')) i++
                if (atEnd() || s[i] !in '0'..'9') throw fail("Invalid number")
                while (i < s.length && s[i] in '0'..'9') i++
            }
            out.append(s, start, i)
        }

        private fun literal(word: String) {
            if (!s.startsWith(word, i)) throw fail("Unexpected word")
            out.append(word)
            i += word.length
        }
    }
}

// ---- Colors ---------------------------------------------------------------------------------------

data class Rgb(val r: Int, val g: Int, val b: Int, val alpha: Int = 255) {
    val hex: String get() = if (alpha == 255) String.format(Locale.ROOT, "#%02X%02X%02X", r, g, b)
    else String.format(Locale.ROOT, "#%02X%02X%02X%02X", alpha, r, g, b)

    val css: String get() = if (alpha == 255) "rgb($r, $g, $b)"
    else "rgba($r, $g, $b, ${String.format(Locale.ROOT, "%.2f", alpha / 255.0).trimEnd('0').trimEnd('.')})"

    /** Hue 0–360, saturation and lightness 0–100. */
    fun hsl(): Triple<Int, Int, Int> {
        val rf = r / 255.0
        val gf = g / 255.0
        val bf = b / 255.0
        val max = maxOf(rf, gf, bf)
        val min = minOf(rf, gf, bf)
        val l = (max + min) / 2
        val d = max - min
        if (d == 0.0) return Triple(0, 0, Math.round(l * 100).toInt())
        val s = d / (1 - kotlin.math.abs(2 * l - 1))
        val h = when (max) {
            rf -> 60 * (((gf - bf) / d).mod(6.0))
            gf -> 60 * ((bf - rf) / d + 2)
            else -> 60 * ((rf - gf) / d + 4)
        }
        return Triple(Math.round(h).toInt() % 360, Math.round(s * 100).toInt(), Math.round(l * 100).toInt())
    }

    /** Packed 0xAARRGGBB, e.g. for drawing a preview swatch. */
    val argb: Long get() = (alpha.toLong() shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()

    companion object {
        private val hexChars = Regex("^[0-9a-fA-F]+$")

        /** "#RGB", "#RRGGBB" or "#AARRGGBB" (the # is optional). */
        fun parseHex(text: String): Rgb? {
            val s = text.trim().removePrefix("#")
            if (!hexChars.matches(s)) return null
            return when (s.length) {
                3 -> Rgb(dup(s[0]), dup(s[1]), dup(s[2]))
                6 -> Rgb(s.substring(0, 2).toInt(16), s.substring(2, 4).toInt(16), s.substring(4, 6).toInt(16))
                8 -> Rgb(s.substring(2, 4).toInt(16), s.substring(4, 6).toInt(16), s.substring(6, 8).toInt(16), s.substring(0, 2).toInt(16))
                else -> null
            }
        }

        private fun dup(c: Char): Int = "$c$c".toInt(16)
    }
}

// ---- Regex tester ---------------------------------------------------------------------------------

data class RegexMatch(val start: Int, val end: Int, val value: String, val groups: List<String?>)

sealed interface RegexResult {
    data class Invalid(val message: String) : RegexResult
    data class Matches(val matches: List<RegexMatch>, val truncated: Boolean) : RegexResult
    data object TimedOut : RegexResult
}

/**
 * Runs a user's pattern safely: input and match count are capped, and a pattern that backtracks
 * for too long is stopped instead of freezing the app.
 */
object RegexTester {
    const val MAX_TEXT = 20_000
    const val MAX_MATCHES = 200

    fun run(pattern: String, text: String, ignoreCase: Boolean, multiline: Boolean, timeoutMs: Long = 500): RegexResult {
        if (pattern.isEmpty()) return RegexResult.Matches(emptyList(), false)
        val options = buildSet {
            if (ignoreCase) add(RegexOption.IGNORE_CASE)
            if (multiline) add(RegexOption.MULTILINE)
        }
        val regex = try {
            Regex(pattern, options)
        } catch (e: Exception) {
            return RegexResult.Invalid(e.message?.lineSequence()?.firstOrNull() ?: "Invalid pattern")
        }
        val input = DeadlineCharSequence(text.take(MAX_TEXT), System.nanoTime() + timeoutMs * 1_000_000)
        return try {
            val found = ArrayList<RegexMatch>()
            var truncated = false
            for (m in regex.findAll(input)) {
                if (found.size == MAX_MATCHES) {
                    truncated = true
                    break
                }
                found += RegexMatch(m.range.first, m.range.last + 1, m.value, m.groupValues.drop(1).mapIndexed { i, g -> g.takeIf { m.groups[i + 1] != null } })
            }
            RegexResult.Matches(found, truncated)
        } catch (e: TimeUp) {
            RegexResult.TimedOut
        }
    }

    private class TimeUp : RuntimeException()

    /** Checks the clock while the regex engine reads characters, so runaway backtracking ends. */
    private class DeadlineCharSequence(private val inner: CharSequence, private val deadline: Long) : CharSequence {
        private var reads = 0
        override val length: Int get() = inner.length
        override fun get(index: Int): Char {
            if (++reads and 0x3FF == 0 && System.nanoTime() > deadline) throw TimeUp()
            return inner[index]
        }
        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
            DeadlineCharSequence(inner.subSequence(startIndex, endIndex), deadline)
        override fun toString(): String = inner.toString()
    }
}
