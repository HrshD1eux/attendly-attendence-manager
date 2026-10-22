package com.hrshd1eux.attendly.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrshd1eux.attendly.core.model.AttendanceStatus
import com.hrshd1eux.attendly.data.preferences.PreferencesManager
import com.hrshd1eux.attendly.data.preferences.UserPreferences
import com.hrshd1eux.attendly.domain.model.AttendanceSummary
import com.hrshd1eux.attendly.domain.model.Holiday
import com.hrshd1eux.attendly.domain.model.SubjectAttendanceDetail
import com.hrshd1eux.attendly.domain.model.TodayClassItem
import com.hrshd1eux.attendly.domain.repository.AttendanceRepository
import com.hrshd1eux.attendly.domain.repository.HolidayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.hrshd1eux.attendly.ui.components.LeaveOption
import com.hrshd1eux.attendly.domain.repository.UnmarkedClassSession
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val userName: String = "Student",
    val userPreferences: UserPreferences = UserPreferences(),
    val currentDate: LocalDate = LocalDate.now(),
    val todayClasses: List<TodayClassItem> = emptyList(),
    val activeClassPrompt: TodayClassItem? = null,
    val overallSummary: AttendanceSummary? = null,
    val subjectDetails: List<SubjectAttendanceDetail> = emptyList(),
    val todayHoliday: Holiday? = null,
    val isLoading: Boolean = false,
    val detectedBackup: com.hrshd1eux.attendly.data.backup.AutoBackupSummary? = null,
    val unmarkedSessions: List<UnmarkedClassSession> = emptyList(),
    val isCatchUpDialogOpen: Boolean = false,
    val isCatchUpDismissed: Boolean = false,
    val isAddExtraClassDialogOpen: Boolean = false
)

sealed class HomeUiEvent {
    data class ShowUndoSnackbar(
        val message: String,
        val classSessionId: Long,
        val previousStatus: AttendanceStatus?
    ) : HomeUiEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val holidayRepository: HolidayRepository,
    private val preferencesManager: PreferencesManager,
    private val autoBackupManager: com.hrshd1eux.attendly.data.backup.AutoBackupManager
) : ViewModel() {

    private val _detectedBackup = MutableStateFlow<com.hrshd1eux.attendly.data.backup.AutoBackupSummary?>(null)
    private val _unmarkedSessions = MutableStateFlow<List<UnmarkedClassSession>>(emptyList())
    private val _isCatchUpDialogOpen = MutableStateFlow(false)
    private val _isCatchUpDismissed = MutableStateFlow(false)
    private val _isAddExtraClassDialogOpen = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            val backup = autoBackupManager.checkForExistingAutoBackup()
            if (backup != null) {
                _detectedBackup.value = backup
            }
            refreshUnmarkedSessions()
        }
    }

    private val _dialogAndInboxState = combine(
        _unmarkedSessions,
        _isCatchUpDialogOpen,
        _isCatchUpDismissed,
        _isAddExtraClassDialogOpen
    ) { unmarked, isCatchUpOpen, isCatchUpDismissed, isExtraOpen ->
        CatchUpAndExtraState(
            unmarked = unmarked,
            isCatchUpOpen = isCatchUpOpen,
            isCatchUpDismissed = isCatchUpDismissed,
            isExtraOpen = isExtraOpen
        )
    }

    private val _attendanceStatsFlow = combine(
        attendanceRepository.getOverallAttendanceSummary(),
        attendanceRepository.getSubjectDetails(),
        holidayRepository.getHolidayForDate(LocalDate.now())
    ) { overall, subjects, holiday ->
        Triple(overall, subjects, holiday)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        combine(preferencesManager.userPreferencesFlow, _detectedBackup) { prefs, backup ->
            Pair(prefs, backup)
        },
        attendanceRepository.getTodayClassesWithStatus(LocalDate.now()),
        _attendanceStatsFlow,
        _dialogAndInboxState
    ) { (prefs, detectedBackup), todayClasses, (overallSummary, subjectDetails, todayHoliday), inboxState ->
        val currentMinute = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
        val activePrompt = if (prefs.isOnLeaveToday || todayHoliday != null) {
            null
        } else {
            // Pick ongoing or finished unmarked class, otherwise null
            todayClasses.firstOrNull { it.status == null && currentMinute >= (it.startMinute - 10) }
        }

        // Only show detected backup if no subjects exist and not dismissed
        val backupToShow = if (subjectDetails.isEmpty() && !prefs.autoRestorePromptDismissed) {
            detectedBackup
        } else {
            null
        }

        HomeUiState(
            userName = prefs.userName.ifEmpty { "Student" },
            userPreferences = prefs,
            currentDate = LocalDate.now(),
            todayClasses = todayClasses,
            activeClassPrompt = activePrompt,
            overallSummary = overallSummary,
            subjectDetails = subjectDetails,
            todayHoliday = todayHoliday,
            isLoading = false,
            detectedBackup = backupToShow,
            unmarkedSessions = inboxState.unmarked,
            isCatchUpDialogOpen = inboxState.isCatchUpOpen,
            isCatchUpDismissed = inboxState.isCatchUpDismissed,
            isAddExtraClassDialogOpen = inboxState.isExtraOpen
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    private val _events = MutableSharedFlow<HomeUiEvent>()
    val events: SharedFlow<HomeUiEvent> = _events.asSharedFlow()

    fun markTodayClass(item: TodayClassItem, status: AttendanceStatus) {
        viewModelScope.launch {
            val previousStatus = item.status
            val recordedId = attendanceRepository.recordAttendance(
                subjectId = item.subjectId,
                date = item.date,
                status = status,
                timetableSessionId = item.timetableSessionId,
                startMinute = item.startMinute,
                endMinute = item.endMinute,
                location = item.location,
                units = item.attendanceUnits
            )

            val statusText = when (status) {
                AttendanceStatus.PRESENT -> "Present"
                AttendanceStatus.ABSENT -> "Absent"
                AttendanceStatus.CANCELLED -> "Cancelled"
                AttendanceStatus.ON_DUTY -> "On-Duty"
                AttendanceStatus.MEDICAL_LEAVE -> "Medical Leave"
            }

            _events.emit(
                HomeUiEvent.ShowUndoSnackbar(
                    message = "Marked ${item.subjectName} as $statusText",
                    classSessionId = recordedId,
                    previousStatus = previousStatus
                )
            )
        }
    }

    fun clearClassStatus(item: TodayClassItem) {
        viewModelScope.launch {
            item.classSessionId?.let { id ->
                attendanceRepository.deleteClassSession(id)
            }
        }
    }

    fun undoAction(classSessionId: Long, previousStatus: AttendanceStatus?) {
        viewModelScope.launch {
            if (previousStatus != null) {
                attendanceRepository.updateAttendanceStatus(classSessionId, previousStatus)
            } else {
                attendanceRepository.deleteClassSession(classSessionId)
            }
        }
    }

    fun markSubjectAttendance(subjectId: Long, subjectName: String, status: AttendanceStatus) {
        viewModelScope.launch {
            val now = java.time.LocalTime.now()
            val startMinute = now.hour * 60 + now.minute
            val recordedId = attendanceRepository.recordAttendance(
                subjectId = subjectId,
                date = LocalDate.now(),
                status = status,
                startMinute = startMinute,
                endMinute = startMinute + 50,
                units = 1
            )

            val statusText = when (status) {
                AttendanceStatus.PRESENT -> "Present"
                AttendanceStatus.ABSENT -> "Absent"
                AttendanceStatus.CANCELLED -> "Cancelled"
                AttendanceStatus.ON_DUTY -> "On-Duty"
                AttendanceStatus.MEDICAL_LEAVE -> "Medical Leave"
            }

            _events.emit(
                HomeUiEvent.ShowUndoSnackbar(
                    message = "Marked $subjectName as $statusText",
                    classSessionId = recordedId,
                    previousStatus = null
                )
            )
        }
    }

    fun setOnLeaveToday(option: LeaveOption) {
        viewModelScope.launch {
            preferencesManager.setOnLeaveToday(true, option.name)
            val currentClasses = uiState.value.todayClasses
            when (option) {
                LeaveOption.CANCELLED -> {
                    for (item in currentClasses) {
                        if (item.status == null) {
                            attendanceRepository.recordAttendance(
                                subjectId = item.subjectId,
                                date = item.date,
                                status = AttendanceStatus.CANCELLED,
                                timetableSessionId = item.timetableSessionId,
                                startMinute = item.startMinute,
                                endMinute = item.endMinute,
                                location = item.location,
                                units = item.attendanceUnits
                            )
                        }
                    }
                }
                LeaveOption.ABSENT -> {
                    for (item in currentClasses) {
                        if (item.status == null) {
                            attendanceRepository.recordAttendance(
                                subjectId = item.subjectId,
                                date = item.date,
                                status = AttendanceStatus.ABSENT,
                                timetableSessionId = item.timetableSessionId,
                                startMinute = item.startMinute,
                                endMinute = item.endMinute,
                                location = item.location,
                                units = item.attendanceUnits
                            )
                        }
                    }
                }
                LeaveOption.MUTE_ONLY -> {
                    // Just pause reminders and prompts for today without modifying records
                }
            }
        }
    }

    fun cancelLeave() {
        viewModelScope.launch {
            preferencesManager.setOnLeaveToday(false, "")
        }
    }

    fun declareHolidayToday(name: String, markClassesCancelled: Boolean) {
        viewModelScope.launch {
            val today = LocalDate.now()
            holidayRepository.addHoliday(today, name)
            if (markClassesCancelled) {
                val currentClasses = uiState.value.todayClasses
                for (item in currentClasses) {
                    if (item.status == null) {
                        attendanceRepository.recordAttendance(
                            subjectId = item.subjectId,
                            date = item.date,
                            status = AttendanceStatus.CANCELLED,
                            timetableSessionId = item.timetableSessionId,
                            startMinute = item.startMinute,
                            endMinute = item.endMinute,
                            location = item.location,
                            units = item.attendanceUnits
                        )
                    }
                }
            }
        }
    }

    fun removeTodayHoliday() {
        viewModelScope.launch {
            holidayRepository.deleteHolidayByDate(LocalDate.now())
        }
    }

    fun restoreDetectedBackup() {
        viewModelScope.launch {
            val success = autoBackupManager.restoreAutoBackup()
            if (success) {
                _detectedBackup.value = null
            }
        }
    }

    fun dismissDetectedBackup() {
        viewModelScope.launch {
            preferencesManager.setAutoRestorePromptDismissed(true)
            _detectedBackup.value = null
        }
    }

    fun refreshUnmarkedSessions() {
        viewModelScope.launch {
            _unmarkedSessions.value = attendanceRepository.getUnmarkedSessions(7)
        }
    }

    fun openCatchUpDialog() {
        _isCatchUpDialogOpen.value = true
    }

    fun closeCatchUpDialog() {
        _isCatchUpDialogOpen.value = false
    }

    fun dismissCatchUp() {
        _isCatchUpDismissed.value = true
    }

    fun markUnmarkedSession(session: UnmarkedClassSession, status: AttendanceStatus) {
        viewModelScope.launch {
            attendanceRepository.recordAttendance(
                subjectId = session.subjectId,
                date = session.date,
                status = status,
                timetableSessionId = session.timetableSessionId,
                startMinute = session.startMinute,
                endMinute = session.endMinute,
                location = session.location
            )
            refreshUnmarkedSessions()
        }
    }

    fun markAllUnmarkedAsPresent() {
        viewModelScope.launch {
            val current = _unmarkedSessions.value
            for (session in current) {
                attendanceRepository.recordAttendance(
                    subjectId = session.subjectId,
                    date = session.date,
                    status = AttendanceStatus.PRESENT,
                    timetableSessionId = session.timetableSessionId,
                    startMinute = session.startMinute,
                    endMinute = session.endMinute,
                    location = session.location
                )
            }
            refreshUnmarkedSessions()
        }
    }

    fun openAddExtraClassDialog() {
        _isAddExtraClassDialogOpen.value = true
    }

    fun closeAddExtraClassDialog() {
        _isAddExtraClassDialogOpen.value = false
    }

    fun recordExtraClass(
        subjectId: Long,
        date: LocalDate,
        startMinute: Int,
        endMinute: Int,
        location: String,
        status: AttendanceStatus,
        notes: String
    ) {
        viewModelScope.launch {
            attendanceRepository.recordAttendance(
                subjectId = subjectId,
                date = date,
                status = status,
                timetableSessionId = null,
                startMinute = startMinute,
                endMinute = endMinute,
                location = location,
                notes = notes
            )
            closeAddExtraClassDialog()
            refreshUnmarkedSessions()
        }
    }
}

private data class CatchUpAndExtraState(
    val unmarked: List<UnmarkedClassSession> = emptyList(),
    val isCatchUpOpen: Boolean = false,
    val isCatchUpDismissed: Boolean = false,
    val isExtraOpen: Boolean = false
)
