package com.hrshd1eux.attendly.domain.repository

import com.hrshd1eux.attendly.domain.model.Subject
import kotlinx.coroutines.flow.Flow

interface SubjectRepository {
    fun getAllSubjects(): Flow<List<Subject>>
    fun getActiveSubjects(): Flow<List<Subject>>
    fun getArchivedSubjects(): Flow<List<Subject>>
    fun getSubjectById(id: Long): Flow<Subject?>
    suspend fun getSubjectByIdSync(id: Long): Subject?
    suspend fun addSubject(subject: Subject): Long
    suspend fun updateSubject(subject: Subject)
    suspend fun setArchivedStatus(id: Long, isArchived: Boolean)
    suspend fun deleteSubject(id: Long)
}
