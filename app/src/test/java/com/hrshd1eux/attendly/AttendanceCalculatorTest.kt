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

    @Test
    fun testOnDutyPolicy_countAsPresent() {
        val summary = AttendanceCalculator.calculateSummary(
            presentUnits = 10,
            absentUnits = 5,
            cancelledUnits = 0,
            targetPercentage = 75,
            onDutyUnits = 5,
            onDutyPolicy = com.hrshd1eux.attendly.core.model.OnDutyPolicy.COUNT_AS_PRESENT
        )
        // 10 base present + 5 OD present = 15 present. Total = 15 + 5 = 20. 15/20 = 75.0%
        assertEquals(20, summary.totalEligibleUnits)
        assertEquals(15, summary.presentUnits)
        assertEquals(75.0, summary.percentage, 0.001)
        assertEquals(AttendanceStanding.ON_TRACK, summary.standing)
    }

    @Test
    fun testOnDutyPolicy_excused() {
        val summary = AttendanceCalculator.calculateSummary(
            presentUnits = 10,
            absentUnits = 5,
            cancelledUnits = 0,
            targetPercentage = 75,
            onDutyUnits = 5,
            onDutyPolicy = com.hrshd1eux.attendly.core.model.OnDutyPolicy.EXCUSED
        )
        // 10 present, 5 absent. 5 OD excused (cancelled). Total = 15. 10/15 = 66.67%
        assertEquals(15, summary.totalEligibleUnits)
        assertEquals(10, summary.presentUnits)
        assertEquals(5, summary.cancelledUnits)
    }

    @Test
    fun testMedicalLeavePolicy_lowerTargetThreshold() {
        val summary = AttendanceCalculator.calculateSummary(
            presentUnits = 13,
            absentUnits = 7, // 13/20 = 65%
            cancelledUnits = 0,
            targetPercentage = 75,
            medicalUnits = 3,
            medicalLeavePolicy = com.hrshd1eux.attendly.core.model.MedicalLeavePolicy.LOWER_TARGET_THRESHOLD,
            medicalConcessionTarget = 65
        )
        // Effective target lowered to 65% due to medical concession
        assertEquals(65, summary.targetPercentage)
        assertEquals(65.0, summary.percentage, 0.001)
        assertEquals(AttendanceStanding.ON_TRACK, summary.standing)
    }

    @Test
    fun testWhatIfScenarioSimulation() {
        val currentSummary = AttendanceCalculator.calculateSummary(
            presentUnits = 36,
            absentUnits = 9,
            targetPercentage = 75
        ) // 80% (36/45)

        // Simulate bunking 5 classes
        val simResult = AttendanceCalculator.simulateScenario(
            currentSummary = currentSummary,
            additionalAttended = 0,
            additionalBunked = 5
        )

        // 36 / 50 = 72% -> drops below 75% target!
        assertEquals(72.0, simResult.projectedSummary.percentage, 0.001)
        assertTrue(simResult.isDebarRisk)
        assertEquals(-8.0, simResult.deltaPercentage, 0.001)
        assertEquals(6, simResult.recoveryClassesNeeded) // need 6 classes straight to recover back to 75%
    }
}
