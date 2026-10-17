package com.hrshd1eux.attendly

import com.hrshd1eux.attendly.core.model.DayOfWeek
import com.hrshd1eux.attendly.domain.parser.TimetableTextParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetableTextParserTest {

    @Test
    fun testParseStandardAiPromptFormat() {
        val input = """
            TIMETABLE
            FORMAT_VERSION: 1

            DAY: MONDAY
            09:00-10:00 | Mathematics | Room 101
            10:00-11:00 | Physics | Room 204
            11:00-13:00 | Chemistry Laboratory | Lab 2

            DAY: TUESDAY
            09:00-10:00 | English | Room 105
            10:00-12:00 | Computer Laboratory | Lab 1
        """.trimIndent()

        val result = TimetableTextParser.parse(input)
        assertEquals(5, result.entries.size)
        assertEquals(0, result.unrecognizedLines.size)
        assertEquals(0, result.warnings.size)

        val monFirst = result.entries[0]
        assertEquals(DayOfWeek.MONDAY, monFirst.dayOfWeek)
        assertEquals(540, monFirst.startMinute) // 09:00
        assertEquals(600, monFirst.endMinute)   // 10:00
        assertEquals("Mathematics", monFirst.subjectName)
        assertEquals("Room 101", monFirst.location)

        val chemLab = result.entries[2]
        assertEquals(DayOfWeek.MONDAY, chemLab.dayOfWeek)
        assertEquals(660, chemLab.startMinute)  // 11:00
        assertEquals(780, chemLab.endMinute)    // 13:00 (2-hour lab)
        assertEquals("Chemistry Laboratory", chemLab.subjectName)
        assertEquals("Lab 2", chemLab.location)
    }

    @Test
    fun testParseDetectsOverlapConflict() {
        val input = """
            DAY: WEDNESDAY
            09:00-11:00 | Mechanics | Room 1
            10:00-12:00 | Thermodynamics | Room 2
        """.trimIndent()

        val result = TimetableTextParser.parse(input)
        assertEquals(2, result.entries.size)
        assertTrue(result.warnings.isNotEmpty())
        assertTrue(result.entries.any { it.hasConflict })
    }

    @Test
    fun testParseHandlesUncertaintyComments() {
        val input = """
            DAY: THURSDAY
            09:00-10:00 | Calculus | Room 5
            COMMENT: UNCERTAIN - lab session might be shifted to Friday
        """.trimIndent()

        val result = TimetableTextParser.parse(input)
        assertEquals(1, result.entries.size)
        assertEquals(1, result.comments.size)
        assertTrue(result.comments[0].contains("UNCERTAIN"))
    }
}
