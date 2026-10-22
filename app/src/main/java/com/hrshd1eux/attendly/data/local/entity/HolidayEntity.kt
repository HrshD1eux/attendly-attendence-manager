package com.hrshd1eux.attendly.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hrshd1eux.attendly.domain.model.Holiday
import java.time.LocalDate

@Entity(
    tableName = "holidays",
    indices = [
        Index(value = ["date"], unique = true)
    ]
)
data class HolidayEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // ISO-8601 YYYY-MM-DD
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Holiday {
        return Holiday(
            id = id,
            date = LocalDate.parse(date),
            name = name,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(domain: Holiday): HolidayEntity {
            return HolidayEntity(
                id = domain.id,
                date = domain.date.toString(),
                name = domain.name,
                createdAt = domain.createdAt
            )
        }
    }
}
