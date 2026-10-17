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
        val (processedEntries, conflictWarnings) = checkConflicts(entries)
        warnings.addAll(conflictWarnings)

        val detectedGroups = entries.mapNotNull { it.group }.distinct().sorted()

        return TimetableParseResult(
            entries = processedEntries,
            warnings = warnings.distinct(),
            unrecognizedLines = unrecognizedLines,
            comments = comments,
            detectedGroups = detectedGroups
        )
    }

    /**
     * Checks for overlapping entries on each day and returns updated entries with conflict flags and warnings.
     */
    fun checkConflicts(entries: List<ParsedTimetableEntry>): Pair<List<ParsedTimetableEntry>, List<String>> {
        val processedEntries = mutableListOf<ParsedTimetableEntry>()
        val warnings = mutableListOf<String>()
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
        return Pair(processedEntries, warnings)
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

        var rawLocation = ""
        var rawNotes = extractedComment ?: ""
        var extractedTeacher: String? = extractTeacher(extractedComment)

        if (parts.size == 3) {
            val part2 = parts[2].trim()
            val teacherInPart = extractTeacher(part2)
            if (teacherInPart != null && !isLocationToken(part2)) {
                extractedTeacher = teacherInPart
                rawNotes = if (rawNotes.isNotEmpty()) "$teacherInPart; $rawNotes" else teacherInPart
            } else if (isLocationToken(part2) || (!isTeacherToken(part2) && !part2.contains("not specified", ignoreCase = true))) {
                rawLocation = part2
            }
        } else if (parts.size >= 4) {
            val part2 = parts[2].trim()
            val part3 = parts[3].trim()

            if (isLocationToken(part3) && (isTeacherToken(part2) || !isLocationToken(part2))) {
                rawLocation = part3
                val t = extractTeacher(part2) ?: part2
                extractedTeacher = t
                rawNotes = if (rawNotes.isNotEmpty()) "$t; $rawNotes" else t
            } else {
                rawLocation = if (isLocationToken(part2) || !isTeacherToken(part2)) part2 else ""
                val t = extractTeacher(part3) ?: if (!isLocationToken(part3)) part3 else null
                if (t != null) {
                    extractedTeacher = t
                    rawNotes = if (rawNotes.isNotEmpty()) "$t; $rawNotes" else t
                }
            }
        }

        // Clean location if it contains placeholder or uncertain text
        val location = if (rawLocation.contains("not specified", ignoreCase = true) ||
            rawLocation.contains("uncertain", ignoreCase = true) ||
            rawLocation.contains("unknown", ignoreCase = true)) {
            ""
        } else {
            rawLocation
        }

        val teacherFinal = extractedTeacher ?: extractTeacher(rawNotes) ?: ""
        val notesFinal = if (teacherFinal.isNotEmpty() && (rawNotes.isBlank() || rawNotes.contains("UNCERTAIN", ignoreCase = true))) {
            teacherFinal
        } else {
            rawNotes
        }

        val detectedGroup = TimetableGroupDetector.extractGroup(subjectName)
            ?: TimetableGroupDetector.extractGroup(location)
            ?: (if (extractedComment != null) TimetableGroupDetector.extractGroup(extractedComment) else null)

        val entry = ParsedTimetableEntry(
            dayOfWeek = day,
            startMinute = startMin,
            endMinute = endMin,
            subjectName = subjectName,
            location = location,
            notes = notesFinal,
            group = detectedGroup,
            teacher = teacherFinal
        )

        val warningMsg = if (warning != null) "$subjectName: $warning" else null

        return ParsedLineResult(
            entry = entry,
            warning = warningMsg,
            comment = extractedComment
        )
    }

    /**
     * Extracts teacher or instructor name from notes or timetable comments.
     * e.g. "Guest instructor Pallavi", "Instructor Dr. Navdeep Mor", "Teacher: Dr. Smith", "Prof. Sharma"
     */
    fun extractTeacher(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val clean = text.trim()

        // 1. Matches phrases like:
        // "Guest instructor Pallavi", "Instructor Dr. Navdeep Mor", "Teacher: Dr. Smith", "Faculty: Er. Rohit"
        val prefixPattern = Regex(
            """(?i)(?:guest\s+instructor|instructor|teacher|faculty)\s*[:\-–]?\s*([A-Za-z0-9.\s]+?)(?=\s*[;,|]|\s*[-–]\s*(?:room|location)|\s+(?:room|location)\b|\.\s*$|$)"""
        )
        prefixPattern.find(clean)?.let { match ->
            val candidate = match.groupValues[1].trim().trimEnd('.')
            if (candidate.isNotEmpty() && !candidate.equals("UNCERTAIN", ignoreCase = true)) {
                val isGuest = clean.contains("guest", ignoreCase = true)
                return if (isGuest && !candidate.contains("guest", ignoreCase = true)) {
                    "Guest: $candidate"
                } else {
                    candidate
                }
            }
        }

        // 2. Matches titles: "Dr. Navdeep Mor", "Prof. Sharma", "Dr. H. C. Verma", "Er. Rohit", "Mr. Smith", etc.
        val titlePattern = Regex(
            """(?i)\b(?:Dr\.|Prof\.|Professor|Er\.|Mr\.|Ms\.|Mrs\.)\s+[A-Za-z0-9.\s]+?(?=\s*[;,|]|\s*[-–]\s*(?:room|location)|\s+(?:room|location)\b|\.\s*$|$)"""
        )
        titlePattern.find(clean)?.let { match ->
            val candidate = match.value.trim().trimEnd('.')
            if (candidate.isNotEmpty()) {
                return candidate
            }
        }

        return null
    }

    fun isLocationToken(token: String): Boolean {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) return false
        val lower = trimmed.lowercase()
        if (lower.contains("not specified") || lower.contains("uncertain") || lower.contains("unknown") || lower.contains("tbd")) return false
        if (Regex("""^(?:lab|lh|tb|room|r|c|lt|hall|block)[-–\s]?\d+.*""", RegexOption.IGNORE_CASE).matches(trimmed)) return true
        if (Regex("""^\d{2,4}[A-Za-z]?$""").matches(trimmed)) return true
        if (lower.contains("lab") || lower.contains("room") || lower.contains("hall") || lower.contains("floor") || lower.contains("building")) return true
        return false
    }

    fun isTeacherToken(token: String): Boolean {
        return extractTeacher(token) != null
    }
}
