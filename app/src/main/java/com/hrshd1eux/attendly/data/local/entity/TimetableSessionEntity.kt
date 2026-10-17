package com.hrshd1eux.attendly.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hrshd1eux.attendly.core.model.AttendanceUnitPolicy
import com.hrshd1eux.attendly.core.model.DayOfWeek
import com.hrshd1eux.attendly.domain.model.TimetableSession

@Entity(
    tableName = "timetable_sessions",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("subjectId"),
        Index("dayOfWeek")
    ]
)
data class TimetableSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long,
    val dayOfWeek: Int, // 1..7 (Monday=1)
    val startMinute: Int,
    val endMinute: Int,
    val location: String = "",
    val attendanceUnitPolicy: String = "ONE_PER_SESSION",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(subject: SubjectEntity? = null): TimetableSession {
        return TimetableSession(
            id = id,
            subjectId = subjectId,
            subjectName = subject?.name ?: "",
            subjectShortCode = subject?.shortCode ?: "",
            subjectColorKey = subject?.colorKey ?: "EMERALD",
            dayOfWeek = DayOfWeek.fromIsoValue(dayOfWeek),
            startMinute = startMinute,
            endMinute = endMinute,
            location = location.ifEmpty { subject?.location ?: "" },
            attendanceUnitPolicy = try {
                AttendanceUnitPolicy.valueOf(attendanceUnitPolicy)
            } catch (e: Exception) {
                AttendanceUnitPolicy.ONE_PER_SESSION
            },
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(session: TimetableSession): TimetableSessionEntity {
            return TimetableSessionEntity(
                id = session.id,
                subjectId = session.subjectId,
                dayOfWeek = session.dayOfWeek.isoValue,
                startMinute = session.startMinute,
                endMinute = session.endMinute,
                location = session.location,
                attendanceUnitPolicy = session.attendanceUnitPolicy.name,
                notes = session.notes,
                createdAt = session.createdAt,
                updatedAt = session.updatedAt
            )
        }
    }
}
