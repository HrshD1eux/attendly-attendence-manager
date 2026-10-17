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

        // 1. Pre-process raw text to split concatenated days and concatenated time slots
        val normalizedLines = preprocessTimetableText(rawText)

        for (rawLine in normalizedLines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            // Check metadata headers
            if (line.startsWith("TIMETABLE", ignoreCase = true) ||
                line.startsWith("FORMAT_VERSION", ignoreCase = true)) {
                continue
            }

            // Check pure comment lines
            if (line.startsWith("COMMENT:", ignoreCase = true) || line.startsWith("#") || line.startsWith("//")) {
                val commentContent = line.removePrefix("COMMENT:").removePrefix("#").removePrefix("//").trim()
                if (commentContent.isNotEmpty()) {
                    comments.add(commentContent)
                }
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

            val parseResult = parseLineEntry(line, currentDay)
            if (parseResult != null) {
                entries.add(parseResult.entry)
                if (parseResult.warning != null) {
                    warnings.add("${currentDay.displayName}: ${parseResult.warning}")
                }
                if (parseResult.comment != null) {
                    comments.add(parseResult.comment)
                }
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

                // Check overlap with other entries
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

    private fun preprocessTimetableText(raw: String): List<String> {
        var text = raw.replace("\r\n", "\n").replace("\r", "\n")

        // 1. Separate DAY: <DAY_NAME> onto its own line when followed by entries on the same line
        // e.g. "DAY: MONDAY 09:00-10:00 | OOPS" -> "\nDAY: MONDAY\n09:00-10:00 | OOPS"
        val dayPattern = Regex("""(?i)\bDAY\s*:\s*(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY|MON|TUE|WED|THU|FRI|SAT|SUN)\b""")
        text = text.replace(dayPattern) { matchResult ->
            "\nDAY: ${matchResult.groupValues[1]}\n"
        }

        // Also handle "MONDAY:", "TUESDAY:" when preceded by start of line or space
        val colonDayPattern = Regex("""(?i)(?:^|\n)\s*(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY)\s*:(?!\s*\d)""")
        text = text.replace(colonDayPattern) { matchResult ->
            "\nDAY: ${matchResult.groupValues[1]}\n"
        }

        // 2. Separate concatenated class sessions when time ranges appear mid-line:
        // If the text uses pipes, look for a time range followed by a pipe '|'
        // This avoids splitting inside comments that mention times (e.g. "spans 14:00-18:00, with...")
        val inlineTimeRangePattern = if (text.contains("|")) {
            Regex("""(?<=\S)\s+(?=\d{1,2}:\d{2}\s*(?:AM|PM|am|pm)?\s*[-–—]\s*\d{1,2}:\d{2}\s*(?:AM|PM|am|pm)?\s*\|)""")
        } else {
            Regex("""(?<=\S)\s+(?=\d{1,2}:\d{2}\s*(?:AM|PM|am|pm)?\s*[-–—]\s*\d{1,2}:\d{2}\s*(?:AM|PM|am|pm)?\s+[A-Za-z])""")
        }
        text = text.replace(inlineTimeRangePattern, "\n")

        return text.lines()
    }

    private data class ParsedLineResult(
        val entry: ParsedTimetableEntry,
        val warning: String? = null,
        val comment: String? = null
    )

    private fun parseLineEntry(line: String, day: DayOfWeek): ParsedLineResult? {
        // Extract any inline COMMENT: e.g. "16:00-18:00 | OOPS LAB | LAB-307 COMMENT: UNCERTAIN - ..."
        // or "09:00-10:00 | OOPS | COMMENT: UNCERTAIN - Room not specified."
        var cleanLine = line
        var extractedComment: String? = null
        var warning: String? = null

        if (cleanLine.contains("COMMENT:", ignoreCase = true)) {
            val idx = cleanLine.indexOf("COMMENT:", ignoreCase = true)
            extractedComment = cleanLine.substring(idx + 8).trim()
            cleanLine = cleanLine.substring(0, idx).trim().trimEnd('|').trim()

            if (extractedComment.contains("UNCERTAIN", ignoreCase = true)) {
                warning = extractedComment
            }
        }

        // Supported delimiters: "|" or tabs
        val parts = if (cleanLine.contains("|")) {
            cleanLine.split("|").map { it.trim() }
        } else if (cleanLine.contains("\t")) {
            cleanLine.split("\t").map { it.trim() }
        } else {
            // Attempt to split by time range and remainder
            val timeMatch = Regex("""^(\d{1,2}:\d{2}\s*(?:AM|PM)?)\s*[-–—]\s*(\d{1,2}:\d{2}\s*(?:AM|PM)?)\s+(.*)$""", RegexOption.IGNORE_CASE).find(cleanLine)
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
        val notes = if (parts.size > 3) parts[3].trim() else (extractedComment ?: "")

        val entry = ParsedTimetableEntry(
            dayOfWeek = day,
            startMinute = startMin,
            endMinute = endMin,
            subjectName = subjectName,
            location = location,
            notes = notes
        )

        val warningMsg = if (warning != null) "$subjectName: $warning" else null

        return ParsedLineResult(
            entry = entry,
            warning = warningMsg,
            comment = extractedComment
        )
    }
}
