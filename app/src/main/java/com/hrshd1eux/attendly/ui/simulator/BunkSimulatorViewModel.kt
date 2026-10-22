package com.hrshd1eux.attendly.ui.simulator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrshd1eux.attendly.core.model.DayOfWeek
import com.hrshd1eux.attendly.domain.calculator.AttendanceCalculator
import com.hrshd1eux.attendly.domain.model.AttendanceSummary
import com.hrshd1eux.attendly.domain.model.ScenarioSimulationResult
import com.hrshd1eux.attendly.domain.model.Subject
import com.hrshd1eux.attendly.domain.model.SubjectAttendanceDetail
import com.hrshd1eux.attendly.domain.model.TimetableSession
import com.hrshd1eux.attendly.domain.repository.AttendanceRepository
import com.hrshd1eux.attendly.domain.repository.HolidayRepository
import com.hrshd1eux.attendly.domain.repository.SubjectRepository
import com.hrshd1eux.attendly.domain.repository.TimetableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class SimulatorFestClass(
    val subjectId: Long,
    val subjectName: String,
    val date: LocalDate,
    val dayOfWeek: DayOfWeek,
    val startMinute: Int,
    val endMinute: Int,
    val isBunked: Boolean = true
)

data class SimulatorUiState(
    val selectedSubjectId: Long? = null, // null = Overall
    val subjects: List<Subject> = emptyList(),
    val subjectDetails: List<SubjectAttendanceDetail> = emptyList(),
    val overallSummary: AttendanceSummary? = null,
    val additionalBunked: Int = 0,
    val additionalAttended: Int = 0,
    val simulationResult: ScenarioSimulationResult? = null,
    // Fest planner mode
    val festStartDate: LocalDate = LocalDate.now().plusDays(1),
    val festEndDate: LocalDate = LocalDate.now().plusDays(3),
    val festClasses: List<SimulatorFestClass> = emptyList(),
    val isFestMode: Boolean = false
)

@HiltViewModel
class BunkSimulatorViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val attendanceRepository: AttendanceRepository,
    private val timetableRepository: TimetableRepository,
    private val holidayRepository: HolidayRepository
) : ViewModel() {

    private val _selectedSubjectId = MutableStateFlow<Long?>(null)
    private val _additionalBunked = MutableStateFlow(0)
    private val _additionalAttended = MutableStateFlow(0)
    private val _festStartDate = MutableStateFlow(LocalDate.now().plusDays(1))
    private val _festEndDate = MutableStateFlow(LocalDate.now().plusDays(3))
    private val _festBunkToggles = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private val _isFestMode = MutableStateFlow(false)

    private val _dataFlow = combine(
        subjectRepository.getActiveSubjects(),
        attendanceRepository.getOverallAttendanceSummary(),
        attendanceRepository.getSubjectDetails(),
        timetableRepository.getAllSessions()
    ) { subjects, overallSummary, subjectDetails, ttSessions ->
        DataTuple(subjects, overallSummary, subjectDetails, ttSessions)
    }

    private val _controlFlow = combine(
        _selectedSubjectId,
        _additionalBunked,
        _additionalAttended
    ) { subId, bunked, attended ->
        ControlTuple(subId, bunked, attended)
    }

    private val _festFlow = combine(
        _festStartDate,
        _festEndDate,
        _festBunkToggles,
        _isFestMode
    ) { festStart, festEnd, festToggles, isFest ->
        FestTuple(festStart, festEnd, festToggles, isFest)
    }

    val uiState: StateFlow<SimulatorUiState> = combine(
        _dataFlow,
        _controlFlow,
        _festFlow
    ) { data, control, fest ->
        val subjects = data.subjects
        val overallSummary = data.overallSummary
        val subjectDetails = data.subjectDetails
        val ttSessions = data.ttSessions

        val subId = control.subId
        val bunked = control.bunked
        val attended = control.attended

        val festStart = fest.festStart
        val festEnd = fest.festEnd
        val festToggles = fest.festToggles
        val isFest = fest.isFest

        val currentSummary = if (subId == null) {
            overallSummary
        } else {
            subjectDetails.find { it.subject.id == subId }?.summary ?: overallSummary
        }

        // Calculate fest classes
        val festClasses = computeFestClasses(festStart, festEnd, ttSessions, subjects, festToggles)

        val effectiveBunked: Int
        val effectiveAttended: Int

        if (isFest) {
            // In fest mode, count toggled fest classes matching subject filter
            val matchingFestBunked = festClasses.count {
                it.isBunked && (subId == null || it.subjectId == subId)
            }
            effectiveBunked = matchingFestBunked
            effectiveAttended = 0
        } else {
            effectiveBunked = bunked
            effectiveAttended = attended
        }

        val simResult = currentSummary?.let {
            AttendanceCalculator.simulateScenario(
                currentSummary = it,
                additionalAttended = effectiveAttended,
                additionalBunked = effectiveBunked
            )
        }

        SimulatorUiState(
            selectedSubjectId = subId,
            subjects = subjects,
            subjectDetails = subjectDetails,
            overallSummary = overallSummary,
            additionalBunked = effectiveBunked,
            additionalAttended = effectiveAttended,
            simulationResult = simResult,
            festStartDate = festStart,
            festEndDate = festEnd,
            festClasses = festClasses,
            isFestMode = isFest
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SimulatorUiState()
    )

    private fun computeFestClasses(
        start: LocalDate,
        end: LocalDate,
        sessions: List<TimetableSession>,
        subjects: List<Subject>,
        toggles: Map<String, Boolean>
    ): List<SimulatorFestClass> {
        val subjectMap = subjects.associateBy { it.id }
        val sessionsByDay = sessions.groupBy { it.dayOfWeek }
        val result = mutableListOf<SimulatorFestClass>()

        if (start.isAfter(end)) return emptyList()
        val days = ChronoUnit.DAYS.between(start, end).coerceAtMost(30)

        for (i in 0..days) {
            val d = start.plusDays(i)
            val dayOfWeek = DayOfWeek.fromJavaDayOfWeek(d.dayOfWeek)
            val daySessions = sessionsByDay[dayOfWeek] ?: emptyList()

            for (s in daySessions) {
                val subject = subjectMap[s.subjectId] ?: continue
                val key = "${d}_${s.id}"
                val isBunked = toggles[key] ?: true

                result.add(
                    SimulatorFestClass(
                        subjectId = subject.id,
                        subjectName = subject.name,
                        date = d,
                        dayOfWeek = dayOfWeek,
                        startMinute = s.startMinute,
                        endMinute = s.endMinute,
                        isBunked = isBunked
                    )
                )
            }
        }

        return result.sortedWith(compareBy<SimulatorFestClass> { it.date }.thenBy { it.startMinute })
    }

    fun selectSubject(subjectId: Long?) {
        _selectedSubjectId.value = subjectId
    }

    fun setAdditionalBunked(count: Int) {
        _additionalBunked.value = maxOf(0, count)
    }

    fun setAdditionalAttended(count: Int) {
        _additionalAttended.value = maxOf(0, count)
    }

    fun addBunks(delta: Int) {
        _additionalBunked.value = maxOf(0, _additionalBunked.value + delta)
    }

    fun addAttended(delta: Int) {
        _additionalAttended.value = maxOf(0, _additionalAttended.value + delta)
    }

    fun resetSimulation() {
        _additionalBunked.value = 0
        _additionalAttended.value = 0
    }

    fun setFestMode(isFest: Boolean) {
        _isFestMode.value = isFest
    }

    fun setFestDateRange(start: LocalDate, end: LocalDate) {
        _festStartDate.value = start
        _festEndDate.value = if (end.isBefore(start)) start else end
    }

    fun toggleFestClass(date: LocalDate, subjectId: Long, startMinute: Int) {
        val current = _festBunkToggles.value.toMutableMap()
        // find matching
        val key = "${date}_${subjectId}_$startMinute"
        current[key] = !(current[key] ?: true)
        _festBunkToggles.value = current
    }

    fun toggleAllFest(bunkAll: Boolean) {
        val festList = uiState.value.festClasses
        val current = mutableMapOf<String, Boolean>()
        for (item in festList) {
            val key = "${item.date}_${item.subjectId}_${item.startMinute}"
            current[key] = bunkAll
        }
        _festBunkToggles.value = current
    }
}

private data class DataTuple(
    val subjects: List<Subject>,
    val overallSummary: AttendanceSummary?,
    val subjectDetails: List<SubjectAttendanceDetail>,
    val ttSessions: List<TimetableSession>
)

private data class ControlTuple(
    val subId: Long?,
    val bunked: Int,
    val attended: Int
)

private data class FestTuple(
    val festStart: LocalDate,
    val festEnd: LocalDate,
    val festToggles: Map<String, Boolean>,
    val isFest: Boolean
)
