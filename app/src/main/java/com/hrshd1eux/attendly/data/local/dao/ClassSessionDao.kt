package com.hrshd1eux.attendly.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hrshd1eux.attendly.data.local.entity.ClassSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassSessionDao {

    @Query("SELECT * FROM class_sessions ORDER BY date DESC, startMinute ASC")
    fun getAllClassSessions(): Flow<List<ClassSessionEntity>>

    @Query("SELECT * FROM class_sessions ORDER BY date DESC, startMinute ASC")
    suspend fun getAllClassSessionsSync(): List<ClassSessionEntity>

    @Query("SELECT * FROM class_sessions WHERE date = :date ORDER BY startMinute ASC")
    fun getSessionsForDate(date: String): Flow<List<ClassSessionEntity>>

    @Query("SELECT * FROM class_sessions WHERE date = :date ORDER BY startMinute ASC")
    suspend fun getSessionsForDateSync(date: String): List<ClassSessionEntity>

    @Query("SELECT * FROM class_sessions WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, startMinute ASC")
    fun getSessionsBetweenDates(startDate: String, endDate: String): Flow<List<ClassSessionEntity>>

    @Query("SELECT * FROM class_sessions WHERE subjectId = :subjectId ORDER BY date DESC, startMinute ASC")
    fun getSessionsForSubject(subjectId: Long): Flow<List<ClassSessionEntity>>

    @Query("SELECT * FROM class_sessions WHERE subjectId = :subjectId ORDER BY date DESC, startMinute ASC")
    suspend fun getSessionsForSubjectSync(subjectId: Long): List<ClassSessionEntity>

    @Query("SELECT * FROM class_sessions WHERE timetableSessionId = :timetableSessionId AND date = :date LIMIT 1")
    suspend fun findSessionByTimetableAndDate(timetableSessionId: Long, date: String): ClassSessionEntity?

    @Query("SELECT * FROM class_sessions WHERE subjectId = :subjectId AND date = :date AND startMinute = :startMinute LIMIT 1")
    suspend fun findSessionBySubjectDateAndTime(subjectId: Long, date: String, startMinute: Int): ClassSessionEntity?

    @Query("SELECT * FROM class_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): ClassSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ClassSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<ClassSessionEntity>): List<Long>

    @Update
    suspend fun updateSession(session: ClassSessionEntity)

    @Delete
    suspend fun deleteSession(session: ClassSessionEntity)

    @Query("DELETE FROM class_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    @Query("DELETE FROM class_sessions WHERE subjectId = :subjectId")
    suspend fun deleteSessionsForSubject(subjectId: Long)

    @Query("DELETE FROM class_sessions")
    suspend fun deleteAllSessions()
}
