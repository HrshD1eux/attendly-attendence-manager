package com.hrshd1eux.attendly.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hrshd1eux.attendly.data.local.AttendlyDatabase
import com.hrshd1eux.attendly.data.local.dao.ClassSessionDao
import com.hrshd1eux.attendly.data.local.dao.HolidayDao
import com.hrshd1eux.attendly.data.local.dao.SubjectDao
import com.hrshd1eux.attendly.data.local.dao.TimetableSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AttendlyDatabase {
        return Room.databaseBuilder(
            context,
            AttendlyDatabase::class.java,
            AttendlyDatabase.DATABASE_NAME
        )
        .addCallback(object : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.execSQL("PRAGMA foreign_keys=ON;")
            }
        })
        .addMigrations(AttendlyDatabase.MIGRATION_1_2)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
    }

    @Provides
    fun provideSubjectDao(database: AttendlyDatabase): SubjectDao {
        return database.subjectDao()
    }

    @Provides
    fun provideTimetableSessionDao(database: AttendlyDatabase): TimetableSessionDao {
        return database.timetableSessionDao()
    }

    @Provides
    fun provideClassSessionDao(database: AttendlyDatabase): ClassSessionDao {
        return database.classSessionDao()
    }

    @Provides
    fun provideHolidayDao(database: AttendlyDatabase): HolidayDao {
        return database.holidayDao()
    }
}
