package com.hrshd1eux.attendly.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrshd1eux.attendly.data.backup.AutoBackupManager
import com.hrshd1eux.attendly.data.backup.AutoBackupSummary
import com.hrshd1eux.attendly.data.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val userName: String = "",
    val targetPercentage: Int = 75,
    val isCompleted: Boolean = false,
    val detectedBackup: AutoBackupSummary? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val autoBackupManager: AutoBackupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val backup = autoBackupManager.checkForExistingAutoBackup()
            if (backup != null) {
                _uiState.update { it.copy(detectedBackup = backup) }
            }
        }
    }

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(userName = name) }
    }

    fun onTargetChanged(target: Int) {
        _uiState.update { it.copy(targetPercentage = target.coerceIn(1, 100)) }
    }

    fun restoreDetectedBackup(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val success = autoBackupManager.restoreAutoBackup()
            if (success) {
                preferencesManager.setOnboardingCompleted(true)
                _uiState.update { it.copy(isCompleted = true) }
                onSuccess()
            }
        }
    }

    fun dismissDetectedBackup() {
        _uiState.update { it.copy(detectedBackup = null) }
    }

    fun completeOnboarding(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val name = _uiState.value.userName.trim().ifEmpty { "Student" }
            preferencesManager.setUserName(name)
            preferencesManager.setDefaultTarget(_uiState.value.targetPercentage)
            preferencesManager.setOnboardingCompleted(true)
            _uiState.update { it.copy(isCompleted = true) }
            onSuccess()
        }
    }
}
