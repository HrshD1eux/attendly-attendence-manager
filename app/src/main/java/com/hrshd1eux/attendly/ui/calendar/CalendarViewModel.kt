package com.hrshd1eux.attendly.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrshd1eux.attendly.core.model.AttendanceStatus
import com.hrshd1eux.attendly.core.model.DayOfWeek
import com.hrshd1eux.attendly.data.preferences.PreferencesManager
import com.hrshd1eux.attendly.domain.model.ClassSession
import com.hrshd1eux.attendly.domain.model.Subject
import com.hrshd1eux.attendly.domain.repository.AttendanceRepository
import com.hrshd1eux.attendly.domain.repository.SubjectRepository
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
    val activeSubjects: List<Subject> = emptyList(),
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val isAddAdHocDialogOpen: Boolean = false
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val subjectRepository: SubjectRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _isAddAdHocDialogOpen = MutableStateFlow(false)

    val uiState: StateFlow<CalendarUiState> = combine(
        _currentMonth,
        _selectedDate,
        preferencesManager.userPreferencesFlow,
        subjectRepository.getActiveSubjects(),
        _isAddAdHocDialogOpen
    ) { month, selectedDate, prefs, subjects, isDialogOpen ->
        CalendarUiState(
            currentMonth = month,
            selectedDate = selectedDate,
            activeSubjects = subjects,
            firstDayOfWeek = prefs.firstDayOfWeek,
            isAddAdHocDialogOpen = isDialogOpen
        )
    }.flatMapLatest { baseState ->
        val start = baseState.currentMonth.atDay(1)
        val end = baseState.currentMonth.atEndOfMonth()

        combine(
            attendanceRepository.getSessionsBetweenDates(start, end),
            attendanceRepository.getSessionsForDate(baseState.selectedDate)
        ) { monthSessions, selectedDateSessions ->
            baseState.copy(
                monthSessions = monthSessions,
                selectedDateSessions = selectedDateSessions
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

    fun openAddAdHocDialog() {
        _isAddAdHocDialogOpen.value = true
    }

    fun closeAddAdHocDialog() {
        _isAddAdHocDialogOpen.value = false
    }

    fun addAdHocSession(subjectId: Long, status: AttendanceStatus) {
        viewModelScope.launch {
            attendanceRepository.recordAttendance(
                subjectId = subjectId,
                date = _selectedDate.value,
                status = status
            )
            closeAddAdHocDialog()
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
