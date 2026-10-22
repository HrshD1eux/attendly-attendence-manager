package com.hrshd1eux.attendly.domain.model

import java.time.LocalDate

data class Holiday(
    val id: Long = 0,
    val date: LocalDate,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
