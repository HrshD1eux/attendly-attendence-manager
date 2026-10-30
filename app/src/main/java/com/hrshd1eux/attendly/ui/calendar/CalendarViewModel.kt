package com.hrshd1eux.attendly.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrshd1eux.attendly.core.model.AttendanceStatus
import com.hrshd1eux.attendly.core.model.DayOfWeek
import com.hrshd1eux.attendly.data.preferences.PreferencesManager
import com.hrshd1eux.attendly.domain.model.ClassSession
import com.hrshd1eux.attendly.domain.model.Holiday
import com.hrshd1eux.attendly.domain.model.Subject
import com.hrshd1eux.attendly.domain.model.TimetableSession
import com.hrshd1eux.attendly.domain.repository.AttendanceRepository
import com.hrshd1eux.attendly.domain.repository.HolidayRepository
import com.hrshd1eux.attendly.domain.repository.SubjectRepository
import com.hrshd1eux.attendly.domain.repository.TimetableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val monthSessions: List<ClassSession> = emptyList(),
    val selectedDateSessions: List<ClassSession> = emptyList(),
    val scheduledTimetableSessions: List<TimetableSession> = emptyList(),
    val activeSubjects: List<Subject> = emptyList(),
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val isRecordDialogOpen: Boolean = false,
    val holidays: List<Holiday> = emptyList(),
    val selectedDateHoliday: Holiday? = null
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val timetableRepository: TimetableRepository,
    private val subjectRepository: SubjectRepository,
    private val holidayRepository: HolidayRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _isRecordDialogOpen = MutableStateFlow(false)

    val uiState: StateFlow<CalendarUiState> = combine(
        _currentMonth,
        _selectedDate,
        preferencesManager.userPreferencesFlow,
        subjectRepository.getActiveSubjects(),
        _isRecordDialogOpen
    ) { month, selectedDate, prefs, subjects, isDialogOpen ->
        CalendarUiState(
            currentMonth = month,
            selectedDate = selectedDate,
            activeSubjects = subjects,
            firstDayOfWeek = prefs.firstDayOfWeek,
            isRecordDialogOpen = isDialogOpen
        )
    }.flatMapLatest { baseState ->
        val start = baseState.currentMonth.atDay(1)
        val end = baseState.currentMonth.atEndOfMonth()
        val dayOfWeek = DayOfWeek.fromJavaDayOfWeek(baseState.selectedDate.dayOfWeek)

        combine(
            attendanceRepository.getSessionsBetweenDates(start, end),
            attendanceRepository.getSessionsForDate(baseState.selectedDate),
            timetableRepository.getSessionsForDay(dayOfWeek),
            holidayRepository.getAllHolidays(),
            holidayRepository.getHolidayForDate(baseState.selectedDate)
        ) { monthSessions, selectedDateSessions, scheduledSessions, allHolidays, selectedHoliday ->
            baseState.copy(
                monthSessions = monthSessions,
                selectedDateSessions = selectedDateSessions,
                scheduledTimetableSessions = scheduledSessions,
                holidays = allHolidays,
                selectedDateHoliday = selectedHoliday
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState()
    )

    fun prevMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    fun goToToday() {
        val today = LocalDate.now()
        _currentMonth.value = YearMonth.from(today)
        _selectedDate.value = today
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        if (YearMonth.from(date) != _currentMonth.value) {
            _currentMonth.value = YearMonth.from(date)
        }
    }

    fun openRecordDialog() {
        _isRecordDialogOpen.value = true
    }

    fun closeRecordDialog() {
        _isRecordDialogOpen.value = false
    }

    fun markAllScheduledPresent() {
        viewModelScope.launch {
            val date = _selectedDate.value
            val scheduled = uiState.value.scheduledTimetableSessions
            val alreadyRecorded = uiState.value.selectedDateSessions.associateBy { it.timetableSessionId }

            for (session in scheduled) {
                // If not already recorded for this timetable session, record as PRESENT
                if (!alreadyRecorded.containsKey(session.id)) {
                    attendanceRepository.recordAttendance(
                        subjectId = session.subjectId,
                        date = date,
                        status = AttendanceStatus.PRESENT,
                        timetableSessionId = session.id,
                        startMinute = session.startMinute,
                        endMinute = session.endMinute,
                        location = session.location,
                        units = 1
                    )
                }
            }
        }
    }

    fun recordSelectedScheduledSessions(selections: List<Pair<TimetableSession, AttendanceStatus>>) {
        viewModelScope.launch {
            val date = _selectedDate.value
            for ((session, status) in selections) {
                attendanceRepository.recordAttendance(
                    subjectId = session.subjectId,
                    date = date,
                    status = status,
                    timetableSessionId = session.id,
                    startMinute = session.startMinute,
                    endMinute = session.endMinute,
                    location = session.location,
                    units = 1
                )
            }
            closeRecordDialog()
        }
    }

    fun addAdHocSession(subjectId: Long, status: AttendanceStatus) {
        viewModelScope.launch {
            attendanceRepository.recordAttendance(
                subjectId = subjectId,
                date = _selectedDate.value,
                status = status
            )
            closeRecordDialog()
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

    fun addMultipleHolidays(dates: List<LocalDate>, name: String) {
        if (dates.isEmpty()) return
        viewModelScope.launch {
            val trimmedName = name.trim().ifEmpty { "Academic Holiday" }
            val holidaysToAdd = dates.distinct().map { date ->
                Holiday(
                    date = date,
                    name = trimmedName
                )
            }
            holidayRepository.addHolidays(holidaysToAdd)
        }
    }

    fun removeHolidayForDate(date: LocalDate) {
        viewModelScope.launch {
            holidayRepository.deleteHolidayByDate(date)
        }
    }
}
