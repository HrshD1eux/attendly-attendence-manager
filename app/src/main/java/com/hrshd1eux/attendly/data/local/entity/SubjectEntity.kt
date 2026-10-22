package com.hrshd1eux.attendly.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hrshd1eux.attendly.domain.model.Subject
import java.time.LocalDate

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val shortCode: String = "",
    val colorKey: String = "EMERALD",
    val targetPercentage: Int = 75,
    val location: String = "",
    val semesterStartDate: String? = null,
    val semesterEndDate: String? = null,
    val initialPresent: Int = 0,
    val initialAbsent: Int = 0,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Subject {
        return Subject(
            id = id,
            name = name,
            shortCode = shortCode,
            colorKey = colorKey,
            targetPercentage = targetPercentage,
            location = location,
            semesterStartDate = semesterStartDate?.let { LocalDate.parse(it) },
            semesterEndDate = semesterEndDate?.let { LocalDate.parse(it) },
            initialPresent = initialPresent,
            initialAbsent = initialAbsent,
            isArchived = isArchived,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(subject: Subject): SubjectEntity {
            return SubjectEntity(
                id = subject.id,
                name = subject.name,
                shortCode = subject.shortCode,
                colorKey = subject.colorKey,
                targetPercentage = subject.targetPercentage,
                location = subject.location,
                semesterStartDate = subject.semesterStartDate?.toString(),
                semesterEndDate = subject.semesterEndDate?.toString(),
                initialPresent = subject.initialPresent,
                initialAbsent = subject.initialAbsent,
                isArchived = subject.isArchived,
                createdAt = subject.createdAt,
                updatedAt = subject.updatedAt
            )
        }
    }
}
