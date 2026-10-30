package com.hrshd1eux.attendly.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Today
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hrshd1eux.attendly.core.model.AttendanceStatus
import com.hrshd1eux.attendly.core.model.DayOfWeek
import com.hrshd1eux.attendly.core.util.TimeUtils
import com.hrshd1eux.attendly.domain.model.ClassSession
import com.hrshd1eux.attendly.domain.model.Holiday
import com.hrshd1eux.attendly.domain.model.Subject
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.OutlinedButton
import com.hrshd1eux.attendly.ui.components.AddHolidayDialog
import com.hrshd1eux.attendly.ui.components.AttendanceStatusBadge
import com.hrshd1eux.attendly.ui.components.AttendlyTopBar
import com.hrshd1eux.attendly.ui.theme.LocalStatusColors
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val statusColors = LocalStatusColors.current
    var isAddHolidayDialogOpen by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AttendlyTopBar(
                title = "Calendar",
                actions = {
                    TextButton(onClick = viewModel::goToToday) {
                        Icon(imageVector = Icons.Default.Today, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Today")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Month navigation & Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = viewModel::prevMonth) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                            }

                            Text(
                                text = "${uiState.currentMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${uiState.currentMonth.year}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )

                            IconButton(onClick = viewModel::nextMonth) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Day of week labels
                        val daysOfWeek = if (uiState.firstDayOfWeek == DayOfWeek.SUNDAY) {
                            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                        } else {
                            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            daysOfWeek.forEach { dayName ->
                                Text(
                                    text = dayName,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Calendar Grid Days
                        CalendarGrid(
                            yearMonth = uiState.currentMonth,
                            selectedDate = uiState.selectedDate,
                            firstDayOfWeek = uiState.firstDayOfWeek,
                            monthSessions = uiState.monthSessions,
                            holidays = uiState.holidays,
                            onDateSelected = viewModel::selectDate
                        )
                    }
                }
            }

            // Holiday Banner for Selected Date (if it is a holiday)
            if (uiState.selectedDateHoliday != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = statusColors.absentContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎉", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.selectedDateHoliday!!.name,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = statusColors.onAbsentContainer
                                )
                                Text(
                                    text = "Academic / National Holiday",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = statusColors.onAbsentContainer.copy(alpha = 0.8f)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.removeHolidayForDate(uiState.selectedDate) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove Holiday",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Selected Date Schedule / History
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = TimeUtils.formatDate(uiState.selectedDate, "EEEE, MMMM d"),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${uiState.selectedDateSessions.size} classes recorded",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (uiState.selectedDateHoliday == null) {
                            OutlinedButton(
                                onClick = { isAddHolidayDialogOpen = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Celebration,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("Holiday")
                            }
                        }

                        if (uiState.scheduledTimetableSessions.isNotEmpty()) {
                            Button(
                                onClick = viewModel::markAllScheduledPresent,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = statusColors.present,
                                    contentColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("All Present")
                            }
                        }

                        if (uiState.activeSubjects.isNotEmpty() || uiState.scheduledTimetableSessions.isNotEmpty()) {
                            Button(
                                onClick = viewModel::openRecordDialog,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("Record")
                            }
                        }
                    }
                }
            }

            if (uiState.selectedDateSessions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No attendance recorded for this date.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.selectedDateSessions, key = { it.id }) { session ->
                    CalendarSessionItem(
                        session = session,
                        onUpdateStatus = { status -> viewModel.updateSessionStatus(session.id, status) },
                        onDelete = { viewModel.deleteSession(session.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Record Timetable / Ad-Hoc Classes Dialog
        if (uiState.isRecordDialogOpen) {
            RecordDateClassesDialog(
                selectedDate = uiState.selectedDate,
                scheduledSessions = uiState.scheduledTimetableSessions,
                alreadyRecordedSessions = uiState.selectedDateSessions,
                activeSubjects = uiState.activeSubjects,
                onDismiss = viewModel::closeRecordDialog,
                onMarkAllPresent = viewModel::markAllScheduledPresent,
                onRecordSelected = viewModel::recordSelectedScheduledSessions,
                onRecordAdHoc = viewModel::addAdHocSession
            )
        }

        // Add Holiday(s) Dialog from Calendar
        if (isAddHolidayDialogOpen) {
            AddHolidayDialog(
                initialStartDate = uiState.selectedDate,
                onDismiss = { isAddHolidayDialogOpen = false },
                onAddHolidays = { dates, name ->
                    viewModel.addMultipleHolidays(dates, name)
                    isAddHolidayDialogOpen = false
                }
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    firstDayOfWeek: DayOfWeek,
    monthSessions: List<ClassSession>,
    holidays: List<Holiday>,
    onDateSelected: (LocalDate) -> Unit
) {
    val statusColors = LocalStatusColors.current
    val firstOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()

    // Map holidays by LocalDate
    val holidaysByDate = remember(holidays) { holidays.associateBy { it.date } }

    // Determine leading empty cells
    val firstDayIso = firstOfMonth.dayOfWeek.value // Mon=1..Sun=7
    val leadDays = if (firstDayOfWeek == DayOfWeek.SUNDAY) {
        firstDayIso % 7
    } else {
        firstDayIso - 1
    }

    val totalCells = leadDays + daysInMonth
    val numRows = (totalCells + 6) / 7

    val sessionsByDate = monthSessions.groupBy { it.date.dayOfMonth }
    val redHolidayColor = statusColors.absent

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in 0 until numRows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNum = cellIndex - leadDays + 1

                    if (dayNum in 1..daysInMonth) {
                        val date = yearMonth.atDay(dayNum)
                        val isSelected = date == selectedDate
                        val isToday = date == LocalDate.now()
                        val holiday = holidaysByDate[date]
                        val isHoliday = holiday != null
                        val daySessions = sessionsByDate[dayNum] ?: emptyList()

                        val indicatorColor = when {
                            daySessions.any { it.status == AttendanceStatus.ABSENT } -> statusColors.absent
                            daySessions.any { it.status == AttendanceStatus.PRESENT } -> statusColors.present
                            daySessions.any { it.status == AttendanceStatus.CANCELLED } -> statusColors.cancelled
                            else -> null
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                                        isHoliday -> statusColors.absentContainer.copy(alpha = 0.45f)
                                        else -> androidx.compose.ui.graphics.Color.Transparent
                                    }
                                )
                                .then(
                                    when {
                                        isHoliday -> Modifier.border(
                                            width = 1.8.dp,
                                            color = redHolidayColor,
                                            shape = CircleShape
                                        )
                                        isToday && !isSelected -> Modifier.border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape
                                        )
                                        else -> Modifier
                                    }
                                )
                                .clickable { onDateSelected(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dayNum.toString(),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isSelected || isToday || isHoliday) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            isHoliday -> redHolidayColor
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                )
                                if (indicatorColor != null || isHoliday) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (indicatorColor != null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(indicatorColor)
                                            )
                                        }
                                        if (isHoliday && indicatorColor == null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(redHolidayColor)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Calendar Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, redHolidayColor, CircleShape)
                )
                Text("Holiday", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColors.present)
                )
                Text("Present", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColors.absent)
                )
                Text("Absent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CalendarSessionItem(
    session: ClassSession,
    onUpdateStatus: (AttendanceStatus) -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.subjectName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (session.startMinute > 0 || session.endMinute > 0) {
                    Text(
                        text = "${TimeUtils.minutesToTimeString(session.startMinute)} - ${TimeUtils.minutesToTimeString(session.endMinute)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                AttendanceStatusBadge(status = session.status)

                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Options")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (session.status != AttendanceStatus.PRESENT) {
                            DropdownMenuItem(
                                text = { Text("Change to Present") },
                                onClick = {
                                    showMenu = false
                                    onUpdateStatus(AttendanceStatus.PRESENT)
                                }
                            )
                        }
                        if (session.status != AttendanceStatus.ABSENT) {
                            DropdownMenuItem(
                                text = { Text("Change to Absent") },
                                onClick = {
                                    showMenu = false
                                    onUpdateStatus(AttendanceStatus.ABSENT)
                                }
                            )
                        }
                        if (session.status != AttendanceStatus.CANCELLED) {
                            DropdownMenuItem(
                                text = { Text("Mark Cancelled") },
                                onClick = {
                                    showMenu = false
                                    onUpdateStatus(AttendanceStatus.CANCELLED)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}
