package com.hrshd1eux.attendly.ui.subjects

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrshd1eux.attendly.core.model.AttendanceStatus
import com.hrshd1eux.attendly.domain.model.Subject
import com.hrshd1eux.attendly.domain.model.SubjectAttendanceDetail
import com.hrshd1eux.attendly.domain.repository.AttendanceRepository
import com.hrshd1eux.attendly.domain.repository.SubjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class SubjectDetailUiState(
    val detail: SubjectAttendanceDetail? = null,
    val isEditDialogOpen: Boolean = false,
    val isDeleteDialogOpen: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class SubjectDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val subjectRepository: SubjectRepository,
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {

    private val subjectId: Long = checkNotNull(savedStateHandle["subjectId"])

    private val _isEditDialogOpen = MutableStateFlow(false)
    private val _isDeleteDialogOpen = MutableStateFlow(false)

    val uiState: StateFlow<SubjectDetailUiState> = combine(
        attendanceRepository.getSubjectDetail(subjectId),
        _isEditDialogOpen,
        _isDeleteDialogOpen
    ) { detail, isEdit, isDelete ->
        SubjectDetailUiState(
            detail = detail,
            isEditDialogOpen = isEdit,
            isDeleteDialogOpen = isDelete,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SubjectDetailUiState(isLoading = true)
    )

    fun openEditDialog() {
        _isEditDialogOpen.value = true
    }

    fun closeEditDialog() {
        _isEditDialogOpen.value = false
    }

    fun openDeleteDialog() {
        _isDeleteDialogOpen.value = true
    }

    fun closeDeleteDialog() {
        _isDeleteDialogOpen.value = false
    }

    fun updateSubject(
        name: String,
        shortCode: String,
        colorKey: String,
        targetPercentage: Int,
        location: String,
        initialPresent: Int,
        initialAbsent: Int
    ) {
        viewModelScope.launch {
            val current = uiState.value.detail?.subject ?: return@launch
            subjectRepository.updateSubject(
                current.copy(
                    name = name.trim(),
                    shortCode = shortCode.trim(),
                    colorKey = colorKey,
                    targetPercentage = targetPercentage,
                    location = location.trim(),
                    initialPresent = initialPresent,
                    initialAbsent = initialAbsent
                )
            )
            closeEditDialog()
        }
    }

    fun toggleArchive() {
        viewModelScope.launch {
            val current = uiState.value.detail?.subject ?: return@launch
            subjectRepository.setArchivedStatus(current.id, !current.isArchived)
        }
    }

    fun deleteSubject(onDeleted: () -> Unit) {
        viewModelScope.launch {
            subjectRepository.deleteSubject(subjectId)
            closeDeleteDialog()
            onDeleted()
        }
    }

    fun recordAttendance(status: AttendanceStatus, date: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            attendanceRepository.recordAttendance(
                subjectId = subjectId,
                date = date,
                status = status,
                units = 1
            )
        }
    }

    fun updateSessionStatus(sessionId: Long, status: AttendanceStatus) {
        viewModelScope.launch {
            attendanceRepository.updateAttendanceStatus(sessionId, status)
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            attendanceRepository.deleteClassSession(sessionId)
        }
    }
}
