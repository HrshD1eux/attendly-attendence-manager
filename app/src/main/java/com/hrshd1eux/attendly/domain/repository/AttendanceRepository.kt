package com.hrshd1eux.attendly.domain.repository

import com.hrshd1eux.attendly.core.model.AttendanceStatus
import com.hrshd1eux.attendly.domain.model.AttendanceSummary
import com.hrshd1eux.attendly.domain.model.ClassSession
import com.hrshd1eux.attendly.domain.model.SubjectAttendanceDetail
import com.hrshd1eux.attendly.domain.model.TodayClassItem
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

data class UnmarkedClassSession(
    val timetableSessionId: Long,
    val subjectId: Long,
    val subjectName: String,
    val subjectShortCode: String,
    val subjectColorKey: String,
    val date: LocalDate,
    val startMinute: Int,
    val endMinute: Int,
    val location: String
)

interface AttendanceRepository {
    fun getTodayClassesWithStatus(date: LocalDate = LocalDate.now()): Flow<List<TodayClassItem>>
    fun getSessionsForDate(date: LocalDate): Flow<List<ClassSession>>
    fun getSessionsBetweenDates(startDate: LocalDate, endDate: LocalDate): Flow<List<ClassSession>>
    fun getSessionsForSubject(subjectId: Long): Flow<List<ClassSession>>
    fun getOverallAttendanceSummary(): Flow<AttendanceSummary>
    fun getSubjectDetails(): Flow<List<SubjectAttendanceDetail>>
    fun getArchivedSubjectDetails(): Flow<List<SubjectAttendanceDetail>>
    fun getSubjectDetail(subjectId: Long): Flow<SubjectAttendanceDetail?>

    suspend fun getUnmarkedSessions(lookbackDays: Int = 7): List<UnmarkedClassSession>

    suspend fun recordAttendance(
        subjectId: Long,
        date: LocalDate,
        status: AttendanceStatus,
        timetableSessionId: Long? = null,
        startMinute: Int = 0,
        endMinute: Int = 0,
        location: String = "",
        units: Int = 1,
        notes: String = ""
    ): Long

    suspend fun updateAttendanceStatus(classSessionId: Long, status: AttendanceStatus)
    suspend fun deleteClassSession(classSessionId: Long)
}
