package com.hrshd1eux.attendly.domain.model

import com.hrshd1eux.attendly.core.model.AttendanceUnitPolicy
import com.hrshd1eux.attendly.core.model.DayOfWeek

data class TimetableSession(
    val id: Long = 0,
    val subjectId: Long,
    val subjectName: String = "",
    val subjectShortCode: String = "",
    val subjectColorKey: String = "EMERALD",
    val dayOfWeek: DayOfWeek,
    val startMinute: Int, // e.g. 540 = 09:00
    val endMinute: Int,   // e.g. 600 = 10:00
    val location: String = "",
    val attendanceUnitPolicy: AttendanceUnitPolicy = AttendanceUnitPolicy.ONE_PER_SESSION,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
