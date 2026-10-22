package com.hrshd1eux.attendly.domain.repository

import com.hrshd1eux.attendly.core.model.DayOfWeek
import com.hrshd1eux.attendly.domain.model.ParsedTimetableEntry
import com.hrshd1eux.attendly.domain.model.TimetableSession
import kotlinx.coroutines.flow.Flow

interface TimetableRepository {
    fun getAllSessions(): Flow<List<TimetableSession>>
    fun getSessionsForDay(day: DayOfWeek): Flow<List<TimetableSession>>
    suspend fun getSessionsForDaySync(day: DayOfWeek): List<TimetableSession>
    fun getSessionsForSubject(subjectId: Long): Flow<List<TimetableSession>>
    suspend fun addSession(session: TimetableSession): Long
    suspend fun updateSession(session: TimetableSession)
    suspend fun deleteSession(id: Long)
    suspend fun clearTimetable()
    suspend fun importParsedTimetable(entries: List<ParsedTimetableEntry>, replaceExisting: Boolean)
    suspend fun exportTimetableAsText(dayOfWeek: DayOfWeek? = null): String
}
