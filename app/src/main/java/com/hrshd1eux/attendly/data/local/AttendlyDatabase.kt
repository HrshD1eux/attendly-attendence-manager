package com.hrshd1eux.attendly.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hrshd1eux.attendly.data.local.dao.ClassSessionDao
import com.hrshd1eux.attendly.data.local.dao.HolidayDao
import com.hrshd1eux.attendly.data.local.dao.SubjectDao
import com.hrshd1eux.attendly.data.local.dao.TimetableSessionDao
import com.hrshd1eux.attendly.data.local.entity.ClassSessionEntity
import com.hrshd1eux.attendly.data.local.entity.HolidayEntity
import com.hrshd1eux.attendly.data.local.entity.SubjectEntity
import com.hrshd1eux.attendly.data.local.entity.TimetableSessionEntity

@Database(
    entities = [
        SubjectEntity::class,
        TimetableSessionEntity::class,
        ClassSessionEntity::class,
        HolidayEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AttendlyDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun timetableSessionDao(): TimetableSessionDao
    abstract fun classSessionDao(): ClassSessionDao
    abstract fun holidayDao(): HolidayDao

    companion object {
        const val DATABASE_NAME = "attendly.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `holidays` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_holidays_date` ON `holidays` (`date`)")
            }
        }
    }
}
