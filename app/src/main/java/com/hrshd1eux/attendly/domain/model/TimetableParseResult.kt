package com.hrshd1eux.attendly.domain.model

import com.hrshd1eux.attendly.core.model.DayOfWeek

data class ParsedTimetableEntry(
    val dayOfWeek: DayOfWeek,
    val startMinute: Int,
    val endMinute: Int,
    val subjectName: String,
    val location: String = "",
    val notes: String = "",
    val hasConflict: Boolean = false,
    val conflictReason: String? = null
)

data class TimetableParseResult(
    val entries: List<ParsedTimetableEntry>,
    val warnings: List<String>,
    val unrecognizedLines: List<String>,
    val comments: List<String>
) {
    val totalSessions: Int get() = entries.size
    val hasErrors: Boolean get() = entries.isEmpty() && unrecognizedLines.isNotEmpty()
}
