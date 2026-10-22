package com.hrshd1eux.attendly

import com.hrshd1eux.attendly.core.model.DayOfWeek
import com.hrshd1eux.attendly.domain.parser.TimetableTextParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetableExportTest {

    @Test
    fun testExportedFormatIsCompatibleWithParser() {
        val exportedText = """
            TIMETABLE
            FORMAT_VERSION: 1
            # 📅 Attendly Weekly Timetable

            DAY: MONDAY
            09:00-10:00 | Mathematics | Room 101 | Teacher: Dr. Smith
            10:00-11:00 | Data Structures | Lab 2

            DAY: WEDNESDAY
            11:00-12:00 | Database Systems | Room 303
        """.trimIndent()

        val parseResult = TimetableTextParser.parse(exportedText)

        assertEquals(3, parseResult.entries.size)
        assertTrue(parseResult.unrecognizedLines.isEmpty())

        val math = parseResult.entries[0]
        assertEquals("Mathematics", math.subjectName)
        assertEquals(DayOfWeek.MONDAY, math.dayOfWeek)
        assertEquals(540, math.startMinute)
        assertEquals(600, math.endMinute)
        assertEquals("Room 101", math.location)
        assertEquals("Dr. Smith", math.notes)

        val ds = parseResult.entries[1]
        assertEquals("Data Structures", ds.subjectName)
        assertEquals(DayOfWeek.MONDAY, ds.dayOfWeek)
        assertEquals(600, ds.startMinute)
        assertEquals(660, ds.endMinute)
        assertEquals("Lab 2", ds.location)

        val db = parseResult.entries[2]
        assertEquals("Database Systems", db.subjectName)
        assertEquals(DayOfWeek.WEDNESDAY, db.dayOfWeek)
        assertEquals(660, db.startMinute)
        assertEquals(720, db.endMinute)
        assertEquals("Room 303", db.location)
    }

    @Test
    fun testSingleDayExportFormat() {
        val exportedText = """
            TIMETABLE
            FORMAT_VERSION: 1
            # 📅 Attendly Timetable - Friday

            DAY: FRIDAY
            14:00-15:30 | Computer Networks | Hall B
        """.trimIndent()

        val parseResult = TimetableTextParser.parse(exportedText)

        assertEquals(1, parseResult.entries.size)
        val cn = parseResult.entries[0]
        assertEquals("Computer Networks", cn.subjectName)
        assertEquals(DayOfWeek.FRIDAY, cn.dayOfWeek)
        assertEquals(840, cn.startMinute)
        assertEquals(930, cn.endMinute)
        assertEquals("Hall B", cn.location)
    }
}
