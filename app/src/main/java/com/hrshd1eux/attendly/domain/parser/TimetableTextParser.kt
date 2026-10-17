package com.hrshd1eux.attendly.domain.parser

import com.hrshd1eux.attendly.core.model.DayOfWeek
import com.hrshd1eux.attendly.core.util.TimeUtils
import com.hrshd1eux.attendly.domain.model.ParsedTimetableEntry
import com.hrshd1eux.attendly.domain.model.TimetableParseResult

object TimetableTextParser {

    fun parse(rawText: String): TimetableParseResult {
        val entries = mutableListOf<ParsedTimetableEntry>()
        val warnings = mutableListOf<String>()
        val unrecognizedLines = mutableListOf<String>()
        val comments = mutableListOf<String>()

        var currentDay: DayOfWeek? = null

        val lines = rawText.lines()

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            // Check metadata headers
            if (line.startsWith("TIMETABLE", ignoreCase = true) ||
                line.startsWith("FORMAT_VERSION", ignoreCase = true)) {
                continue
            }

            // Check comments
            if (line.startsWith("COMMENT:", ignoreCase = true) || line.startsWith("#") || line.startsWith("//")) {
                comments.add(line)
                continue
            }

            // Check Day heading: "DAY: MONDAY", "DAY:MONDAY", "MONDAY:", "MONDAY"
            val dayCandidate = when {
                line.startsWith("DAY:", ignoreCase = true) -> line.substringAfter(":").trim()
                line.endsWith(":") -> line.dropLast(1).trim()
                else -> line
            }

            val parsedDay = DayOfWeek.parse(dayCandidate)
            if (parsedDay != null) {
                currentDay = parsedDay
                continue
            }

            // Check class entry: e.g. "09:00-10:00 | Mathematics | Room 101"
            if (currentDay == null) {
                unrecognizedLines.add(line)
                continue
            }

            val entry = parseLineEntry(line, currentDay)
            if (entry != null) {
                entries.add(entry)
            } else {
                unrecognizedLines.add(line)
            }
        }

        // Post-process: Check for overlapping entries on each day
        val processedEntries = mutableListOf<ParsedTimetableEntry>()
        val entriesByDay = entries.groupBy { it.dayOfWeek }

        for ((day, dayEntries) in entriesByDay) {
            val sorted = dayEntries.sortedBy { it.startMinute }
            for (i in sorted.indices) {
                val current = sorted[i]
                var hasOverlap = false
                var conflictMsg: String? = null

                // Check overlap with next entries
                for (j in sorted.indices) {
                    if (i == j) continue
                    val other = sorted[j]
                    // Overlap if start < other.end && end > other.start
                    if (current.startMinute < other.endMinute && current.endMinute > other.startMinute) {
                        hasOverlap = true
                        conflictMsg = "Overlaps with ${other.subjectName} (${TimeUtils.minutesToTimeString(other.startMinute)} - ${TimeUtils.minutesToTimeString(other.endMinute)})"
                        warnings.add("${day.displayName}: ${current.subjectName} overlaps with ${other.subjectName}")
                        break
                    }
                }

                processedEntries.add(
                    current.copy(
                        hasConflict = hasOverlap,
                        conflictReason = conflictMsg
                    )
                )
            }
        }

        return TimetableParseResult(
            entries = processedEntries,
            warnings = warnings,
            unrecognizedLines = unrecognizedLines,
            comments = comments
        )
    }

    private fun parseLineEntry(line: String, day: DayOfWeek): ParsedTimetableEntry? {
        // Supported delimiters: "|" or tabs
        val parts = if (line.contains("|")) {
            line.split("|").map { it.trim() }
        } else if (line.contains("\t")) {
            line.split("\t").map { it.trim() }
        } else {
            // Attempt to split by time range and remainder
            val timeMatch = Regex("""^(\d{1,2}:\d{2}\s*(?:AM|PM)?)\s*[-–—]\s*(\d{1,2}:\d{2}\s*(?:AM|PM)?)\s+(.*)$""", RegexOption.IGNORE_CASE).find(line)
            if (timeMatch != null) {
                val (start, end, remainder) = timeMatch.destructured
                listOf("$start - $end", remainder)
            } else {
                return null
            }
        }

        if (parts.isEmpty()) return null

        // Part 0 is time range: "09:00-10:00" or "09:00 - 10:00"
        val timeRangePart = parts[0]
        val timeParts = timeRangePart.split(Regex("""\s*[-–—]\s*"""))
        if (timeParts.size < 2) return null

        val startMin = TimeUtils.parseTimeToMinutes(timeParts[0]) ?: return null
        val endMin = TimeUtils.parseTimeToMinutes(timeParts[1]) ?: return null

        if (endMin <= startMin) {
            // End time must be later than start time
            return null
        }

        val subjectName = if (parts.size > 1) parts[1].trim() else ""
        if (subjectName.isEmpty()) return null

        val location = if (parts.size > 2) parts[2].trim() else ""
        val notes = if (parts.size > 3) parts[3].trim() else ""

        return ParsedTimetableEntry(
            dayOfWeek = day,
            startMinute = startMin,
            endMinute = endMin,
            subjectName = subjectName,
            location = location,
            notes = notes
        )
    }
}
