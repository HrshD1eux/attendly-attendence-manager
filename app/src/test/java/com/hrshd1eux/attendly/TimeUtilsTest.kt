package com.hrshd1eux.attendly

import com.hrshd1eux.attendly.core.model.TimeFormat
import com.hrshd1eux.attendly.core.util.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TimeUtilsTest {

    @Test
    fun testMinutesToTimeString12H() {
        assertEquals("9:00 AM", TimeUtils.minutesToTimeString(540, TimeFormat.FORMAT_12H))
        assertEquals("12:00 PM", TimeUtils.minutesToTimeString(720, TimeFormat.FORMAT_12H))
        assertEquals("1:30 PM", TimeUtils.minutesToTimeString(810, TimeFormat.FORMAT_12H))
        assertEquals("12:00 AM", TimeUtils.minutesToTimeString(0, TimeFormat.FORMAT_12H))
    }

    @Test
    fun testMinutesToTimeString24H() {
        assertEquals("09:00", TimeUtils.minutesToTimeString(540, TimeFormat.FORMAT_24H))
        assertEquals("12:00", TimeUtils.minutesToTimeString(720, TimeFormat.FORMAT_24H))
        assertEquals("13:30", TimeUtils.minutesToTimeString(810, TimeFormat.FORMAT_24H))
    }

    @Test
    fun testParseTimeToMinutes() {
        assertEquals(540, TimeUtils.parseTimeToMinutes("09:00"))
        assertEquals(540, TimeUtils.parseTimeToMinutes("9:00 AM"))
        assertEquals(810, TimeUtils.parseTimeToMinutes("1:30 PM"))
        assertEquals(810, TimeUtils.parseTimeToMinutes("13:30"))
        assertNull(TimeUtils.parseTimeToMinutes("invalid_time"))
    }

    @Test
    fun testDurationCalculation() {
        assertEquals("1h", TimeUtils.formatDuration(540, 600))
        assertEquals("2h", TimeUtils.formatDuration(660, 780))
        assertEquals("1h 30m", TimeUtils.formatDuration(540, 630))
        assertEquals(2, TimeUtils.calculateDurationHours(660, 780))
    }
}
