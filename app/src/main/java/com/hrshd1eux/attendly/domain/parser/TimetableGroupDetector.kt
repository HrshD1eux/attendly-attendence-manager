package com.hrshd1eux.attendly.domain.parser

object TimetableGroupDetector {

    // Regex to detect group designations:
    // 1. Group 1, Group-1, Group A, Batch 2, Batch-B, Gr. 1, Gr-2
    // 2. G1, G-1, G 1, G.1, G_1, G2, G-2, G3, G-3...
    // 3. G-A, G A, G.B (delimiter required for letter groups to avoid matching common words)
    private val GROUP_DETECTION_REGEX = Regex(
        """(?i)(?:\b(?:GROUP|BATCH|GR)[-.\s]*([0-9]+|[A-Za-z])\b|\bG[-.\s]*([0-9]+)\b|\bG[-.\s]+([A-Za-z])\b)"""
    )

    // Regex to strip trailing group suffixes from subject names (e.g. "DSA LAB G-1" -> "DSA LAB")
    private val TRAILING_GROUP_CLEAN_REGEX = Regex(
        """(?i)(?:\s*[-–—/(\[]\s*(?:GROUP|BATCH|GR|G)[-.\s]*[0-9A-Za-z]+[)\]]?\s*$|\s+(?:GROUP|BATCH|GR|G)[-.\s]*[0-9A-Za-z]+[)\]]?\s*$)"""
    )

    /**
     * Extracts a normalized group identifier from text, or null if no group is present.
     * Examples:
     * - "DSA LAB G-1" -> "G1"
     * - "OOPS LAB G-3" -> "G3"
     * - "Physics Lab Group 2" -> "G2"
     * - "Chemistry Lab Batch-A" -> "Group A"
     * - "Mathematics" -> null
     */
    fun extractGroup(text: String): String? {
        val match = GROUP_DETECTION_REGEX.find(text) ?: return null
        val rawValue = match.groupValues[1].ifEmpty {
            match.groupValues[2].ifEmpty {
                match.groupValues[3]
            }
        }.trim().uppercase()

        if (rawValue.isEmpty()) return null

        return if (rawValue.all { it.isDigit() }) {
            "G$rawValue"
        } else {
            "Group $rawValue"
        }
    }

    /**
     * Cleans trailing group designations from a subject name.
     * Example: "DSA LAB G-1" -> "DSA LAB", "OOPS LAB (G3)" -> "OOPS LAB", "HVPD" -> "HVPD"
     */
    fun cleanSubjectName(name: String): String {
        val cleaned = name.replace(TRAILING_GROUP_CLEAN_REGEX, "").trim()
        return if (cleaned.isNotEmpty()) cleaned else name
    }

    /**
     * Formats a group identifier for display.
     * Example: "G1" -> "Group 1 (G1)", "G2" -> "Group 2 (G2)", "Group A" -> "Group A"
     */
    fun getGroupDisplayName(groupKey: String): String {
        return if (groupKey.startsWith("G", ignoreCase = true) && groupKey.drop(1).all { it.isDigit() }) {
            "Group ${groupKey.drop(1)} ($groupKey)"
        } else {
            groupKey
        }
    }
}
