package com.hrshd1eux.attendly.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrshd1eux.attendly.BuildConfig
import com.hrshd1eux.attendly.core.model.AppTheme
import com.hrshd1eux.attendly.core.model.AttendanceUnitPolicy
import com.hrshd1eux.attendly.core.model.DayOfWeek
import com.hrshd1eux.attendly.core.model.TimeFormat
import com.hrshd1eux.attendly.data.preferences.PreferencesManager
import com.hrshd1eux.attendly.data.preferences.UserPreferences
import com.hrshd1eux.attendly.domain.repository.BackupRestoreRepository
import com.hrshd1eux.attendly.domain.repository.BackupValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

sealed class UpdateCheckState {
    object Idle : UpdateCheckState()
    object Checking : UpdateCheckState()
    data class UpdateAvailable(
        val latestVersion: String,
        val currentVersion: String,
        val releaseNotes: String,
        val downloadUrl: String,
        val releasePageUrl: String
    ) : UpdateCheckState()
    data class UpToDate(val version: String) : UpdateCheckState()
    data class Error(val message: String, val releasePageUrl: String) : UpdateCheckState()
}

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val isEditNameDialogOpen: Boolean = false,
    val isExplanationDialogOpen: Boolean = false,
    val isResetConfirmDialogOpen: Boolean = false,
    val backupValidationResult: BackupValidationResult? = null,
    val pendingRestoreJson: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val backupRestoreRepository: BackupRestoreRepository
) : ViewModel() {

    private val _isEditNameDialogOpen = MutableStateFlow(false)
    private val _isExplanationDialogOpen = MutableStateFlow(false)
    private val _isResetConfirmDialogOpen = MutableStateFlow(false)
    private val _backupValidationResult = MutableStateFlow<BackupValidationResult?>(null)
    private val _pendingRestoreJson = MutableStateFlow<String?>(null)
    private val _updateCheckState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)

    val uiState: StateFlow<UserPreferences> = preferencesManager.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    val isEditNameDialogOpen = _isEditNameDialogOpen.asStateFlow()
    val isExplanationDialogOpen = _isExplanationDialogOpen.asStateFlow()
    val isResetConfirmDialogOpen = _isResetConfirmDialogOpen.asStateFlow()
    val backupValidationResult = _backupValidationResult.asStateFlow()
    val updateCheckState = _updateCheckState.asStateFlow()

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            preferencesManager.setAppTheme(theme)
        }
    }

    fun setTimeFormat(format: TimeFormat) {
        viewModelScope.launch {
            preferencesManager.setTimeFormat(format)
        }
    }

    fun setFirstDayOfWeek(day: DayOfWeek) {
        viewModelScope.launch {
            preferencesManager.setFirstDayOfWeek(day)
        }
    }

    fun setDefaultTarget(target: Int) {
        viewModelScope.launch {
            preferencesManager.setDefaultTarget(target)
        }
    }

    fun setLabUnitPolicy(policy: AttendanceUnitPolicy) {
        viewModelScope.launch {
            preferencesManager.setDefaultLabUnitPolicy(policy)
        }
    }

    fun setUserName(name: String) {
        viewModelScope.launch {
            preferencesManager.setUserName(name)
            _isEditNameDialogOpen.value = false
        }
    }

    fun openEditNameDialog() {
        _isEditNameDialogOpen.value = true
    }

    fun closeEditNameDialog() {
        _isEditNameDialogOpen.value = false
    }

    fun openExplanationDialog() {
        _isExplanationDialogOpen.value = true
    }

    fun closeExplanationDialog() {
        _isExplanationDialogOpen.value = false
    }

    fun openResetConfirmDialog() {
        _isResetConfirmDialogOpen.value = true
    }

    fun closeResetConfirmDialog() {
        _isResetConfirmDialogOpen.value = false
    }

    fun resetAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            backupRestoreRepository.resetAllData()
            _isResetConfirmDialogOpen.value = false
            onComplete()
        }
    }

    suspend fun getExportBackupJson(): String {
        val json = backupRestoreRepository.createBackupJson()
        preferencesManager.setLastBackupTimestamp(System.currentTimeMillis())
        return json
    }

    suspend fun getExportAttendanceCsv(): String {
        return backupRestoreRepository.exportAttendanceCsv()
    }

    fun validateRestoreFile(jsonContent: String) {
        viewModelScope.launch {
            val result = backupRestoreRepository.validateBackupJson(jsonContent)
            _backupValidationResult.value = result
            _pendingRestoreJson.value = jsonContent
        }
    }

    fun confirmRestore(replaceExisting: Boolean, onComplete: () -> Unit) {
        val json = _pendingRestoreJson.value ?: return
        viewModelScope.launch {
            backupRestoreRepository.restoreBackupJson(json, replaceExisting)
            _backupValidationResult.value = null
            _pendingRestoreJson.value = null
            onComplete()
        }
    }

    fun cancelRestore() {
        _backupValidationResult.value = null
        _pendingRestoreJson.value = null
    }

    fun checkForUpdates() {
        viewModelScope.launch(Dispatchers.IO) {
            _updateCheckState.value = UpdateCheckState.Checking
            val currentVersion = BuildConfig.VERSION_NAME.removePrefix("v").trim()
            val releasePageUrl = "https://github.com/HrshD1eux/attendly-attendence-manager/releases"

            try {
                val url = URL("https://api.github.com/repos/HrshD1eux/attendly-attendence-manager/releases/latest")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.connectTimeout = 6000
                conn.readTimeout = 6000

                val code = conn.responseCode
                if (code == 200) {
                    val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(jsonStr)
                    val tagName = json.optString("tag_name", "").removePrefix("v").trim()
                    val body = json.optString("body", "Bug fixes and performance improvements.")
                    val htmlUrl = json.optString("html_url", releasePageUrl)

                    var apkDownloadUrl = htmlUrl
                    val assets = json.optJSONArray("assets")
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.optString("name", "")
                            if (name.endsWith(".apk", ignoreCase = true)) {
                                apkDownloadUrl = asset.optString("browser_download_url", htmlUrl)
                                break
                            }
                        }
                    }

                    if (isRemoteVersionNewer(tagName, currentVersion)) {
                        _updateCheckState.value = UpdateCheckState.UpdateAvailable(
                            latestVersion = tagName,
                            currentVersion = currentVersion,
                            releaseNotes = body,
                            downloadUrl = apkDownloadUrl,
                            releasePageUrl = htmlUrl
                        )
                    } else {
                        _updateCheckState.value = UpdateCheckState.UpToDate(version = currentVersion)
                    }
                } else if (code == 404) {
                    _updateCheckState.value = UpdateCheckState.UpToDate(version = currentVersion)
                } else {
                    _updateCheckState.value = UpdateCheckState.Error(
                        message = "GitHub responded with status $code",
                        releasePageUrl = releasePageUrl
                    )
                }
                conn.disconnect()
            } catch (e: Exception) {
                _updateCheckState.value = UpdateCheckState.Error(
                    message = e.localizedMessage ?: "Unable to connect to GitHub",
                    releasePageUrl = releasePageUrl
                )
            }
        }
    }

    fun dismissUpdateDialog() {
        _updateCheckState.value = UpdateCheckState.Idle
    }

    private fun isRemoteVersionNewer(remote: String, current: String): Boolean {
        val rParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val cParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(rParts.size, cParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
