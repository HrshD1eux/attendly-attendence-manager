package com.hrshd1eux.attendly.domain.model

enum class AttendanceStanding(val label: String) {
    EXCELLENT("Excellent"),
    ABOVE_TARGET("Above Target"),
    ON_TRACK("On Track"),
    BELOW_TARGET("Below Target"),
    NO_DATA("No Data Recorded")
}

data class AttendanceSummary(
    val presentUnits: Int,
    val absentUnits: Int,
    val cancelledUnits: Int = 0,
    val onDutyUnits: Int = 0,
    val medicalUnits: Int = 0,
    val totalEligibleUnits: Int,
    val percentage: Double,
    val formattedPercentage: String,
    val targetPercentage: Int,
    val safeAbsences: Int,
    val recoveryNeeded: Int?,
    val isImpossible100: Boolean = false,
    val standing: AttendanceStanding,
    val statusMessage: String
)

data class ScenarioSimulationResult(
    val currentSummary: AttendanceSummary,
    val projectedSummary: AttendanceSummary,
    val deltaPercentage: Double,
    val additionalAttended: Int,
    val additionalBunked: Int,
    val isDebarRisk: Boolean,
    val recoveryClassesNeeded: Int?
)

data class SubjectAttendanceDetail(
    val subject: Subject,
    val summary: AttendanceSummary,
    val recentSessions: List<ClassSession> = emptyList()
)
