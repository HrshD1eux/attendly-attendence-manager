package com.hrshd1eux.attendly.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrshd1eux.attendly.core.model.AppTheme
import com.hrshd1eux.attendly.core.model.AttendanceUnitPolicy
import com.hrshd1eux.attendly.core.model.DayOfWeek
import com.hrshd1eux.attendly.core.model.TimeFormat
import com.hrshd1eux.attendly.data.preferences.PreferencesManager
import com.hrshd1eux.attendly.data.preferences.UserPreferences
import com.hrshd1eux.attendly.domain.repository.BackupRestoreRepository
import com.hrshd1eux.attendly.domain.repository.BackupValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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
}
