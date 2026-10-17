package com.hrshd1eux.attendly.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hrshd1eux.attendly.data.local.dao.ClassSessionDao
import com.hrshd1eux.attendly.data.local.dao.SubjectDao
import com.hrshd1eux.attendly.data.local.dao.TimetableSessionDao
import com.hrshd1eux.attendly.data.local.entity.ClassSessionEntity
import com.hrshd1eux.attendly.data.local.entity.SubjectEntity
import com.hrshd1eux.attendly.data.local.entity.TimetableSessionEntity

@Database(
    entities = [
        SubjectEntity::class,
        TimetableSessionEntity::class,
        ClassSessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AttendlyDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun timetableSessionDao(): TimetableSessionDao
    abstract fun classSessionDao(): ClassSessionDao

    companion object {
        const val DATABASE_NAME = "attendly.db"
    }
}
