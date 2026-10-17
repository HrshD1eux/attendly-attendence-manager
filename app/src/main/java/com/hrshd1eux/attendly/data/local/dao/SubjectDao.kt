package com.hrshd1eux.attendly.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hrshd1eux.attendly.data.local.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {

    @Query("SELECT * FROM subjects ORDER BY isArchived ASC, name ASC")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE isArchived = 0 ORDER BY name ASC")
    fun getActiveSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE isArchived = 1 ORDER BY name ASC")
    fun getArchivedSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    fun getSubjectByIdFlow(id: Long): Flow<SubjectEntity?>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectById(id: Long): SubjectEntity?

    @Query("SELECT * FROM subjects WHERE name = :name LIMIT 1")
    suspend fun getSubjectByName(name: String): SubjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<SubjectEntity>): List<Long>

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Delete
    suspend fun deleteSubject(subject: SubjectEntity)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubjectById(id: Long)

    @Query("UPDATE subjects SET isArchived = :isArchived, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setArchivedStatus(id: Long, isArchived: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE subjects SET isArchived = 1, updatedAt = :updatedAt WHERE isArchived = 0")
    suspend fun archiveAllActiveSubjects(updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM subjects")
    suspend fun deleteAllSubjects()
}
