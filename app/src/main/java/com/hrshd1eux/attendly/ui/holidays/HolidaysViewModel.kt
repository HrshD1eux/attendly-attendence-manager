package com.hrshd1eux.attendly.ui.holidays

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrshd1eux.attendly.domain.model.Holiday
import com.hrshd1eux.attendly.domain.parser.HolidayParseResult
import com.hrshd1eux.attendly.domain.parser.HolidayTextParser
import com.hrshd1eux.attendly.domain.repository.HolidayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HolidaysUiState(
    val allHolidays: List<Holiday> = emptyList(),
    val todayHoliday: Holiday? = null,
    val parseResult: HolidayParseResult? = null,
    val isLoading: Boolean = false,
    val snackbarMessage: String? = null
)

@HiltViewModel
class HolidaysViewModel @Inject constructor(
    private val holidayRepository: HolidayRepository
) : ViewModel() {

    private val _parseResult = MutableStateFlow<HolidayParseResult?>(null)
    private val _snackbarMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HolidaysUiState> = combine(
        holidayRepository.getAllHolidays(),
        _parseResult,
        _snackbarMessage
    ) { allHolidays, parseResult, snackbarMessage ->
        val today = LocalDate.now()
        val todayHoliday = allHolidays.firstOrNull { it.date == today }

        HolidaysUiState(
            allHolidays = allHolidays,
            todayHoliday = todayHoliday,
            parseResult = parseResult,
            snackbarMessage = snackbarMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HolidaysUiState(isLoading = true)
    )

    fun parseHolidayText(text: String) {
        val result = HolidayTextParser.parse(text)
        _parseResult.value = result
    }

    fun clearParseResult() {
        _parseResult.value = null
    }

    fun importParsedHolidays(replaceExisting: Boolean) {
        val result = _parseResult.value ?: return
        if (result.holidays.isEmpty()) return

        viewModelScope.launch {
            if (replaceExisting) {
                holidayRepository.clearAllHolidays()
            }
            holidayRepository.addHolidays(result.holidays)
            _parseResult.value = null
            _snackbarMessage.value = "Successfully imported ${result.holidays.size} holiday dates!"
        }
    }

    fun addSingleHoliday(date: LocalDate, name: String) {
        viewModelScope.launch {
            holidayRepository.addHoliday(date, name)
            _snackbarMessage.value = "Added \"$name\" for $date"
        }
    }

    fun deleteHoliday(id: Long) {
        viewModelScope.launch {
            holidayRepository.deleteHoliday(id)
            _snackbarMessage.value = "Holiday removed"
        }
    }

    fun deleteTodayHoliday() {
        viewModelScope.launch {
            holidayRepository.deleteHolidayByDate(LocalDate.now())
            _snackbarMessage.value = "Today's holiday removed"
        }
    }

    fun clearAllHolidays() {
        viewModelScope.launch {
            holidayRepository.clearAllHolidays()
            _snackbarMessage.value = "All holidays cleared"
        }
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }
}
