package com.hrshd1eux.attendly.domain.model

import com.hrshd1eux.attendly.core.model.AttendanceStatus
import java.time.LocalDate

data class TodayClassItem(
    val id: String, // Unique composite key e.g. "tt_$timetableSessionId" or "cs_$classSessionId"
    val subjectId: Long,
    val subjectName: String,
    val subjectShortCode: String = "",
    val subjectColorKey: String = "EMERALD",
    val timetableSessionId: Long? = null,
    val classSessionId: Long? = null,
    val date: LocalDate,
    val startMinute: Int,
    val endMinute: Int,
    val location: String = "",
    val status: AttendanceStatus? = null, // null if unrecorded
    val attendanceUnits: Int = 1,
    val notes: String = ""
)
