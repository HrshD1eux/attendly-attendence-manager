package com.hrshd1eux.attendly.domain.repository

import com.hrshd1eux.attendly.domain.model.Holiday
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface HolidayRepository {
    fun getAllHolidays(): Flow<List<Holiday>>
    fun getUpcomingHolidays(fromDate: LocalDate): Flow<List<Holiday>>
    fun getHolidayForDate(date: LocalDate): Flow<Holiday?>
    suspend fun getHolidayForDateSync(date: LocalDate): Holiday?
    suspend fun getAllHolidaysSync(): List<Holiday>
    suspend fun addHoliday(date: LocalDate, name: String): Long
    suspend fun addHolidays(holidays: List<Holiday>)
    suspend fun deleteHoliday(id: Long)
    suspend fun deleteHolidayByDate(date: LocalDate)
    suspend fun clearAllHolidays()
    suspend fun seedDefaultNationalHolidays(overwrite: Boolean = false)
}
