package com.hrshd1eux.attendly.domain.calculator

import com.hrshd1eux.attendly.core.model.MedicalLeavePolicy
import com.hrshd1eux.attendly.core.model.OnDutyPolicy
import com.hrshd1eux.attendly.domain.model.AttendanceStanding
import com.hrshd1eux.attendly.domain.model.AttendanceSummary
import com.hrshd1eux.attendly.domain.model.ScenarioSimulationResult
import java.util.Locale

object AttendanceCalculator {

    /**
     * Calculates the attendance summary for a given number of present and absent units,
     * against a specific target percentage (1..100) and respecting university leave policies.
     */
    fun calculateSummary(
        presentUnits: Int,
        absentUnits: Int,
        cancelledUnits: Int = 0,
        targetPercentage: Int = 75,
        onDutyUnits: Int = 0,
        medicalUnits: Int = 0,
        onDutyPolicy: OnDutyPolicy = OnDutyPolicy.COUNT_AS_PRESENT,
        medicalLeavePolicy: MedicalLeavePolicy = MedicalLeavePolicy.EXCUSED,
        medicalConcessionTarget: Int = 65
    ): AttendanceSummary {
        val validOD = maxOf(0, onDutyUnits)
        val validMed = maxOf(0, medicalUnits)
        val baseP = maxOf(0, presentUnits)
        val validA = maxOf(0, absentUnits)

        // Apply On-Duty Policy
        val validP = if (onDutyPolicy == OnDutyPolicy.COUNT_AS_PRESENT) {
            baseP + validOD
        } else {
            baseP
        }

        // Apply Medical & Excused Cancelled Units
        val extraCancelled = (if (onDutyPolicy == OnDutyPolicy.EXCUSED) validOD else 0) +
                (if (medicalLeavePolicy == MedicalLeavePolicy.EXCUSED) validMed else 0)
        val validC = maxOf(0, cancelledUnits) + extraCancelled

        // Determine effective target (applies concession threshold if medical leave present)
        val effectiveTarget = if (validMed > 0 && medicalLeavePolicy == MedicalLeavePolicy.LOWER_TARGET_THRESHOLD) {
            minOf(targetPercentage, medicalConcessionTarget).coerceIn(1, 100)
        } else {
            targetPercentage.coerceIn(1, 100)
        }

        val totalEligible = validP + validA

        if (totalEligible == 0) {
            return AttendanceSummary(
                presentUnits = validP,
                absentUnits = validA,
                cancelledUnits = validC,
                onDutyUnits = validOD,
                medicalUnits = validMed,
                totalEligibleUnits = 0,
                percentage = 0.0,
                formattedPercentage = "0%",
                targetPercentage = effectiveTarget,
                safeAbsences = 0,
                recoveryNeeded = null,
                isImpossible100 = false,
                standing = AttendanceStanding.NO_DATA,
                statusMessage = "No attendance recorded yet"
            )
        }

        val percentage = (validP.toDouble() * 100.0) / totalEligible.toDouble()
        val formattedPercentage = formatPercentage(percentage)

        // Compare 100 * P with Target * (P + A) using exact integer arithmetic
        val lhs = 100L * validP
        val rhs = effectiveTarget.toLong() * totalEligible

        return if (lhs >= rhs) {
            // At or above target: calculate safe absences
            // T * x <= 100 * P - T * (P + A)
            val surplus = lhs - rhs
            val safeAbsences = (surplus / effectiveTarget).toInt().coerceAtLeast(0)

            val standing = when {
                percentage >= effectiveTarget + 10.0 -> AttendanceStanding.EXCELLENT
                percentage > effectiveTarget -> AttendanceStanding.ABOVE_TARGET
                else -> AttendanceStanding.ON_TRACK
            }

            val statusMsg = when {
                safeAbsences > 1 -> "$safeAbsences safe absences remaining"
                safeAbsences == 1 -> "1 safe absence remaining"
                else -> "On track - cannot miss next class"
            }

            AttendanceSummary(
                presentUnits = validP,
                absentUnits = validA,
                cancelledUnits = validC,
                onDutyUnits = validOD,
                medicalUnits = validMed,
                totalEligibleUnits = totalEligible,
                percentage = percentage,
                formattedPercentage = formattedPercentage,
                targetPercentage = effectiveTarget,
                safeAbsences = safeAbsences,
                recoveryNeeded = null,
                isImpossible100 = false,
                standing = standing,
                statusMessage = statusMsg
            )
        } else {
            // Below target: calculate recovery classes needed
            if (effectiveTarget == 100) {
                // With at least 1 absence, 100% is impossible for any finite number of classes
                AttendanceSummary(
                    presentUnits = validP,
                    absentUnits = validA,
                    cancelledUnits = validC,
                    onDutyUnits = validOD,
                    medicalUnits = validMed,
                    totalEligibleUnits = totalEligible,
                    percentage = percentage,
                    formattedPercentage = formattedPercentage,
                    targetPercentage = effectiveTarget,
                    safeAbsences = 0,
                    recoveryNeeded = null,
                    isImpossible100 = true,
                    standing = AttendanceStanding.BELOW_TARGET,
                    statusMessage = "Target 100% cannot be reached due to past absences"
                )
            } else {
                // (100 - T) * n >= T * (P + A) - 100 * P
                val deficit = rhs - lhs
                val denom = (100 - effectiveTarget).toLong()
                // Ceiling division for positive integers: (num + denom - 1) / denom
                val recovery = ((deficit + denom - 1) / denom).toInt()

                val statusMsg = if (recovery == 1) {
                    "Attend the next class to reach target"
                } else {
                    "Attend the next $recovery classes to reach target"
                }

                AttendanceSummary(
                    presentUnits = validP,
                    absentUnits = validA,
                    cancelledUnits = validC,
                    onDutyUnits = validOD,
                    medicalUnits = validMed,
                    totalEligibleUnits = totalEligible,
                    percentage = percentage,
                    formattedPercentage = formattedPercentage,
                    targetPercentage = effectiveTarget,
                    safeAbsences = 0,
                    recoveryNeeded = recovery,
                    isImpossible100 = false,
                    standing = AttendanceStanding.BELOW_TARGET,
                    statusMessage = statusMsg
                )
            }
        }
    }

    /**
     * Simulates future scenario: attending X more, bunking Y more classes.
     */
    fun simulateScenario(
        currentSummary: AttendanceSummary,
        additionalAttended: Int,
        additionalBunked: Int,
        additionalCancelled: Int = 0,
        targetPercentage: Int = currentSummary.targetPercentage
    ): ScenarioSimulationResult {
        val projPresent = currentSummary.presentUnits + maxOf(0, additionalAttended)
        val projAbsent = currentSummary.absentUnits + maxOf(0, additionalBunked)
        val projCancelled = currentSummary.cancelledUnits + maxOf(0, additionalCancelled)

        val projSummary = calculateSummary(
            presentUnits = projPresent,
            absentUnits = projAbsent,
            cancelledUnits = projCancelled,
            targetPercentage = targetPercentage,
            onDutyUnits = currentSummary.onDutyUnits,
            medicalUnits = currentSummary.medicalUnits
        )

        val delta = projSummary.percentage - currentSummary.percentage
        val isDebarRisk = projSummary.percentage < targetPercentage

        return ScenarioSimulationResult(
            currentSummary = currentSummary,
            projectedSummary = projSummary,
            deltaPercentage = delta,
            additionalAttended = additionalAttended,
            additionalBunked = additionalBunked,
            isDebarRisk = isDebarRisk,
            recoveryClassesNeeded = projSummary.recoveryNeeded
        )
    }

    /**
     * Formats a double percentage cleanly, e.g. 78.5% or 80%.
     */
    fun formatPercentage(percentage: Double): String {
        return if (percentage % 1.0 == 0.0) {
            String.format(Locale.US, "%.0f%%", percentage)
        } else {
            String.format(Locale.US, "%.1f%%", percentage)
        }
    }
}
