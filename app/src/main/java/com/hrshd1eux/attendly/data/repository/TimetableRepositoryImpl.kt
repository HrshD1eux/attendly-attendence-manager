package com.hrshd1eux.attendly.data.repository

import com.hrshd1eux.attendly.core.model.AttendanceUnitPolicy
import com.hrshd1eux.attendly.core.model.DayOfWeek
import com.hrshd1eux.attendly.core.util.TimeUtils
import com.hrshd1eux.attendly.data.local.dao.SubjectDao
import com.hrshd1eux.attendly.data.local.dao.TimetableSessionDao
import com.hrshd1eux.attendly.data.local.entity.SubjectEntity
import com.hrshd1eux.attendly.data.local.entity.TimetableSessionEntity
import com.hrshd1eux.attendly.domain.model.ParsedTimetableEntry
import com.hrshd1eux.attendly.domain.model.TimetableSession
import com.hrshd1eux.attendly.domain.repository.TimetableRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import android.content.Context
import com.hrshd1eux.attendly.widget.WidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimetableRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val timetableDao: TimetableSessionDao,
    private val subjectDao: SubjectDao
) : TimetableRepository {

    override fun getAllSessions(): Flow<List<TimetableSession>> {
        return combine(timetableDao.getAllTimetableSessions(), subjectDao.getAllSubjects()) { sessions, subjects ->
            val subjectMap = subjects.associateBy { it.id }
            sessions.map { it.toDomain(subjectMap[it.subjectId]) }
        }
    }

    override fun getSessionsForDay(day: DayOfWeek): Flow<List<TimetableSession>> {
        return combine(timetableDao.getSessionsForDay(day.isoValue), subjectDao.getAllSubjects()) { sessions, subjects ->
            val subjectMap = subjects.associateBy { it.id }
            sessions.map { it.toDomain(subjectMap[it.subjectId]) }
        }
    }

    override suspend fun getSessionsForDaySync(day: DayOfWeek): List<TimetableSession> {
        val sessions = timetableDao.getSessionsForDaySync(day.isoValue)
        return sessions.map { it.toDomain(subjectDao.getSubjectById(it.subjectId)) }
    }

    override fun getSessionsForSubject(subjectId: Long): Flow<List<TimetableSession>> {
        return combine(timetableDao.getSessionsForSubject(subjectId), subjectDao.getSubjectByIdFlow(subjectId)) { sessions, subject ->
            sessions.map { it.toDomain(subject) }
        }
    }

    override suspend fun addSession(session: TimetableSession): Long {
        val id = timetableDao.insertSession(TimetableSessionEntity.fromDomain(session))
        WidgetUpdater.notifyDataChanged(context)
        return id
    }

    override suspend fun updateSession(session: TimetableSession) {
        timetableDao.updateSession(TimetableSessionEntity.fromDomain(session.copy(updatedAt = System.currentTimeMillis())))
        WidgetUpdater.notifyDataChanged(context)
    }

    override suspend fun deleteSession(id: Long) {
        timetableDao.deleteSessionById(id)
        WidgetUpdater.notifyDataChanged(context)
    }

    override suspend fun clearTimetable() {
        timetableDao.deleteAllSessions()
        WidgetUpdater.notifyDataChanged(context)
    }

    override suspend fun importParsedTimetable(
        entries: List<ParsedTimetableEntry>,
        replaceExisting: Boolean
    ) {
        if (replaceExisting) {
            timetableDao.deleteAllSessions()
        }

        // Cache or create subjects
        for (entry in entries) {
            val trimmedName = entry.subjectName.trim()
            if (trimmedName.isEmpty()) continue

            var existingSubject = subjectDao.getSubjectByName(trimmedName)
            val subjectId = if (existingSubject != null) {
                existingSubject.id
            } else {
                val newSubject = SubjectEntity(
                    name = trimmedName,
                    shortCode = trimmedName.take(4).uppercase(),
                    targetPercentage = 75,
                    location = entry.location
                )
                subjectDao.insertSubject(newSubject)
            }

            val sessionEntity = TimetableSessionEntity(
                subjectId = subjectId,
                dayOfWeek = entry.dayOfWeek.isoValue,
                startMinute = entry.startMinute,
                endMinute = entry.endMinute,
                location = entry.location,
                attendanceUnitPolicy = AttendanceUnitPolicy.ONE_PER_SESSION.name,
                notes = entry.notes
            )
            timetableDao.insertSession(sessionEntity)
        }
        WidgetUpdater.notifyDataChanged(context)
    }

    override suspend fun exportTimetableAsText(dayOfWeek: DayOfWeek?): String {
        val days = if (dayOfWeek != null) listOf(dayOfWeek) else DayOfWeek.entries
        val sb = StringBuilder()
        sb.appendLine("TIMETABLE")
        sb.appendLine("FORMAT_VERSION: 1")
        if (dayOfWeek != null) {
            sb.appendLine("# 📅 Attendly Timetable - ${dayOfWeek.displayName}")
        } else {
            sb.appendLine("# 📅 Attendly Weekly Timetable")
        }
        sb.appendLine()

        var hasAny = false
        for (day in days) {
            val daySessions = timetableDao.getSessionsForDaySync(day.isoValue).sortedBy { it.startMinute }
            if (daySessions.isNotEmpty()) {
                hasAny = true
                sb.appendLine("DAY: ${day.name}")
                for (s in daySessions) {
                    val subj = subjectDao.getSubjectById(s.subjectId)
                    val start = TimeUtils.minutesToTimeString(s.startMinute, com.hrshd1eux.attendly.core.model.TimeFormat.FORMAT_24H)
                    val end = TimeUtils.minutesToTimeString(s.endMinute, com.hrshd1eux.attendly.core.model.TimeFormat.FORMAT_24H)
                    val subjName = subj?.name ?: "Unknown"
                    val locPart = if (s.location.isNotBlank()) " | ${s.location.trim()}" else ""
                    val notesPart = if (s.notes.isNotBlank()) " | ${s.notes.trim()}" else ""
                    sb.appendLine("$start-$end | $subjName$locPart$notesPart")
                }
                sb.appendLine()
            }
        }
        if (!hasAny) {
            return ""
        }
        return sb.toString().trim()
    }
}
