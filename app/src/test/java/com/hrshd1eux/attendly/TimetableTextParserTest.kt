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

    @Test
    fun testParseUserConcatenatedTimetableWithInlineComments() {
        val input = """
            TIMETABLE FORMAT_VERSION: 1

            DAY: MONDAY 09:00-10:00 | OOPS | COMMENT: UNCERTAIN - Room/location not specified. 10:00-11:00 | DSA | COMMENT: UNCERTAIN - Room/location not specified. 11:00-12:00 | DM | COMMENT: UNCERTAIN - Room/location not specified. 14:00-16:00 | DSA LAB G-1 | LAB-212 16:00-18:00 | OOPS LAB G-3 | LAB-307 COMMENT: UNCERTAIN - The lab timetable spans 14:00-18:00, with DSA LAB G-1 and OOPS LAB G-3 shown as separate two-hour group sessions; verify exact group/session boundaries.

            DAY: TUESDAY 09:00-10:00 | OOPS | COMMENT: UNCERTAIN - Room/location not specified. 10:00-11:00 | DSA | COMMENT: UNCERTAIN - Room/location not specified. 11:00-12:00 | COA | COMMENT: UNCERTAIN - Room/location not specified. 12:00-13:00 | Math-III | COMMENT: UNCERTAIN - Room/location not specified. 14:00-16:00 | DSA LAB G-2 | LAB-212 16:00-18:00 | OOPS LAB G-1 | LAB-307 14:00-18:00 | HVPD | LH-3, TB-7 COMMENT: UNCERTAIN - HVPD is shown in the 16:00-17:00 column, but the printed header labels that column 16:00-17:00; verify the intended time. The displayed cell appears to occupy one hour, not four hours.

            DAY: WEDNESDAY 09:00-10:00 | OOPS | COMMENT: UNCERTAIN - Room/location not specified. 10:00-11:00 | DSA | COMMENT: UNCERTAIN - Room/location not specified. 11:00-12:00 | DSA LAB G-1 | LAB-115 12:00-13:00 | OOPS LAB G-3 | LAB-307 14:00-16:00 | DSA LAB G-3 | LAB-212 16:00-18:00 | OOPS LAB G-2 | LAB-307

            DAY: THURSDAY 09:00-10:00 | Math-III | COMMENT: UNCERTAIN - Room/location not specified. 10:00-11:00 | DM | COMMENT: UNCERTAIN - Room/location not specified. 11:00-12:00 | DSA LAB G-2 | LAB-106 12:00-13:00 | OOPS LAB G-1 | LAB-307 14:00-15:00 | COA | COMMENT: UNCERTAIN - Guest instructor Pallavi; room/location not specified. 15:00-16:00 | HVPD | COMMENT: UNCERTAIN - Instructor Dr. Navdeep Mor; room/location not specified.

            DAY: FRIDAY 09:00-10:00 | Math-III | COMMENT: UNCERTAIN - Room/location not specified. 10:00-11:00 | DM | COMMENT: UNCERTAIN - Room/location not specified. 11:00-12:00 | DSA LAB G-3 | LAB-106 12:00-13:00 | OOPS LAB G-2 | LAB-307 14:00-15:00 | COA | COMMENT: UNCERTAIN - Guest instructor Pallavi; room/location not specified. 15:00-16:00 | HVPD | COMMENT: UNCERTAIN - Instructor Dr. Navdeep Mor; room/location not specified.
        """.trimIndent()

        val result = TimetableTextParser.parse(input)
        assertEquals(0, result.unrecognizedLines.size)
        assertEquals(30, result.entries.size)

        // Verify Monday sessions
        val mondayEntries = result.entries.filter { it.dayOfWeek == DayOfWeek.MONDAY }
        assertEquals(5, mondayEntries.size)
        assertEquals("OOPS", mondayEntries[0].subjectName)
        assertEquals("DSA", mondayEntries[1].subjectName)
        assertEquals("DM", mondayEntries[2].subjectName)
        assertEquals("DSA LAB G-1", mondayEntries[3].subjectName)
        assertEquals("LAB-212", mondayEntries[3].location)
        assertEquals("OOPS LAB G-3", mondayEntries[4].subjectName)
        assertEquals("LAB-307", mondayEntries[4].location)

        // Verify Tuesday conflict detection for 14:00-18:00 HVPD
        val tuesdayEntries = result.entries.filter { it.dayOfWeek == DayOfWeek.TUESDAY }
        assertEquals(7, tuesdayEntries.size)
        assertTrue(tuesdayEntries.any { it.hasConflict })
    }
}
