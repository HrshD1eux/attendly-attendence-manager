package com.hrshd1eux.attendly.domain.model

import com.hrshd1eux.attendly.core.model.DayOfWeek
import com.hrshd1eux.attendly.domain.parser.TimetableGroupDetector
import com.hrshd1eux.attendly.domain.parser.TimetableTextParser

data class ParsedTimetableEntry(
    val dayOfWeek: DayOfWeek,
    val startMinute: Int,
    val endMinute: Int,
    val subjectName: String,
    val location: String = "",
    val notes: String = "",
    val hasConflict: Boolean = false,
    val conflictReason: String? = null,
    val group: String? = null,
    val teacher: String = ""
)

data class TimetableParseResult(
    val entries: List<ParsedTimetableEntry>,
    val warnings: List<String>,
    val unrecognizedLines: List<String>,
    val comments: List<String>,
    val detectedGroups: List<String> = emptyList()
) {
    val totalSessions: Int get() = entries.size
    val hasErrors: Boolean get() = entries.isEmpty() && unrecognizedLines.isNotEmpty()
    val hasMultipleGroups: Boolean get() = detectedGroups.size > 1

    /**
     * Filters entries for a specific group.
     * When targetGroup is specified (e.g. "G1"):
     * - Keeps entries belonging to targetGroup
     * - Keeps entries with no group (common lectures for everyone)
     * - Ignores all other groups (e.g. G2, G3)
     * - Re-evaluates schedule conflicts for the active entries only
     */
    fun filterByGroup(targetGroup: String?, cleanSubjectNames: Boolean = false): TimetableParseResult {
        val isAllGroups = targetGroup == null || targetGroup.equals("ALL", ignoreCase = true)
        
        val filtered = if (isAllGroups) {
            entries
        } else {
            entries.filter { it.group == null || it.group.equals(targetGroup, ignoreCase = true) }
        }

        val processed = if (cleanSubjectNames) {
            filtered.map { it.copy(subjectName = TimetableGroupDetector.cleanSubjectName(it.subjectName)) }
        } else {
            filtered
        }

        val (recheckedEntries, newConflictWarnings) = TimetableTextParser.checkConflicts(processed)
        val nonConflictWarnings = warnings.filterNot { it.contains("overlaps with", ignoreCase = true) }
        val allWarnings = (nonConflictWarnings + newConflictWarnings).distinct()

        return this.copy(
            entries = recheckedEntries,
            warnings = allWarnings,
            detectedGroups = detectedGroups
        )
    }
}
