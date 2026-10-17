package com.hrshd1eux.attendly.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hrshd1eux.attendly.data.local.entity.TimetableSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableSessionDao {

    @Query("SELECT * FROM timetable_sessions ORDER BY dayOfWeek ASC, startMinute ASC")
    fun getAllTimetableSessions(): Flow<List<TimetableSessionEntity>>

    @Query("SELECT * FROM timetable_sessions WHERE dayOfWeek = :dayOfWeek ORDER BY startMinute ASC")
    fun getSessionsForDay(dayOfWeek: Int): Flow<List<TimetableSessionEntity>>

    @Query("SELECT * FROM timetable_sessions WHERE dayOfWeek = :dayOfWeek ORDER BY startMinute ASC")
    suspend fun getSessionsForDaySync(dayOfWeek: Int): List<TimetableSessionEntity>

    @Query("SELECT * FROM timetable_sessions WHERE subjectId = :subjectId ORDER BY dayOfWeek ASC, startMinute ASC")
    fun getSessionsForSubject(subjectId: Long): Flow<List<TimetableSessionEntity>>

    @Query("SELECT * FROM timetable_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): TimetableSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TimetableSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<TimetableSessionEntity>): List<Long>

    @Update
    suspend fun updateSession(session: TimetableSessionEntity)

    @Delete
    suspend fun deleteSession(session: TimetableSessionEntity)

    @Query("DELETE FROM timetable_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    @Query("DELETE FROM timetable_sessions WHERE subjectId = :subjectId")
    suspend fun deleteSessionsForSubject(subjectId: Long)

    @Query("DELETE FROM timetable_sessions")
    suspend fun deleteAllSessions()
}
