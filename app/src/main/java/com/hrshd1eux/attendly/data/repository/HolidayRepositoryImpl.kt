package com.hrshd1eux.attendly.data.repository

import com.hrshd1eux.attendly.data.local.dao.HolidayDao
import com.hrshd1eux.attendly.data.local.entity.HolidayEntity
import com.hrshd1eux.attendly.domain.model.Holiday
import com.hrshd1eux.attendly.domain.repository.HolidayRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HolidayRepositoryImpl @Inject constructor(
    private val holidayDao: HolidayDao
) : HolidayRepository {

    override fun getAllHolidays(): Flow<List<Holiday>> {
        return holidayDao.getAllHolidays().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getUpcomingHolidays(fromDate: LocalDate): Flow<List<Holiday>> {
        return holidayDao.getUpcomingHolidays(fromDate.toString()).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getHolidayForDate(date: LocalDate): Flow<Holiday?> {
        return holidayDao.getHolidayForDate(date.toString()).map { it?.toDomain() }
    }

    override suspend fun getHolidayForDateSync(date: LocalDate): Holiday? {
        return holidayDao.getHolidayForDateSync(date.toString())?.toDomain()
    }

    override suspend fun getAllHolidaysSync(): List<Holiday> {
        return holidayDao.getAllHolidaysSync().map { it.toDomain() }
    }

    override suspend fun addHoliday(date: LocalDate, name: String): Long {
        val entity = HolidayEntity(
            date = date.toString(),
            name = name.trim().ifEmpty { "Holiday" }
        )
        return holidayDao.insertHoliday(entity)
    }

    override suspend fun addHolidays(holidays: List<Holiday>) {
        val entities = holidays.map { HolidayEntity.fromDomain(it) }
        holidayDao.insertHolidays(entities)
    }

    override suspend fun deleteHoliday(id: Long) {
        holidayDao.deleteHolidayById(id)
    }

    override suspend fun deleteHolidayByDate(date: LocalDate) {
        holidayDao.deleteHolidayByDate(date.toString())
    }

    override suspend fun clearAllHolidays() {
        holidayDao.clearAllHolidays()
    }

    override suspend fun seedDefaultNationalHolidays(overwrite: Boolean) {
        if (overwrite) {
            holidayDao.clearAllHolidays()
        }
        val existingDates = holidayDao.getAllHolidaysSync().map { it.date }.toSet()
        val toInsert = com.hrshd1eux.attendly.domain.model.DefaultHolidays.LIST
            .filter { !existingDates.contains(it.date.toString()) }
            .map { HolidayEntity.fromDomain(it) }
        if (toInsert.isNotEmpty()) {
            holidayDao.insertHolidays(toInsert)
        }
    }
}
