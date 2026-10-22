package com.hrshd1eux.attendly.domain.repository

data class BackupValidationResult(
    val isValid: Boolean,
    val version: Int,
    val subjectCount: Int,
    val timetableSessionCount: Int,
    val classSessionCount: Int,
    val errorMessage: String? = null
)

interface BackupRestoreRepository {
    suspend fun createBackupJson(): String
    suspend fun validateBackupJson(jsonString: String): BackupValidationResult
    suspend fun restoreBackupJson(jsonString: String, replaceExisting: Boolean): Boolean
    suspend fun exportAttendanceCsv(): String
    suspend fun resetAllData()
}
