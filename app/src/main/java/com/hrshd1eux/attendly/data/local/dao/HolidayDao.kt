package com.hrshd1eux.attendly.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hrshd1eux.attendly.data.local.entity.HolidayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HolidayDao {

    @Query("SELECT * FROM holidays ORDER BY date ASC")
    fun getAllHolidays(): Flow<List<HolidayEntity>>

    @Query("SELECT * FROM holidays WHERE date >= :fromDate ORDER BY date ASC")
    fun getUpcomingHolidays(fromDate: String): Flow<List<HolidayEntity>>

    @Query("SELECT * FROM holidays WHERE date = :date LIMIT 1")
    fun getHolidayForDate(date: String): Flow<HolidayEntity?>

    @Query("SELECT * FROM holidays WHERE date = :date LIMIT 1")
    suspend fun getHolidayForDateSync(date: String): HolidayEntity?

    @Query("SELECT * FROM holidays ORDER BY date ASC")
    suspend fun getAllHolidaysSync(): List<HolidayEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHoliday(holiday: HolidayEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHolidays(holidays: List<HolidayEntity>): List<Long>

    @Query("DELETE FROM holidays WHERE id = :id")
    suspend fun deleteHolidayById(id: Long)

    @Query("DELETE FROM holidays WHERE date = :date")
    suspend fun deleteHolidayByDate(date: String)

    @Query("DELETE FROM holidays")
    suspend fun clearAllHolidays()
}
