package com.hrshd1eux.attendly.data.repository

import com.hrshd1eux.attendly.core.model.AttendanceStatus
import com.hrshd1eux.attendly.core.model.DayOfWeek
import com.hrshd1eux.attendly.domain.calculator.AttendanceCalculator
import com.hrshd1eux.attendly.data.local.dao.ClassSessionDao
import com.hrshd1eux.attendly.data.local.dao.SubjectDao
import com.hrshd1eux.attendly.data.local.dao.TimetableSessionDao
import com.hrshd1eux.attendly.data.local.entity.ClassSessionEntity
import com.hrshd1eux.attendly.domain.model.AttendanceSummary
import com.hrshd1eux.attendly.domain.model.ClassSession
import com.hrshd1eux.attendly.domain.model.SubjectAttendanceDetail
import com.hrshd1eux.attendly.domain.model.TodayClassItem
import com.hrshd1eux.attendly.domain.repository.AttendanceRepository
import android.content.Context
import com.hrshd1eux.attendly.widget.WidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val classSessionDao: ClassSessionDao,
    private val subjectDao: SubjectDao,
    private val timetableDao: TimetableSessionDao
) : AttendanceRepository {

    override fun getTodayClassesWithStatus(date: LocalDate): Flow<List<TodayClassItem>> {
        val dayOfWeek = DayOfWeek.fromJavaDayOfWeek(date.dayOfWeek)
        val dateStr = date.toString()

        return combine(
            timetableDao.getSessionsForDay(dayOfWeek.isoValue),
            classSessionDao.getSessionsForDate(dateStr),
            subjectDao.getAllSubjects()
        ) { ttSessions, classSessions, subjects ->
            val activeSubjects = subjects.filter { !it.isArchived }
            val subjectMap = subjects.associateBy { it.id }
            val activeSubjectIds = activeSubjects.map { it.id }.toSet()

            val items = mutableListOf<TodayClassItem>()
            val matchedClassSessionIds = mutableSetOf<Long>()

            // 1. Process scheduled timetable sessions
            for (tt in ttSessions) {
                // If subject is archived, skip
                if (!activeSubjectIds.contains(tt.subjectId)) continue

                val subject = subjectMap[tt.subjectId] ?: continue

                // Check if already recorded
                val recorded = classSessions.find {
                    (it.timetableSessionId != null && it.timetableSessionId == tt.id) ||
                    (it.subjectId == tt.subjectId && it.startMinute == tt.startMinute)
                }

                if (recorded != null) {
                    matchedClassSessionIds.add(recorded.id)
                }

                val status = recorded?.let {
                    runCatching { AttendanceStatus.valueOf(it.status) }.getOrNull()
                }

                items.add(
                    TodayClassItem(
                        id = "tt_${tt.id}",
                        subjectId = tt.subjectId,
                        subjectName = subject.name,
                        subjectShortCode = subject.shortCode,
                        subjectColorKey = subject.colorKey,
                        timetableSessionId = tt.id,
                        classSessionId = recorded?.id,
                        date = date,
                        startMinute = tt.startMinute,
                        endMinute = tt.endMinute,
                        location = tt.location,
                        status = status,
                        attendanceUnits = recorded?.attendanceUnits ?: 1,
                        notes = tt.notes
                    )
                )
            }

            // 2. Add extra ad-hoc class sessions not tied to today's timetable
            for (cs in classSessions) {
                if (matchedClassSessionIds.contains(cs.id)) continue
                val subject = subjectMap[cs.subjectId] ?: continue

                val status = runCatching { AttendanceStatus.valueOf(cs.status) }.getOrDefault(AttendanceStatus.PRESENT)

                items.add(
                    TodayClassItem(
                        id = "cs_${cs.id}",
                        subjectId = cs.subjectId,
                        subjectName = subject.name,
                        subjectShortCode = subject.shortCode,
                        subjectColorKey = subject.colorKey,
                        timetableSessionId = cs.timetableSessionId,
                        classSessionId = cs.id,
                        date = date,
                        startMinute = cs.startMinute,
                        endMinute = cs.endMinute,
                        location = cs.location,
                        status = status,
                        attendanceUnits = cs.attendanceUnits,
                        notes = cs.notes
                    )
                )
            }

            items.sortedBy { it.startMinute }
        }
    }

    override fun getSessionsForDate(date: LocalDate): Flow<List<ClassSession>> {
        return combine(classSessionDao.getSessionsForDate(date.toString()), subjectDao.getAllSubjects()) { sessions, subjects ->
            val subjectMap = subjects.associateBy { it.id }
            sessions.map { it.toDomain(subjectMap[it.subjectId]) }
        }
    }

    override fun getSessionsBetweenDates(startDate: LocalDate, endDate: LocalDate): Flow<List<ClassSession>> {
        return combine(
            classSessionDao.getSessionsBetweenDates(startDate.toString(), endDate.toString()),
            subjectDao.getAllSubjects()
        ) { sessions, subjects ->
            val subjectMap = subjects.associateBy { it.id }
            sessions.map { it.toDomain(subjectMap[it.subjectId]) }
        }
    }

    override fun getSessionsForSubject(subjectId: Long): Flow<List<ClassSession>> {
        return combine(classSessionDao.getSessionsForSubject(subjectId), subjectDao.getSubjectByIdFlow(subjectId)) { sessions, subject ->
            sessions.map { it.toDomain(subject) }
        }
    }

    override fun getOverallAttendanceSummary(): Flow<AttendanceSummary> {
        return combine(
            subjectDao.getActiveSubjects(),
            classSessionDao.getAllClassSessions()
        ) { activeSubjects, allSessions ->
            if (activeSubjects.isEmpty()) {
                return@combine AttendanceCalculator.calculateSummary(0, 0, 0, 75)
            }

            var totalPresent = 0
            var totalAbsent = 0
            var totalCancelled = 0

            // Baseline counts from active subjects
            for (subject in activeSubjects) {
                totalPresent += subject.initialPresent
                totalAbsent += subject.initialAbsent
            }

            val activeSubjectIds = activeSubjects.map { it.id }.toSet()

            for (session in allSessions) {
                if (!activeSubjectIds.contains(session.subjectId)) continue
                when (session.status) {
                    AttendanceStatus.PRESENT.name -> totalPresent += session.attendanceUnits
                    AttendanceStatus.ABSENT.name -> totalAbsent += session.attendanceUnits
                    AttendanceStatus.CANCELLED.name -> totalCancelled += session.attendanceUnits
                }
            }

            // Target percentage: average or first subject target, default 75
            val avgTarget = (activeSubjects.map { it.targetPercentage }.average()).toInt().coerceIn(1, 100)

            AttendanceCalculator.calculateSummary(totalPresent, totalAbsent, totalCancelled, avgTarget)
        }
    }

    override fun getSubjectDetails(): Flow<List<SubjectAttendanceDetail>> {
        return combine(
            subjectDao.getActiveSubjects(),
            classSessionDao.getAllClassSessions()
        ) { activeSubjects, allSessions ->
            val sessionsBySubject = allSessions.groupBy { it.subjectId }

            activeSubjects.map { subjectEntity ->
                val subject = subjectEntity.toDomain()
                val sessions = sessionsBySubject[subject.id] ?: emptyList()

                var presentUnits = subject.initialPresent
                var absentUnits = subject.initialAbsent
                var cancelledUnits = 0

                for (s in sessions) {
                    when (s.status) {
                        AttendanceStatus.PRESENT.name -> presentUnits += s.attendanceUnits
                        AttendanceStatus.ABSENT.name -> absentUnits += s.attendanceUnits
                        AttendanceStatus.CANCELLED.name -> cancelledUnits += s.attendanceUnits
                    }
                }

                val summary = AttendanceCalculator.calculateSummary(
                    presentUnits = presentUnits,
                    absentUnits = absentUnits,
                    cancelledUnits = cancelledUnits,
                    targetPercentage = subject.targetPercentage
                )

                SubjectAttendanceDetail(
                    subject = subject,
                    summary = summary,
                    recentSessions = sessions.take(10).map { it.toDomain(subjectEntity) }
                )
            }
        }
    }

    override fun getSubjectDetail(subjectId: Long): Flow<SubjectAttendanceDetail?> {
        return combine(
            subjectDao.getSubjectByIdFlow(subjectId),
            classSessionDao.getSessionsForSubject(subjectId)
        ) { subjectEntity, sessions ->
            if (subjectEntity == null) return@combine null
            val subject = subjectEntity.toDomain()

            var presentUnits = subject.initialPresent
            var absentUnits = subject.initialAbsent
            var cancelledUnits = 0

            for (s in sessions) {
                when (s.status) {
                    AttendanceStatus.PRESENT.name -> presentUnits += s.attendanceUnits
                    AttendanceStatus.ABSENT.name -> absentUnits += s.attendanceUnits
                    AttendanceStatus.CANCELLED.name -> cancelledUnits += s.attendanceUnits
                }
            }

            val summary = AttendanceCalculator.calculateSummary(
                presentUnits = presentUnits,
                absentUnits = absentUnits,
                cancelledUnits = cancelledUnits,
                targetPercentage = subject.targetPercentage
            )

            SubjectAttendanceDetail(
                subject = subject,
                summary = summary,
                recentSessions = sessions.map { it.toDomain(subjectEntity) }
            )
        }
    }

    override suspend fun recordAttendance(
        subjectId: Long,
        date: LocalDate,
        status: AttendanceStatus,
        timetableSessionId: Long?,
        startMinute: Int,
        endMinute: Int,
        location: String,
        units: Int,
        notes: String
    ): Long {
        val dateStr = date.toString()

        // Check if an existing session exists for this timetable session or subject+date+time
        val existing = if (timetableSessionId != null) {
            classSessionDao.findSessionByTimetableAndDate(timetableSessionId, dateStr)
        } else {
            classSessionDao.findSessionBySubjectDateAndTime(subjectId, dateStr, startMinute)
        }

        val resultId = if (existing != null) {
            val updated = existing.copy(
                status = status.name,
                attendanceUnits = units,
                notes = notes.ifEmpty { existing.notes },
                location = location.ifEmpty { existing.location },
                updatedAt = System.currentTimeMillis()
            )
            classSessionDao.updateSession(updated)
            existing.id
        } else {
            val entity = ClassSessionEntity(
                subjectId = subjectId,
                timetableSessionId = timetableSessionId,
                date = dateStr,
                startMinute = startMinute,
                endMinute = endMinute,
                location = location,
                status = status.name,
                attendanceUnits = units,
                notes = notes
            )
            classSessionDao.insertSession(entity)
        }
        WidgetUpdater.notifyDataChanged(context)
        return resultId
    }

    override suspend fun updateAttendanceStatus(classSessionId: Long, status: AttendanceStatus) {
        val existing = classSessionDao.getSessionById(classSessionId) ?: return
        classSessionDao.updateSession(
            existing.copy(
                status = status.name,
                updatedAt = System.currentTimeMillis()
            )
        )
        WidgetUpdater.notifyDataChanged(context)
    }

    override suspend fun deleteClassSession(classSessionId: Long) {
        classSessionDao.deleteSessionById(classSessionId)
        WidgetUpdater.notifyDataChanged(context)
    }
}
