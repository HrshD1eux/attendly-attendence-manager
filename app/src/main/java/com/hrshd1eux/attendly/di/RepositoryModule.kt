package com.hrshd1eux.attendly.di

import com.hrshd1eux.attendly.data.repository.AttendanceRepositoryImpl
import com.hrshd1eux.attendly.data.repository.BackupRestoreRepositoryImpl
import com.hrshd1eux.attendly.data.repository.SubjectRepositoryImpl
import com.hrshd1eux.attendly.data.repository.TimetableRepositoryImpl
import com.hrshd1eux.attendly.domain.repository.AttendanceRepository
import com.hrshd1eux.attendly.domain.repository.BackupRestoreRepository
import com.hrshd1eux.attendly.domain.repository.SubjectRepository
import com.hrshd1eux.attendly.domain.repository.TimetableRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSubjectRepository(impl: SubjectRepositoryImpl): SubjectRepository

    @Binds
    @Singleton
    abstract fun bindTimetableRepository(impl: TimetableRepositoryImpl): TimetableRepository

    @Binds
    @Singleton
    abstract fun bindAttendanceRepository(impl: AttendanceRepositoryImpl): AttendanceRepository

    @Binds
    @Singleton
    abstract fun bindBackupRestoreRepository(impl: BackupRestoreRepositoryImpl): BackupRestoreRepository
}
