package com.hrshd1eux.attendly

import com.hrshd1eux.attendly.domain.parser.HolidayTextParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HolidayTextParserTest {

    @Test
    fun testParseStandardAiPromptHolidays() {
        val input = """
            HOLIDAYS
            FORMAT_VERSION: 1

            2026-01-26 | Republic Day
            2026-03-25 | Holi
            2026-08-15 | Independence Day
            2026-10-02 | Gandhi Jayanti
            2026-10-20 to 2026-10-24 | Diwali Break
            2026-12-25 | Christmas
        """.trimIndent()

        val result = HolidayTextParser.parse(input)
        assertEquals(0, result.errors.size)
        assertEquals(1, result.rangeCount)
        assertEquals(5, result.singleCount)

        // Diwali Break should expand to 5 days (Oct 20, 21, 22, 23, 24)
        // Total holidays = 5 single + 5 range = 10
        assertEquals(10, result.holidays.size)

        val repDay = result.holidays.first { it.date == LocalDate.of(2026, 1, 26) }
        assertEquals("Republic Day", repDay.name)

        val diwaliDay1 = result.holidays.first { it.date == LocalDate.of(2026, 10, 20) }
        assertEquals("Diwali Break", diwaliDay1.name)

        val diwaliDay5 = result.holidays.first { it.date == LocalDate.of(2026, 10, 24) }
        assertEquals("Diwali Break", diwaliDay5.name)
    }

    @Test
    fun testParseAlternativeDateFormatsAndBullets() {
        val input = """
            * 24/10/2026 - Diwali
            - 25-12-2026 : Christmas Day
            1. 24th October 2026 to 26th October 2026 | Extended Diwali
            2. October 2, 2026 | Gandhi Jayanti
        """.trimIndent()

        val result = HolidayTextParser.parse(input)
        assertTrue(result.holidays.isNotEmpty())

        val gj = result.holidays.first { it.date == LocalDate.of(2026, 10, 2) }
        assertEquals("Gandhi Jayanti", gj.name)

        val extDiwali = result.holidays.first { it.date == LocalDate.of(2026, 10, 25) }
        // Should be covered by either Christmas or Extended Diwali
        assertTrue(result.holidays.any { it.date == LocalDate.of(2026, 10, 26) })
    }
}
