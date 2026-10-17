package com.hrshd1eux.attendly.data.repository

import com.hrshd1eux.attendly.data.local.dao.SubjectDao
import com.hrshd1eux.attendly.data.local.entity.SubjectEntity
import com.hrshd1eux.attendly.domain.model.Subject
import com.hrshd1eux.attendly.domain.repository.SubjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import android.content.Context
import com.hrshd1eux.attendly.widget.WidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubjectRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val subjectDao: SubjectDao
) : SubjectRepository {

    override fun getAllSubjects(): Flow<List<Subject>> {
        return subjectDao.getAllSubjects().map { list -> list.map { it.toDomain() } }
    }

    override fun getActiveSubjects(): Flow<List<Subject>> {
        return subjectDao.getActiveSubjects().map { list -> list.map { it.toDomain() } }
    }

    override fun getArchivedSubjects(): Flow<List<Subject>> {
        return subjectDao.getArchivedSubjects().map { list -> list.map { it.toDomain() } }
    }

    override fun getSubjectById(id: Long): Flow<Subject?> {
        return subjectDao.getSubjectByIdFlow(id).map { it?.toDomain() }
    }

    override suspend fun getSubjectByIdSync(id: Long): Subject? {
        return subjectDao.getSubjectById(id)?.toDomain()
    }

    override suspend fun addSubject(subject: Subject): Long {
        val id = subjectDao.insertSubject(SubjectEntity.fromDomain(subject))
        WidgetUpdater.notifyDataChanged(context)
        return id
    }

    override suspend fun updateSubject(subject: Subject) {
        subjectDao.updateSubject(SubjectEntity.fromDomain(subject.copy(updatedAt = System.currentTimeMillis())))
        WidgetUpdater.notifyDataChanged(context)
    }

    override suspend fun setArchivedStatus(id: Long, isArchived: Boolean) {
        subjectDao.setArchivedStatus(id, isArchived)
        WidgetUpdater.notifyDataChanged(context)
    }

    override suspend fun deleteSubject(id: Long) {
        subjectDao.deleteSubjectById(id)
        WidgetUpdater.notifyDataChanged(context)
    }
}
