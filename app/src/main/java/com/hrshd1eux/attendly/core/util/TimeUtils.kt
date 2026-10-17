package com.hrshd1eux.attendly.core.util

import com.hrshd1eux.attendly.core.model.DayOfWeek
import com.hrshd1eux.attendly.core.model.TimeFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeUtils {

    fun minutesToTimeString(minutes: Int, format: TimeFormat = TimeFormat.FORMAT_12H): String {
        val totalMins = ((minutes % 1440) + 1440) % 1440
        val hour = totalMins / 60
        val min = totalMins % 60

        return if (format == TimeFormat.FORMAT_24H) {
            String.format(Locale.US, "%02d:%02d", hour, min)
        } else {
            val period = if (hour < 12) "AM" else "PM"
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            String.format(Locale.US, "%d:%02d %s", displayHour, min, period)
        }
    }

    fun parseTimeToMinutes(timeStr: String): Int? {
        val trimmed = timeStr.trim().uppercase()
        // Try HH:mm
        val colonParts = trimmed.split(":")
        if (colonParts.size == 2) {
            val hourPart = colonParts[0].trim().toIntOrNull() ?: return null
            val secondPart = colonParts[1].trim()

            if (secondPart.contains("AM") || secondPart.contains("PM")) {
                val isPm = secondPart.contains("PM")
                val minDigits = secondPart.replace("AM", "").replace("PM", "").trim().toIntOrNull() ?: return null
                var hour = hourPart
                if (isPm && hour < 12) hour += 12
                if (!isPm && hour == 12) hour = 0
                return hour * 60 + minDigits
            } else {
                val min = secondPart.toIntOrNull() ?: return null
                if (hourPart in 0..23 && min in 0..59) {
                    return hourPart * 60 + min
                }
            }
        }
        return null
    }

    fun formatDuration(startMinute: Int, endMinute: Int): String {
        val diff = if (endMinute >= startMinute) endMinute - startMinute else (1440 - startMinute) + endMinute
        val hours = diff / 60
        val mins = diff % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }

    fun calculateDurationHours(startMinute: Int, endMinute: Int): Int {
        val diff = if (endMinute >= startMinute) endMinute - startMinute else (1440 - startMinute) + endMinute
        val hours = (diff + 30) / 60 // Round to nearest hour
        return if (hours < 1) 1 else hours
    }

    fun formatDate(date: LocalDate, pattern: String = "EEE, MMM d"): String {
        val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
        return date.format(formatter)
    }

    fun formatMonthYear(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
        return date.format(formatter)
    }

    fun getDayOfWeek(date: LocalDate): DayOfWeek {
        return DayOfWeek.fromJavaDayOfWeek(date.dayOfWeek)
    }
}
