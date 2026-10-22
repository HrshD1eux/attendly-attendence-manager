package com.hrshd1eux.attendly.core.model

enum class AttendanceStatus {
    PRESENT,
    ABSENT,
    CANCELLED,
    ON_DUTY,
    MEDICAL_LEAVE
}

enum class OnDutyPolicy(val title: String, val description: String) {
    COUNT_AS_PRESENT("Count as Present", "On-Duty attendance is credited as attended classes"),
    EXCUSED("Excused (Neutral)", "On-Duty classes are exempted from total conducted count")
}

enum class MedicalLeavePolicy(val title: String, val description: String) {
    EXCUSED("Excused (Neutral)", "Medical leaves are excluded from total classes so % is not hurt"),
    LOWER_TARGET_THRESHOLD("Concession Target", "Applies a relaxed attendance requirement (e.g. 65%) for exam eligibility")
}

enum class AttendanceUnitPolicy {
    ONE_PER_SESSION,
    ONE_PER_HOUR
}

enum class DayOfWeek(val displayName: String, val shortName: String, val isoValue: Int) {
    MONDAY("Monday", "Mon", 1),
    TUESDAY("Tuesday", "Tue", 2),
    WEDNESDAY("Wednesday", "Wed", 3),
    THURSDAY("Thursday", "Thu", 4),
    FRIDAY("Friday", "Fri", 5),
    SATURDAY("Saturday", "Sat", 6),
    SUNDAY("Sunday", "Sun", 7);

    companion object {
        fun fromIsoValue(value: Int): DayOfWeek {
            return entries.find { it.isoValue == value } ?: MONDAY
        }

        fun fromJavaDayOfWeek(javaDay: java.time.DayOfWeek): DayOfWeek {
            return fromIsoValue(javaDay.value)
        }

        fun parse(name: String): DayOfWeek? {
            val upper = name.trim().uppercase()
            return entries.find {
                it.name == upper ||
                it.displayName.uppercase() == upper ||
                it.shortName.uppercase() == upper
            }
        }
    }
}

enum class TimeFormat {
    FORMAT_12H,
    FORMAT_24H
}

enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK
}
