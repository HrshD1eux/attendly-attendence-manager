package com.hrshd1eux.attendly.domain.calculator

import com.hrshd1eux.attendly.domain.model.AttendanceStanding
import com.hrshd1eux.attendly.domain.model.AttendanceSummary
import java.util.Locale

object AttendanceCalculator {

    /**
     * Calculates the attendance summary for a given number of present and absent units,
     * against a specific target percentage (1..100).
     */
    fun calculateSummary(
        presentUnits: Int,
        absentUnits: Int,
        cancelledUnits: Int = 0,
        targetPercentage: Int = 75
    ): AttendanceSummary {
        val validP = maxOf(0, presentUnits)
        val validA = maxOf(0, absentUnits)
        val validC = maxOf(0, cancelledUnits)
        val target = targetPercentage.coerceIn(1, 100)

        val totalEligible = validP + validA

        if (totalEligible == 0) {
            return AttendanceSummary(
                presentUnits = validP,
                absentUnits = validA,
                cancelledUnits = validC,
                totalEligibleUnits = 0,
                percentage = 0.0,
                formattedPercentage = "0%",
                targetPercentage = target,
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
        val rhs = target.toLong() * totalEligible

        return if (lhs >= rhs) {
            // At or above target: calculate safe absences
            // T * x <= 100 * P - T * (P + A)
            val surplus = lhs - rhs
            val safeAbsences = (surplus / target).toInt().coerceAtLeast(0)

            val standing = when {
                percentage >= target + 10.0 -> AttendanceStanding.EXCELLENT
                percentage > target -> AttendanceStanding.ABOVE_TARGET
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
                totalEligibleUnits = totalEligible,
                percentage = percentage,
                formattedPercentage = formattedPercentage,
                targetPercentage = target,
                safeAbsences = safeAbsences,
                recoveryNeeded = null,
                isImpossible100 = false,
                standing = standing,
                statusMessage = statusMsg
            )
        } else {
            // Below target: calculate recovery classes needed
            if (target == 100) {
                // With at least 1 absence, 100% is impossible for any finite number of classes
                AttendanceSummary(
                    presentUnits = validP,
                    absentUnits = validA,
                    cancelledUnits = validC,
                    totalEligibleUnits = totalEligible,
                    percentage = percentage,
                    formattedPercentage = formattedPercentage,
                    targetPercentage = target,
                    safeAbsences = 0,
                    recoveryNeeded = null,
                    isImpossible100 = true,
                    standing = AttendanceStanding.BELOW_TARGET,
                    statusMessage = "Target 100% cannot be reached due to past absences"
                )
            } else {
                // (100 - T) * n >= T * (P + A) - 100 * P
                val deficit = rhs - lhs
                val denom = (100 - target).toLong()
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
                    totalEligibleUnits = totalEligible,
                    percentage = percentage,
                    formattedPercentage = formattedPercentage,
                    targetPercentage = target,
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
