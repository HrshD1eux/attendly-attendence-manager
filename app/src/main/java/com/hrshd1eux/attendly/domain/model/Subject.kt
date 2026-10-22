package com.hrshd1eux.attendly.domain.model

import java.time.LocalDate

data class Subject(
    val id: Long = 0,
    val name: String,
    val shortCode: String = "",
    val colorKey: String = "EMERALD", // Preset color key
    val targetPercentage: Int = 75,
    val location: String = "",
    val semesterStartDate: LocalDate? = null,
    val semesterEndDate: LocalDate? = null,
    val initialPresent: Int = 0,
    val initialAbsent: Int = 0,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
