package com.hrshd1eux.attendly.ui.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrshd1eux.attendly.core.model.AttendanceStatus
import com.hrshd1eux.attendly.data.preferences.PreferencesManager
import com.hrshd1eux.attendly.domain.model.Subject
import com.hrshd1eux.attendly.domain.model.SubjectAttendanceDetail
import com.hrshd1eux.attendly.domain.repository.AttendanceRepository
import com.hrshd1eux.attendly.domain.repository.SubjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import com.hrshd1eux.attendly.domain.model.AttendanceStanding
import javax.inject.Inject

enum class SubjectFilter(val displayName: String) {
    ALL("All"),
    SAFE("Safe"),
    CRITICAL("Attention"),
    ARCHIVED("Archived")
}

enum class SubjectSort(val displayName: String) {
    DEFAULT("Default"),
    LOWEST_PERCENTAGE("Lowest %"),
    HIGHEST_PERCENTAGE("Highest %"),
    NAME("Name A-Z")
}

data class SubjectsUiState(
    val activeSubjects: List<SubjectAttendanceDetail> = emptyList(),
    val archivedSubjects: List<SubjectAttendanceDetail> = emptyList(),
    val defaultTarget: Int = 75,
    val selectedFilter: SubjectFilter = SubjectFilter.ALL,
    val selectedSort: SubjectSort = SubjectSort.DEFAULT,
    val searchQuery: String = "",
    val isAddDialogOpen: Boolean = false,
    val subjectToEdit: Subject? = null,
    val isStartSemesterDialogOpen: Boolean = false
) {
    // Quick statistics
    val totalActiveCount: Int get() = activeSubjects.size
    val safeCount: Int get() = activeSubjects.count { it.summary.standing != AttendanceStanding.BELOW_TARGET }
    val criticalCount: Int get() = activeSubjects.count { it.summary.standing == AttendanceStanding.BELOW_TARGET }

    val overallAveragePercentage: Double get() {
        val totalP = activeSubjects.sumOf { it.summary.presentUnits }
        val totalEligible = activeSubjects.sumOf { it.summary.totalEligibleUnits }
        return if (totalEligible > 0) (totalP.toDouble() * 100.0) / totalEligible else 0.0
    }

    val filteredActiveSubjects: List<SubjectAttendanceDetail> get() {
        var list = when (selectedFilter) {
            SubjectFilter.ALL -> activeSubjects
            SubjectFilter.SAFE -> activeSubjects.filter { it.summary.standing != AttendanceStanding.BELOW_TARGET }
            SubjectFilter.CRITICAL -> activeSubjects.filter { it.summary.standing == AttendanceStanding.BELOW_TARGET }
            SubjectFilter.ARCHIVED -> emptyList()
        }

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter {
                it.subject.name.lowercase().contains(q) ||
                it.subject.shortCode.lowercase().contains(q) ||
                it.subject.location.lowercase().contains(q)
            }
        }

        return when (selectedSort) {
            SubjectSort.DEFAULT -> list
            SubjectSort.LOWEST_PERCENTAGE -> list.sortedBy { it.summary.percentage }
            SubjectSort.HIGHEST_PERCENTAGE -> list.sortedByDescending { it.summary.percentage }
            SubjectSort.NAME -> list.sortedBy { it.subject.name.lowercase() }
        }
    }

    val filteredArchivedSubjects: List<SubjectAttendanceDetail> get() {
        var list = archivedSubjects
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter {
                it.subject.name.lowercase().contains(q) ||
                it.subject.shortCode.lowercase().contains(q) ||
                it.subject.location.lowercase().contains(q)
            }
        }
        return when (selectedSort) {
            SubjectSort.DEFAULT -> list
            SubjectSort.LOWEST_PERCENTAGE -> list.sortedBy { it.summary.percentage }
            SubjectSort.HIGHEST_PERCENTAGE -> list.sortedByDescending { it.summary.percentage }
            SubjectSort.NAME -> list.sortedBy { it.subject.name.lowercase() }
        }
    }
}

@HiltViewModel
class SubjectsViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val attendanceRepository: AttendanceRepository,
    private val preferencesManager: PreferencesManager,
    private val autoBackupManager: com.hrshd1eux.attendly.data.backup.AutoBackupManager
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(SubjectFilter.ALL)
    private val _selectedSort = MutableStateFlow(SubjectSort.DEFAULT)
    private val _searchQuery = MutableStateFlow("")
    private val _dialogState = MutableStateFlow<Subject?>(null)
    private val _isAddOpen = MutableStateFlow(false)
    private val _isStartSemesterDialogOpen = MutableStateFlow(false)

    private data class FilterState(
        val filter: SubjectFilter,
        val sort: SubjectSort,
        val search: String,
        val isAdd: Boolean,
        val subjectToEdit: Subject?,
        val isStartSemesterOpen: Boolean
    )

    private val filterStateFlow = combine(
        _selectedFilter,
        _selectedSort,
        _searchQuery,
        _isAddOpen,
        combine(_dialogState, _isStartSemesterDialogOpen) { toEdit, isSemesterOpen ->
            Pair(toEdit, isSemesterOpen)
        }
    ) { filter, sort, search, isAdd, (dialogState, isSemesterOpen) ->
        FilterState(filter, sort, search, isAdd, dialogState, isSemesterOpen)
    }

    val uiState: StateFlow<SubjectsUiState> = combine(
        attendanceRepository.getSubjectDetails(),
        attendanceRepository.getArchivedSubjectDetails(),
        preferencesManager.userPreferencesFlow,
        filterStateFlow
    ) { activeDetails, archivedDetails, prefs, criteria ->
        SubjectsUiState(
            activeSubjects = activeDetails,
            archivedSubjects = archivedDetails,
            defaultTarget = prefs.defaultTargetPercentage,
            selectedFilter = criteria.filter,
            selectedSort = criteria.sort,
            searchQuery = criteria.search,
            isAddDialogOpen = criteria.isAdd || criteria.subjectToEdit != null,
            subjectToEdit = criteria.subjectToEdit,
            isStartSemesterDialogOpen = criteria.isStartSemesterOpen
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SubjectsUiState()
    )

    fun setFilter(filter: SubjectFilter) {
        _selectedFilter.value = filter
    }

    fun setSort(sort: SubjectSort) {
        _selectedSort.value = sort
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openAddDialog() {
        _dialogState.value = null
        _isAddOpen.value = true
    }

    fun openEditDialog(subject: Subject) {
        _isAddOpen.value = false
        _dialogState.value = subject
    }

    fun closeDialog() {
        _isAddOpen.value = false
        _dialogState.value = null
    }

    fun saveSubject(
        name: String,
        shortCode: String,
        colorKey: String,
        targetPercentage: Int,
        location: String,
        initialPresent: Int,
        initialAbsent: Int
    ) {
        viewModelScope.launch {
            val toEdit = _dialogState.value
            if (toEdit != null) {
                subjectRepository.updateSubject(
                    toEdit.copy(
                        name = name.trim(),
                        shortCode = shortCode.trim(),
                        colorKey = colorKey,
                        targetPercentage = targetPercentage,
                        location = location.trim(),
                        initialPresent = initialPresent,
                        initialAbsent = initialAbsent
                    )
                )
            } else {
                subjectRepository.addSubject(
                    Subject(
                        name = name.trim(),
                        shortCode = shortCode.trim(),
                        colorKey = colorKey,
                        targetPercentage = targetPercentage,
                        location = location.trim(),
                        initialPresent = initialPresent,
                        initialAbsent = initialAbsent
                    )
                )
            }
            closeDialog()
        }
    }

    fun setArchivedStatus(subjectId: Long, isArchived: Boolean) {
        viewModelScope.launch {
            subjectRepository.setArchivedStatus(subjectId, isArchived)
        }
    }

    fun deleteSubject(subjectId: Long) {
        viewModelScope.launch {
            subjectRepository.deleteSubject(subjectId)
        }
    }

    fun markSubjectAttendance(subjectId: Long, status: AttendanceStatus) {
        viewModelScope.launch {
            val now = java.time.LocalTime.now()
            val startMinute = now.hour * 60 + now.minute
            attendanceRepository.recordAttendance(
                subjectId = subjectId,
                date = LocalDate.now(),
                status = status,
                startMinute = startMinute,
                endMinute = startMinute + 50,
                units = 1
            )
        }
    }

    fun openStartNextSemesterDialog() {
        _isStartSemesterDialogOpen.value = true
    }

    fun closeStartNextSemesterDialog() {
        _isStartSemesterDialogOpen.value = false
    }

    fun startNextSemester(clearTimetable: Boolean) {
        viewModelScope.launch {
            subjectRepository.startNextSemester(clearTimetable)
            autoBackupManager.performAutoBackup()
            closeStartNextSemesterDialog()
        }
    }
}
