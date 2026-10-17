package com.hrshd1eux.attendly

import com.hrshd1eux.attendly.core.model.DayOfWeek
import com.hrshd1eux.attendly.domain.parser.TimetableGroupDetector
import com.hrshd1eux.attendly.domain.parser.TimetableTextParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        assertFalse(result.hasMultipleGroups)

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
    fun testGroupDetectorExtractionAndCleaning() {
        assertEquals("G1", TimetableGroupDetector.extractGroup("DSA LAB G-1"))
        assertEquals("G2", TimetableGroupDetector.extractGroup("DSA LAB G-2"))
        assertEquals("G3", TimetableGroupDetector.extractGroup("OOPS LAB G-3"))
        assertEquals("G1", TimetableGroupDetector.extractGroup("DSA LAB G1"))
        assertEquals("G2", TimetableGroupDetector.extractGroup("DSA LAB (G-2)"))
        assertEquals("G1", TimetableGroupDetector.extractGroup("Chemistry Lab Batch-1"))
        assertEquals("G2", TimetableGroupDetector.extractGroup("Physics Lab Group 2"))
        assertNull(TimetableGroupDetector.extractGroup("Mathematics"))
        assertNull(TimetableGroupDetector.extractGroup("OOPS"))
        assertNull(TimetableGroupDetector.extractGroup("HVPD"))
        assertNull(TimetableGroupDetector.extractGroup("Math-III"))

        assertEquals("DSA LAB", TimetableGroupDetector.cleanSubjectName("DSA LAB G-1"))
        assertEquals("OOPS LAB", TimetableGroupDetector.cleanSubjectName("OOPS LAB G-3"))
        assertEquals("DSA LAB", TimetableGroupDetector.cleanSubjectName("DSA LAB (G-2)"))
        assertEquals("Physics Lab", TimetableGroupDetector.cleanSubjectName("Physics Lab Group 2"))
        assertEquals("Mathematics", TimetableGroupDetector.cleanSubjectName("Mathematics"))
        assertEquals("Math-III", TimetableGroupDetector.cleanSubjectName("Math-III"))
        assertEquals("HVPD", TimetableGroupDetector.cleanSubjectName("HVPD"))
    }

    @Test
    fun testParseUserConcatenatedTimetableWithInlineCommentsAndGroupFiltering() {
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

        // Verify group detection: G1, G2, G3 detected
        assertTrue(result.hasMultipleGroups)
        assertEquals(listOf("G1", "G2", "G3"), result.detectedGroups)

        // Verify entry group assignments
        val monDsaLab = result.entries.first { it.dayOfWeek == DayOfWeek.MONDAY && it.startMinute == 840 }
        assertEquals("DSA LAB G-1", monDsaLab.subjectName)
        assertEquals("G1", monDsaLab.group)

        val monOopsLab = result.entries.first { it.dayOfWeek == DayOfWeek.MONDAY && it.startMinute == 960 }
        assertEquals("OOPS LAB G-3", monOopsLab.subjectName)
        assertEquals("G3", monOopsLab.group)

        // Test filtering by Group 1 (G1)
        val g1Result = result.filterByGroup("G1", cleanSubjectNames = true)
        assertEquals(22, g1Result.entries.size) // 30 total - 8 sessions of G2 & G3 = 22 sessions
        assertFalse(g1Result.entries.any { it.group == "G2" || it.group == "G3" })
        assertTrue(g1Result.entries.all { it.group == null || it.group == "G1" })

        // Check that Monday has 4 classes for Group 1 (OOPS LAB G-3 was ignored)
        val monG1Entries = g1Result.entries.filter { it.dayOfWeek == DayOfWeek.MONDAY }
        assertEquals(4, monG1Entries.size)
        assertEquals(listOf("OOPS", "DSA", "DM", "DSA LAB"), monG1Entries.map { it.subjectName })

        // Test filtering by Group 2 (G2)
        val g2Result = result.filterByGroup("G2", cleanSubjectNames = true)
        assertEquals(22, g2Result.entries.size)
        assertFalse(g2Result.entries.any { it.group == "G1" || it.group == "G3" })
        assertTrue(g2Result.entries.all { it.group == null || it.group == "G2" })

        // Test filtering by Group 3 (G3)
        val g3Result = result.filterByGroup("G3", cleanSubjectNames = true)
        assertEquals(22, g3Result.entries.size)
        assertFalse(g3Result.entries.any { it.group == "G1" || it.group == "G2" })
        assertTrue(g3Result.entries.all { it.group == null || it.group == "G3" })

        // Check Wednesday for G3: 2 common + 2 G3 (OOPS LAB G-3 and DSA LAB G-3) = 4 sessions
        val wedG3Entries = g3Result.entries.filter { it.dayOfWeek == DayOfWeek.WEDNESDAY }
        assertEquals(4, wedG3Entries.size)
    }
}
