package com.hrshd1eux.attendly.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrshd1eux.attendly.core.model.AttendanceStatus
import com.hrshd1eux.attendly.data.preferences.PreferencesManager
import com.hrshd1eux.attendly.data.preferences.UserPreferences
import com.hrshd1eux.attendly.domain.model.AttendanceSummary
import com.hrshd1eux.attendly.domain.model.SubjectAttendanceDetail
import com.hrshd1eux.attendly.domain.model.TodayClassItem
import com.hrshd1eux.attendly.domain.repository.AttendanceRepository
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
    val isLoading: Boolean = false
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
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        preferencesManager.userPreferencesFlow,
        attendanceRepository.getTodayClassesWithStatus(LocalDate.now()),
        attendanceRepository.getOverallAttendanceSummary(),
        attendanceRepository.getSubjectDetails()
    ) { prefs, todayClasses, overallSummary, subjectDetails ->
        val currentMinute = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
        val activePrompt = if (prefs.isOnLeaveToday) {
            null
        } else {
            // Pick ongoing or finished unmarked class, otherwise null
            todayClasses.firstOrNull { it.status == null && currentMinute >= (it.startMinute - 10) }
        }

        HomeUiState(
            userName = prefs.userName.ifEmpty { "Student" },
            userPreferences = prefs,
            currentDate = LocalDate.now(),
            todayClasses = todayClasses,
            activeClassPrompt = activePrompt,
            overallSummary = overallSummary,
            subjectDetails = subjectDetails,
            isLoading = false
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
}
