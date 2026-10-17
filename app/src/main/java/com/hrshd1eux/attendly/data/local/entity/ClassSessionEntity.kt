package com.hrshd1eux.attendly.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hrshd1eux.attendly.core.model.AttendanceStatus
import com.hrshd1eux.attendly.domain.model.ClassSession
import java.time.LocalDate

@Entity(
    tableName = "class_sessions",
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
        Index("date"),
        Index("timetableSessionId")
    ]
)
data class ClassSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long,
    val timetableSessionId: Long? = null,
    val date: String, // ISO-8601 YYYY-MM-DD
    val startMinute: Int,
    val endMinute: Int,
    val location: String = "",
    val status: String = "PRESENT", // PRESENT, ABSENT, CANCELLED
    val attendanceUnits: Int = 1,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(subject: SubjectEntity? = null): ClassSession {
        return ClassSession(
            id = id,
            subjectId = subjectId,
            subjectName = subject?.name ?: "",
            subjectShortCode = subject?.shortCode ?: "",
            subjectColorKey = subject?.colorKey ?: "EMERALD",
            timetableSessionId = timetableSessionId,
            date = LocalDate.parse(date),
            startMinute = startMinute,
            endMinute = endMinute,
            location = location.ifEmpty { subject?.location ?: "" },
            status = try {
                AttendanceStatus.valueOf(status)
            } catch (e: Exception) {
                AttendanceStatus.PRESENT
            },
            attendanceUnits = attendanceUnits,
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(session: ClassSession): ClassSessionEntity {
            return ClassSessionEntity(
                id = session.id,
                subjectId = session.subjectId,
                timetableSessionId = session.timetableSessionId,
                date = session.date.toString(),
                startMinute = session.startMinute,
                endMinute = session.endMinute,
                location = session.location,
                status = session.status.name,
                attendanceUnits = session.attendanceUnits,
                notes = session.notes,
                createdAt = session.createdAt,
                updatedAt = session.updatedAt
            )
        }
    }
}
