package com.hrshd1eux.attendly.data.backup

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.hrshd1eux.attendly.core.util.TimeUtils
import com.hrshd1eux.attendly.data.preferences.PreferencesManager
import com.hrshd1eux.attendly.domain.repository.BackupRestoreRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class AutoBackupSummary(
    val timestamp: Long,
    val formattedDate: String,
    val subjectCount: Int,
    val timetableSessionCount: Int,
    val classSessionCount: Int,
    val filePath: String,
    val fileSizeFormatted: String
)

@Singleton
class AutoBackupManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val backupRestoreRepository: BackupRestoreRepository,
    private val preferencesManager: PreferencesManager
) {

    companion object {
        const val BACKUP_FILE_NAME = "attendly_auto_backup.json"
        const val ATTENDLY_FOLDER_NAME = "Attendly"
    }

    private fun getDownloadsBackupFile(): File {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val folder = File(downloads, ATTENDLY_FOLDER_NAME)
        if (!folder.exists()) folder.mkdirs()
        return File(folder, BACKUP_FILE_NAME)
    }

    private fun getDocumentsBackupFile(): File {
        val docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val folder = File(docs, ATTENDLY_FOLDER_NAME)
        if (!folder.exists()) folder.mkdirs()
        return File(folder, BACKUP_FILE_NAME)
    }

    /**
     * Performs an automatic backup and saves it to external storage (Downloads/Attendly/ & Documents/Attendly/).
     * This snapshot survives app uninstallation and reinstallation.
     */
    suspend fun performAutoBackup(): Result<File> = withContext(Dispatchers.IO) {
        try {
            val jsonContent = backupRestoreRepository.createBackupJson()
            if (jsonContent.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Generated backup JSON is empty"))
            }

            var savedFile: File? = null

            // 1. Write to Downloads/Attendly/
            try {
                val file = getDownloadsBackupFile()
                FileOutputStream(file).use { out ->
                    out.write(jsonContent.toByteArray(Charsets.UTF_8))
                }
                savedFile = file
            } catch (e: Exception) {
                // Ignore and try Documents
            }

            // 2. Also write to Documents/Attendly/ as secondary backup
            try {
                val docFile = getDocumentsBackupFile()
                FileOutputStream(docFile).use { out ->
                    out.write(jsonContent.toByteArray(Charsets.UTF_8))
                }
                if (savedFile == null) savedFile = docFile
            } catch (e: Exception) {
                // Ignore
            }

            // 3. Fallback to app external files dir if public directories failed
            if (savedFile == null) {
                val extDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
                val fallbackFile = File(extDir, BACKUP_FILE_NAME)
                FileOutputStream(fallbackFile).use { out ->
                    out.write(jsonContent.toByteArray(Charsets.UTF_8))
                }
                savedFile = fallbackFile
            }

            val now = System.currentTimeMillis()
            preferencesManager.setLastBackupTimestamp(now)

            Result.success(savedFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Scans device storage to check if an Attendly backup already exists.
     * Used on fresh installs/reinstalls to detect existing backups.
     */
    suspend fun checkForExistingAutoBackup(): AutoBackupSummary? = withContext(Dispatchers.IO) {
        try {
            val candidateFiles = listOf(
                getDownloadsBackupFile(),
                getDocumentsBackupFile(),
                File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), BACKUP_FILE_NAME)
            )

            for (file in candidateFiles) {
                if (file.exists() && file.isFile && file.length() > 0) {
                    val content = try {
                        FileInputStream(file).bufferedReader(Charsets.UTF_8).use { it.readText() }
                    } catch (e: Exception) {
                        null
                    }

                    if (!content.isNullOrBlank()) {
                        val validation = backupRestoreRepository.validateBackupJson(content)
                        if (validation.isValid && (validation.subjectCount > 0 || validation.classSessionCount > 0)) {
                            val modifiedTime = file.lastModified()
                            val dateTime = LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(if (modifiedTime > 0) modifiedTime else System.currentTimeMillis()),
                                ZoneId.systemDefault()
                            )
                            val formattedDate = dateTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a", Locale.getDefault()))
                            val sizeFormatted = formatFileSize(file.length())

                            return@withContext AutoBackupSummary(
                                timestamp = modifiedTime,
                                formattedDate = formattedDate,
                                subjectCount = validation.subjectCount,
                                timetableSessionCount = validation.timetableSessionCount,
                                classSessionCount = validation.classSessionCount,
                                filePath = file.absolutePath,
                                fileSizeFormatted = sizeFormatted
                            )
                        }
                    }
                }
            }

            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Restores data directly from the detected storage auto-backup.
     */
    suspend fun restoreAutoBackup(): Boolean = withContext(Dispatchers.IO) {
        try {
            val candidateFiles = listOf(
                getDownloadsBackupFile(),
                getDocumentsBackupFile(),
                File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), BACKUP_FILE_NAME)
            )

            for (file in candidateFiles) {
                if (file.exists() && file.isFile && file.length() > 0) {
                    val content = FileInputStream(file).bufferedReader(Charsets.UTF_8).use { it.readText() }
                    if (content.isNotBlank()) {
                        val success = backupRestoreRepository.restoreBackupJson(content, replaceExisting = true)
                        if (success) {
                            return@withContext true
                        }
                    }
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    fun getAutoBackupDisplayPath(): String {
        return "Downloads/Attendly/$BACKUP_FILE_NAME"
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
            else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }
}
