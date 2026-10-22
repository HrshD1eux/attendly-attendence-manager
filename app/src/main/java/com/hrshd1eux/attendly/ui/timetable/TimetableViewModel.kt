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
    val isImporting: Boolean = false,
    val isQrShareDialogOpen: Boolean = false,
    val qrShareBitmap: android.graphics.Bitmap? = null,
    val qrShareTitle: String = "",
    val detectedClipboardText: String? = null,
    val showClipboardPrompt: Boolean = false
) {
    val ignoredSessionsCount: Int
        get() {
            val raw = rawParseResult?.entries?.size ?: 0
            val filtered = parseResult?.entries?.size ?: 0
            return (raw - filtered).coerceAtLeast(0)
        }
}

private data class DialogExtraState(
    val isAddEdit: Boolean = false,
    val sessionToEdit: TimetableSession? = null,
    val isAiPrompt: Boolean = false,
    val isQrShare: Boolean = false,
    val qrBitmap: android.graphics.Bitmap? = null,
    val qrTitle: String = ""
)

private data class ImportExtraState(
    val raw: TimetableParseResult? = null,
    val group: String? = null,
    val clean: Boolean = true,
    val isDialogOpen: Boolean = false,
    val clipboardText: String? = null,
    val showClipboardPrompt: Boolean = false
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

    private val _isQrShareDialogOpen = MutableStateFlow(false)
    private val _qrShareBitmap = MutableStateFlow<android.graphics.Bitmap?>(null)
    private val _qrShareTitle = MutableStateFlow("")

    private val _detectedClipboardText = MutableStateFlow<String?>(null)
    private val _showClipboardPrompt = MutableStateFlow(false)

    private val _dialogState = combine(
        _isAddEditDialogOpen,
        _sessionToEdit,
        _isAiPromptDialogOpen,
        _isQrShareDialogOpen,
        _qrShareBitmap,
        _qrShareTitle
    ) { args ->
        DialogExtraState(
            isAddEdit = args[0] as Boolean,
            sessionToEdit = args[1] as TimetableSession?,
            isAiPrompt = args[2] as Boolean,
            isQrShare = args[3] as Boolean,
            qrBitmap = args[4] as android.graphics.Bitmap?,
            qrTitle = args[5] as String
        )
    }

    private val _importState = combine(
        _rawParseResult,
        _selectedGroup,
        _cleanSubjectNames,
        _isGroupSelectionDialogOpen,
        _detectedClipboardText,
        _showClipboardPrompt
    ) { args ->
        ImportExtraState(
            raw = args[0] as TimetableParseResult?,
            group = args[1] as String?,
            clean = args[2] as Boolean,
            isDialogOpen = args[3] as Boolean,
            clipboardText = args[4] as String?,
            showClipboardPrompt = args[5] as Boolean
        )
    }

    val uiState: StateFlow<TimetableUiState> = combine(
        timetableRepository.getAllSessions(),
        subjectRepository.getActiveSubjects(),
        _selectedDay,
        _dialogState,
        _importState
    ) { sessions, subjects, day, dialogState, importState ->
        val filteredResult = importState.raw?.filterByGroup(
            targetGroup = importState.group,
            cleanSubjectNames = importState.clean
        )

        TimetableUiState(
            selectedDay = day,
            sessionsForSelectedDay = sessions.filter { it.dayOfWeek == day }.sortedBy { it.startMinute },
            allSessions = sessions,
            activeSubjects = subjects,
            isAddEditDialogOpen = dialogState.isAddEdit,
            sessionToEdit = dialogState.sessionToEdit,
            isAiPromptDialogOpen = dialogState.isAiPrompt,
            rawParseResult = importState.raw,
            parseResult = filteredResult,
            detectedGroups = importState.raw?.detectedGroups ?: emptyList(),
            selectedGroup = importState.group,
            cleanSubjectNames = importState.clean,
            isGroupSelectionDialogOpen = importState.isDialogOpen,
            isQrShareDialogOpen = dialogState.isQrShare,
            qrShareBitmap = dialogState.qrBitmap,
            qrShareTitle = dialogState.qrTitle,
            detectedClipboardText = importState.clipboardText,
            showClipboardPrompt = importState.showClipboardPrompt
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

    fun openQrShareDialog(dayOfWeek: DayOfWeek? = null) {
        viewModelScope.launch {
            val text = timetableRepository.exportTimetableAsText(dayOfWeek)
            val compressed = com.hrshd1eux.attendly.core.util.QrCodeUtils.compressTimetable(text)
            val bitmap = com.hrshd1eux.attendly.core.util.QrCodeUtils.generateQrBitmap(compressed, 768)
            val title = if (dayOfWeek == null) "Weekly Timetable" else "${dayOfWeek.displayName} Timetable"
            _qrShareBitmap.value = bitmap
            _qrShareTitle.value = title
            _isQrShareDialogOpen.value = true
        }
    }

    fun closeQrShareDialog() {
        _isQrShareDialogOpen.value = false
        _qrShareBitmap.value = null
    }

    fun checkClipboardForTimetable(clipboardText: String?) {
        if (clipboardText.isNullOrBlank()) return
        val trimmed = clipboardText.trim()
        val isRecognized = trimmed.startsWith("ATTENDLY:v1:") ||
                trimmed.contains("📅 Attendly", ignoreCase = true) ||
                (trimmed.contains("DAY:", ignoreCase = true) && trimmed.contains("|")) ||
                (trimmed.contains("TIMETABLE", ignoreCase = true))

        if (isRecognized && _rawParseResult.value == null) {
            _detectedClipboardText.value = trimmed
            _showClipboardPrompt.value = true
        }
    }

    fun dismissClipboardPrompt() {
        _showClipboardPrompt.value = false
    }

    fun importDetectedClipboard() {
        val text = _detectedClipboardText.value ?: return
        _showClipboardPrompt.value = false
        parseTimetableText(text)
    }

    fun decodeQrBitmap(bitmap: android.graphics.Bitmap, onSuccess: () -> Unit, onError: () -> Unit) {
        val decoded = com.hrshd1eux.attendly.core.util.QrCodeUtils.decodeQrFromBitmap(bitmap)
        if (decoded != null && decoded.isNotBlank()) {
            parseTimetableText(decoded)
            onSuccess()
        } else {
            onError()
        }
    }
}
