package com.hrshd1eux.attendly.domain.model

import com.hrshd1eux.attendly.core.model.AttendanceStatus
import java.time.LocalDate

data class ClassSession(
    val id: Long = 0,
    val subjectId: Long,
    val subjectName: String = "",
    val subjectShortCode: String = "",
    val subjectColorKey: String = "EMERALD",
    val timetableSessionId: Long? = null,
    val date: LocalDate,
    val startMinute: Int,
    val endMinute: Int,
    val location: String = "",
    val status: AttendanceStatus,
    val attendanceUnits: Int = 1,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
