package com.hrshd1eux.attendly

import com.hrshd1eux.attendly.domain.calculator.AttendanceCalculator
import com.hrshd1eux.attendly.domain.model.AttendanceStanding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceCalculatorTest {

    @Test
    fun testEmptyAttendance_returnsZeroAndNoData() {
        val summary = AttendanceCalculator.calculateSummary(
            presentUnits = 0,
            absentUnits = 0,
            targetPercentage = 75
        )
        assertEquals(0, summary.totalEligibleUnits)
        assertEquals(0.0, summary.percentage, 0.001)
        assertEquals(0, summary.safeAbsences)
        assertNull(summary.recoveryNeeded)
        assertEquals(AttendanceStanding.NO_DATA, summary.standing)
    }

    @Test
    fun testSafeAbsenceCalculation_promptExample() {
        // From prompt: 36 present, 9 absent, target 75% -> 80% attendance, exactly 3 safe absences!
        val summary = AttendanceCalculator.calculateSummary(
            presentUnits = 36,
            absentUnits = 9,
            targetPercentage = 75
        )
        assertEquals(45, summary.totalEligibleUnits)
        assertEquals(80.0, summary.percentage, 0.001)
        assertEquals(3, summary.safeAbsences)
        assertNull(summary.recoveryNeeded)
        assertFalse(summary.isImpossible100)
        assertEquals("3 safe absences remaining", summary.statusMessage)
    }

    @Test
    fun testRecoveryClassesCalculation_promptExample() {
        // From prompt: 30 present, 15 absent, target 75% -> 66.67%, exactly 15 recovery classes required!
        val summary = AttendanceCalculator.calculateSummary(
            presentUnits = 30,
            absentUnits = 15,
            targetPercentage = 75
        )
        assertEquals(45, summary.totalEligibleUnits)
        assertEquals(66.666, summary.percentage, 0.01)
        assertEquals(0, summary.safeAbsences)
        assertEquals(15, summary.recoveryNeeded)
        assertFalse(summary.isImpossible100)
        assertEquals(AttendanceStanding.BELOW_TARGET, summary.standing)
        assertEquals("Attend the next 15 classes to reach target", summary.statusMessage)
    }

    @Test
    fun testTarget100WithAbsence_isImpossible() {
        // Target 100% with even 1 absence can never be reached
        val summary = AttendanceCalculator.calculateSummary(
            presentUnits = 20,
            absentUnits = 1,
            targetPercentage = 100
        )
        assertTrue(summary.isImpossible100)
        assertNull(summary.recoveryNeeded)
        assertEquals(0, summary.safeAbsences)
        assertEquals("Target 100% cannot be reached due to past absences", summary.statusMessage)
    }

    @Test
    fun testTarget100WithoutAbsence_isSafe() {
        // Target 100% with 0 absences: safe absences is 0
        val summary = AttendanceCalculator.calculateSummary(
            presentUnits = 20,
            absentUnits = 0,
            targetPercentage = 100
        )
        assertEquals(100.0, summary.percentage, 0.001)
        assertEquals(0, summary.safeAbsences)
        assertFalse(summary.isImpossible100)
        assertEquals("On track - cannot miss next class", summary.statusMessage)
    }

    @Test
    fun testExactlyAtTarget() {
        // 30 present, 10 absent -> 30/40 = 75%
        val summary = AttendanceCalculator.calculateSummary(
            presentUnits = 30,
            absentUnits = 10,
            targetPercentage = 75
        )
        assertEquals(75.0, summary.percentage, 0.001)
        assertEquals(0, summary.safeAbsences)
        assertNull(summary.recoveryNeeded)
        assertEquals(AttendanceStanding.ON_TRACK, summary.standing)
        assertEquals("On track - cannot miss next class", summary.statusMessage)
    }

    @Test
    fun testCancelledClasses_doNotAffectDenominator() {
        val summary = AttendanceCalculator.calculateSummary(
            presentUnits = 8,
            absentUnits = 2,
            cancelledUnits = 5,
            targetPercentage = 75
        )
        // 8 present, 2 absent => 10 eligible classes. Percentage is 80%.
        assertEquals(10, summary.totalEligibleUnits)
        assertEquals(80.0, summary.percentage, 0.001)
        assertEquals(5, summary.cancelledUnits)
    }
}
