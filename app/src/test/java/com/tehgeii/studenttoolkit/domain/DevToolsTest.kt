package com.tehgeii.studenttoolkit.domain

import com.tehgeii.studenttoolkit.domain.tools.JsonFormat
import com.tehgeii.studenttoolkit.domain.tools.JsonResult
import com.tehgeii.studenttoolkit.domain.tools.NumberBase
import com.tehgeii.studenttoolkit.domain.tools.RegexResult
import com.tehgeii.studenttoolkit.domain.tools.RegexTester
import com.tehgeii.studenttoolkit.domain.tools.Rgb
import com.tehgeii.studenttoolkit.domain.tools.Timestamps
import java.math.BigInteger
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DevToolsTest {
    @Test
    fun numberBases() {
        assertEquals(BigInteger.valueOf(255), NumberBase.parse("0xff", 16))
        assertEquals(BigInteger.valueOf(5), NumberBase.parse("0b1_01", 2))
        assertEquals(BigInteger.valueOf(-10), NumberBase.parse("-10", 10))
        assertNull(NumberBase.parse("12", 2))
        assertNull(NumberBase.parse("", 10))
        assertEquals("FF", NumberBase.format(BigInteger.valueOf(255), 16))
        assertEquals("-1010", NumberBase.format(BigInteger.valueOf(-10), 2))
        assertEquals("18446744073709551616", NumberBase.parse("1" + "0".repeat(16), 16)!!.toString())
    }

    @Test
    fun timestamps() {
        assertEquals(Instant.ofEpochSecond(1_791_000_000), Timestamps.parseEpoch("1791000000"))
        assertEquals(Instant.ofEpochMilli(1_791_000_000_123), Timestamps.parseEpoch("1791000000123"))
        assertNull(Timestamps.parseEpoch("abc"))
        val jakarta = ZoneId.of("Asia/Jakarta")
        val instant = Timestamps.parseLocal("2026-10-16 14:30", jakarta)!!
        assertEquals("2026-10-16T07:30:00Z", Timestamps.iso(instant))
        assertEquals("2026-10-16 14:30:00", Timestamps.local(instant, jakarta))
        assertNull(Timestamps.parseLocal("16/10/2026", jakarta))
    }

    @Test
    fun jsonPrettyKeepsValuesAndOrder() {
        val result = JsonFormat.pretty("""{"b":1.50,"a":[true,null,"x\n"],"e":{}}""")
        assertEquals(JsonResult.Ok("{\n  \"b\": 1.50,\n  \"a\": [\n    true,\n    null,\n    \"x\\n\"\n  ],\n  \"e\": {}\n}"), result)
        assertEquals(JsonResult.Ok("""{"b":1.50,"a":[1,2]}"""), JsonFormat.minify("{ \"b\" : 1.50 ,\n \"a\" : [ 1 , 2 ] }"))
    }

    @Test
    fun jsonErrorsPointAtTheProblem() {
        val error = JsonFormat.pretty("{\n  \"a\": 1,\n  \"b\" 2\n}") as JsonResult.Error
        assertEquals(3, error.line)
        assertEquals(7, error.column)
        assertTrue(JsonFormat.pretty("[1,]") is JsonResult.Error)
        assertTrue(JsonFormat.pretty("01") is JsonResult.Error)
        assertTrue(JsonFormat.pretty("") is JsonResult.Error)
        assertTrue(JsonFormat.pretty("[".repeat(500)) is JsonResult.Error)
    }

    @Test
    fun colors() {
        val c = Rgb.parseHex("#4F46E5")!!
        assertEquals(Rgb(79, 70, 229), c)
        assertEquals("#4F46E5", c.hex)
        assertEquals("rgb(79, 70, 229)", c.css)
        assertEquals(Triple(243, 75, 59), c.hsl())
        assertEquals(Rgb(255, 255, 255), Rgb.parseHex("fff"))
        assertEquals("rgba(0, 0, 0, 0.5)", Rgb.parseHex("#80000000")!!.css)
        assertNull(Rgb.parseHex("#12345"))
        assertNull(Rgb.parseHex("zzz"))
    }

    @Test
    fun regexTester() {
        val result = RegexTester.run("(\\w+)@(\\w+)", "a@b, c@d", ignoreCase = false, multiline = false) as RegexResult.Matches
        assertEquals(listOf("a@b", "c@d"), result.matches.map { it.value })
        assertEquals(listOf("c", "d"), result.matches[1].groups)
        assertTrue(RegexTester.run("(", "x", false, false) is RegexResult.Invalid)
        val slow = RegexTester.run("(.*,){15}X", "a,".repeat(40), false, false, timeoutMs = 200)
        assertEquals(RegexResult.TimedOut, slow)
    }
}
