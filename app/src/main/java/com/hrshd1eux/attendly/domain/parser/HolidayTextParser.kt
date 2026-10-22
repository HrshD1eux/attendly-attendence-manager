package com.hrshd1eux.attendly.domain.parser

import com.hrshd1eux.attendly.domain.model.Holiday
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

data class HolidayParseResult(
    val holidays: List<Holiday>,
    val rangeCount: Int,
    val singleCount: Int,
    val errors: List<String>
)

object HolidayTextParser {

    private val DATE_FORMATTERS = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE, // yyyy-MM-dd
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("d/M/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("d-M-yyyy"),
        DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("MMMM d yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("MMM d yyyy", Locale.ENGLISH)
    )

    private val ORDINAL_REGEX = Regex("(?<=\\d)(st|nd|rd|th)", RegexOption.IGNORE_CASE)
    private val BULLET_PREFIX_REGEX = Regex("^\\s*([*\\-•–—]|\\d+[.)])\\s*")

    fun parse(text: String): HolidayParseResult {
        if (text.isBlank()) {
            return HolidayParseResult(emptyList(), 0, 0, emptyList())
        }

        val lines = text.lines()
        val holidaysMap = mutableMapOf<LocalDate, String>() // date -> name
        val errors = mutableListOf<String>()
        var rangeCount = 0
        var singleCount = 0

        for ((index, rawLine) in lines.withIndex()) {
            val lineNumber = index + 1
            var line = rawLine.trim()

            // Skip empty lines or header metadata
            if (line.isEmpty() ||
                line.startsWith("#") ||
                line.startsWith("//") ||
                line.equals("HOLIDAYS", ignoreCase = true) ||
                line.startsWith("FORMAT_VERSION:", ignoreCase = true) ||
                line.startsWith("```")
            ) {
                continue
            }

            // Remove markdown bullets or numbers like "* ", "- ", "1. "
            line = line.replace(BULLET_PREFIX_REGEX, "").trim()

            // Remove markdown bold/italics wrappers
            line = line.replace("**", "").replace("__", "").trim()

            // Separate Date part and Holiday Name part
            // Possible separators: " | ", "|", " : ", ":", " - "
            val parts = splitDateAndName(line)
            if (parts == null) {
                // If it's just a general comment or header, skip or ignore
                if (line.contains("holiday", ignoreCase = true) && !line.any { it.isDigit() }) {
                    continue
                }
                errors.add("Line $lineNumber: Could not separate date and holiday name: \"$rawLine\"")
                continue
            }

            val datePart = parts.first.trim()
            val holidayName = parts.second.trim()

            if (holidayName.isEmpty()) {
                errors.add("Line $lineNumber: Holiday name is empty")
                continue
            }

            // Check if datePart is a date range ("2026-10-20 to 2026-10-24" or "2026-10-20 - 2026-10-24")
            val rangeParts = splitDateRange(datePart)
            if (rangeParts != null) {
                val start = parseDate(rangeParts.first)
                val end = parseDate(rangeParts.second)

                if (start == null || end == null) {
                    errors.add("Line $lineNumber: Invalid date range \"$datePart\"")
                    continue
                }

                if (end.isBefore(start)) {
                    errors.add("Line $lineNumber: End date ($end) is before start date ($start)")
                    continue
                }

                val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(start, end)
                if (daysBetween > 90) {
                    errors.add("Line $lineNumber: Range exceeds 90 days ($start to $end), skipped")
                    continue
                }

                rangeCount++
                val nonNullStart: LocalDate = start
                val nonNullEnd: LocalDate = end
                var cur: LocalDate = nonNullStart
                while (!cur.isAfter(nonNullEnd)) {
                    holidaysMap[cur] = holidayName
                    cur = cur.plusDays(1)
                }
            } else {
                val singleDate = parseDate(datePart)
                if (singleDate == null) {
                    errors.add("Line $lineNumber: Unrecognized date format \"$datePart\"")
                    continue
                }
                singleCount++
                holidaysMap[singleDate] = holidayName
            }
        }

        val sortedHolidays = holidaysMap.entries
            .sortedBy { it.key }
            .map { (date, name) ->
                Holiday(
                    date = date,
                    name = name
                )
            }

        return HolidayParseResult(
            holidays = sortedHolidays,
            rangeCount = rangeCount,
            singleCount = singleCount,
            errors = errors
        )
    }

    private fun splitDateAndName(line: String): Pair<String, String>? {
        // Priority 1: Pipe separator "|"
        if (line.contains("|")) {
            val idx = line.indexOf("|")
            val date = line.substring(0, idx).trim()
            val name = line.substring(idx + 1).trim()
            return Pair(date, name)
        }

        // Priority 2: Colon ":"
        if (line.contains(":")) {
            val idx = line.indexOf(":")
            val date = line.substring(0, idx).trim()
            val name = line.substring(idx + 1).trim()
            // Make sure date part has digits
            if (date.any { it.isDigit() }) {
                return Pair(date, name)
            }
        }

        // Priority 3: Double dash or surrounded dash " -- " or " - " (be careful not to break ISO yyyy-MM-dd)
        val dashSeparators = listOf(" -- ", " — ", " – ", " - ")
        for (sep in dashSeparators) {
            val idx = line.lastIndexOf(sep)
            if (idx != -1) {
                val date = line.substring(0, idx).trim()
                val name = line.substring(idx + sep.length).trim()
                if (date.any { it.isDigit() } && name.isNotEmpty()) {
                    return Pair(date, name)
                }
            }
        }

        return null
    }

    private fun splitDateRange(datePart: String): Pair<String, String>? {
        val lower = datePart.lowercase()
        // " to " or " through " or " till " or " until "
        val rangeSeparators = listOf(" to ", " through ", " till ", " until ", " -- ", " – ", " — ")
        for (sep in rangeSeparators) {
            val idx = lower.indexOf(sep)
            if (idx != -1) {
                val s1 = datePart.substring(0, idx).trim()
                val s2 = datePart.substring(idx + sep.length).trim()
                return Pair(s1, s2)
            }
        }

        // Check if there is a hyphen separator between two dates, e.g. "2026-10-20 - 2026-10-24"
        if (datePart.contains(" - ")) {
            val idx = datePart.indexOf(" - ")
            val s1 = datePart.substring(0, idx).trim()
            val s2 = datePart.substring(idx + 3).trim()
            return Pair(s1, s2)
        }

        return null
    }

    private fun parseDate(rawDateStr: String): LocalDate? {
        var cleaned = rawDateStr.trim()
        cleaned = cleaned.replace(ORDINAL_REGEX, "") // remove 1st, 2nd, 3rd, 4th -> 1, 2, 3, 4
        cleaned = cleaned.replace(",", " ").replace("\\s+".toRegex(), " ").trim()

        // If user provided "24 Oct 2026" or "Oct 24 2026", let's handle commas
        for (formatter in DATE_FORMATTERS) {
            try {
                return LocalDate.parse(cleaned, formatter)
            } catch (ignored: DateTimeParseException) {
            }
        }

        // Also try with original string with comma if needed
        val withComma = rawDateStr.trim().replace(ORDINAL_REGEX, "")
        for (formatter in DATE_FORMATTERS) {
            try {
                return LocalDate.parse(withComma, formatter)
            } catch (ignored: DateTimeParseException) {
            }
        }

        return null
    }
}
