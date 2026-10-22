package com.hrshd1eux.attendly.ui.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrshd1eux.attendly.core.model.AttendanceUnitPolicy
import com.hrshd1eux.attendly.core.model.DayOfWeek
import com.hrshd1eux.attendly.data.preferences.PreferencesManager
import com.hrshd1eux.attendly.domain.model.ParsedTimetableEntry
import com.hrshd1eux.attendly.domain.model.Subject
import com.hrshd1eux.attendly.domain.model.TimetableParseResult
import com.hrshd1eux.attendly.domain.model.TimetableSession
import com.hrshd1eux.attendly.domain.parser.TimetableTextParser
import com.hrshd1eux.attendly.domain.repository.SubjectRepository
import com.hrshd1eux.attendly.domain.repository.TimetableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimetableUiState(
    val selectedDay: DayOfWeek = DayOfWeek.MONDAY,
    val sessionsForSelectedDay: List<TimetableSession> = emptyList(),
    val allSessions: List<TimetableSession> = emptyList(),
    val activeSubjects: List<Subject> = emptyList(),
    val isAddEditDialogOpen: Boolean = false,
    val sessionToEdit: TimetableSession? = null,
    val isAiPromptDialogOpen: Boolean = false,
    val rawParseResult: TimetableParseResult? = null,
    val parseResult: TimetableParseResult? = null,
    val detectedGroups: List<String> = emptyList(),
    val selectedGroup: String? = null,
    val cleanSubjectNames: Boolean = true,
    val isGroupSelectionDialogOpen: Boolean = false,
    val isImporting: Boolean = false
) {
    val ignoredSessionsCount: Int
        get() {
            val raw = rawParseResult?.entries?.size ?: 0
            val filtered = parseResult?.entries?.size ?: 0
            return (raw - filtered).coerceAtLeast(0)
        }
}

private data class ImportParseState(
    val rawParseResult: TimetableParseResult? = null,
    val selectedGroup: String? = null,
    val cleanSubjectNames: Boolean = true,
    val isGroupSelectionDialogOpen: Boolean = false
)

@HiltViewModel
class TimetableViewModel @Inject constructor(
    private val timetableRepository: TimetableRepository,
    private val subjectRepository: SubjectRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _selectedDay = MutableStateFlow(DayOfWeek.MONDAY)
    private val _isAddEditDialogOpen = MutableStateFlow(false)
    private val _sessionToEdit = MutableStateFlow<TimetableSession?>(null)
    private val _isAiPromptDialogOpen = MutableStateFlow(false)

    private val _rawParseResult = MutableStateFlow<TimetableParseResult?>(null)
    private val _selectedGroup = MutableStateFlow<String?>(null)
    private val _cleanSubjectNames = MutableStateFlow(true)
    private val _isGroupSelectionDialogOpen = MutableStateFlow(false)

    private val _dialogState = combine(
        _isAddEditDialogOpen,
        _sessionToEdit,
        _isAiPromptDialogOpen
    ) { isAddEdit, sessionToEdit, isAiPrompt ->
        Triple(isAddEdit, sessionToEdit, isAiPrompt)
    }

    private val _importState = combine(
        _rawParseResult,
        _selectedGroup,
        _cleanSubjectNames,
        _isGroupSelectionDialogOpen
    ) { raw, group, clean, isDialogOpen ->
        ImportParseState(raw, group, clean, isDialogOpen)
    }

    val uiState: StateFlow<TimetableUiState> = combine(
        timetableRepository.getAllSessions(),
        subjectRepository.getActiveSubjects(),
        _selectedDay,
        _dialogState,
        _importState
    ) { sessions, subjects, day, (isAddEdit, sessionToEdit, isAiPrompt), importState ->
        val filteredResult = importState.rawParseResult?.filterByGroup(
            targetGroup = importState.selectedGroup,
            cleanSubjectNames = importState.cleanSubjectNames
        )

        TimetableUiState(
            selectedDay = day,
            sessionsForSelectedDay = sessions.filter { it.dayOfWeek == day }.sortedBy { it.startMinute },
            allSessions = sessions,
            activeSubjects = subjects,
            isAddEditDialogOpen = isAddEdit,
            sessionToEdit = sessionToEdit,
            isAiPromptDialogOpen = isAiPrompt,
            rawParseResult = importState.rawParseResult,
            parseResult = filteredResult,
            detectedGroups = importState.rawParseResult?.detectedGroups ?: emptyList(),
            selectedGroup = importState.selectedGroup,
            cleanSubjectNames = importState.cleanSubjectNames,
            isGroupSelectionDialogOpen = importState.isGroupSelectionDialogOpen
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TimetableUiState()
    )

    fun selectDay(day: DayOfWeek) {
        _selectedDay.value = day
    }

    fun openAddDialog() {
        _sessionToEdit.value = null
        _isAddEditDialogOpen.value = true
    }

    fun openEditDialog(session: TimetableSession) {
        _sessionToEdit.value = session
        _isAddEditDialogOpen.value = true
    }

    fun closeAddEditDialog() {
        _isAddEditDialogOpen.value = false
        _sessionToEdit.value = null
    }

    fun openAiPromptDialog() {
        _isAiPromptDialogOpen.value = true
    }

    fun closeAiPromptDialog() {
        _isAiPromptDialogOpen.value = false
    }

    fun selectGroupFilter(group: String?) {
        _selectedGroup.value = group
    }

    fun toggleCleanSubjectNames(clean: Boolean) {
        _cleanSubjectNames.value = clean
    }

    fun openGroupSelectionDialog() {
        _isGroupSelectionDialogOpen.value = true
    }

    fun closeGroupSelectionDialog() {
        _isGroupSelectionDialogOpen.value = false
    }

    fun saveSession(
        subjectId: Long,
        dayOfWeek: DayOfWeek,
        startMinute: Int,
        endMinute: Int,
        location: String,
        attendanceUnitPolicy: AttendanceUnitPolicy,
        notes: String
    ) {
        viewModelScope.launch {
            val toEdit = _sessionToEdit.value
            if (toEdit != null) {
                timetableRepository.updateSession(
                    toEdit.copy(
                        subjectId = subjectId,
                        dayOfWeek = dayOfWeek,
                        startMinute = startMinute,
                        endMinute = endMinute,
                        location = location.trim(),
                        attendanceUnitPolicy = attendanceUnitPolicy,
                        notes = notes.trim()
                    )
                )
            } else {
                timetableRepository.addSession(
                    TimetableSession(
                        subjectId = subjectId,
                        dayOfWeek = dayOfWeek,
                        startMinute = startMinute,
                        endMinute = endMinute,
                        location = location.trim(),
                        attendanceUnitPolicy = attendanceUnitPolicy,
                        notes = notes.trim()
                    )
                )
            }
            closeAddEditDialog()
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            timetableRepository.deleteSession(sessionId)
        }
    }

    fun clearTimetable() {
        viewModelScope.launch {
            timetableRepository.clearTimetable()
        }
    }

    fun parseTimetableText(text: String) {
        val result = TimetableTextParser.parse(text)
        _rawParseResult.value = result
        if (result.hasMultipleGroups) {
            _selectedGroup.value = result.detectedGroups.firstOrNull()
            _isGroupSelectionDialogOpen.value = true
        } else {
            _selectedGroup.value = null
            _isGroupSelectionDialogOpen.value = false
        }
    }

    fun clearParseResult() {
        _rawParseResult.value = null
        _selectedGroup.value = null
        _isGroupSelectionDialogOpen.value = false
    }

    fun applyImport(replaceExisting: Boolean, onComplete: () -> Unit) {
        val raw = _rawParseResult.value ?: return
        val filtered = raw.filterByGroup(_selectedGroup.value, _cleanSubjectNames.value)
        viewModelScope.launch {
            timetableRepository.importParsedTimetable(filtered.entries, replaceExisting)
            clearParseResult()
            onComplete()
        }
    }

    fun exportTimetable(dayOfWeek: DayOfWeek? = null, onReady: (String) -> Unit) {
        viewModelScope.launch {
            val text = timetableRepository.exportTimetableAsText(dayOfWeek)
            onReady(text)
        }
    }
}
