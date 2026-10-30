package com.hrshd1eux.attendly.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrshd1eux.attendly.core.util.TimeUtils
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

enum class HolidaySelectionMode {
    DATE_RANGE,
    SPECIFIC_DATES
}

@Composable
fun AddHolidayDialog(
    initialStartDate: LocalDate = LocalDate.now(),
    onDismiss: () -> Unit,
    onAddHolidays: (dates: List<LocalDate>, name: String) -> Unit
) {
    val context = LocalContext.current
    var holidayName by remember { mutableStateOf("") }
    var selectionMode by remember { mutableStateOf(HolidaySelectionMode.DATE_RANGE) }

    // Date Range state
    var startDate by remember { mutableStateOf(initialStartDate) }
    var endDate by remember { mutableStateOf(initialStartDate) }

    // Specific dates state (multi-select calendar)
    var pickerMonth by remember { mutableStateOf(YearMonth.from(initialStartDate)) }
    val selectedDates = remember { mutableStateListOf(initialStartDate) }

    val daysRangeCount = remember(startDate, endDate) {
        if (endDate.isBefore(startDate)) 0L
        else ChronoUnit.DAYS.between(startDate, endDate) + 1L
    }

    val openStartDatePicker = {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newStart = LocalDate.of(year, month + 1, dayOfMonth)
                startDate = newStart
                if (endDate.isBefore(newStart)) {
                    endDate = newStart
                }
            },
            startDate.year,
            startDate.monthValue - 1,
            startDate.dayOfMonth
        ).show()
    }

    val openEndDatePicker = {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newEnd = LocalDate.of(year, month + 1, dayOfMonth)
                endDate = newEnd
            },
            endDate.year,
            endDate.monthValue - 1,
            endDate.dayOfMonth
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Celebration,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Add Holiday(s)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mode selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectionMode == HolidaySelectionMode.DATE_RANGE,
                        onClick = { selectionMode = HolidaySelectionMode.DATE_RANGE },
                        label = { Text("Date Range", style = MaterialTheme.typography.labelMedium) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = selectionMode == HolidaySelectionMode.SPECIFIC_DATES,
                        onClick = { selectionMode = HolidaySelectionMode.SPECIFIC_DATES },
                        label = { Text("Select Dates", style = MaterialTheme.typography.labelMedium) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.EventAvailable,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Holiday name input
                OutlinedTextField(
                    value = holidayName,
                    onValueChange = { holidayName = it },
                    label = { Text("Holiday Name / Occasion") },
                    placeholder = {
                        Text(
                            if (selectionMode == HolidaySelectionMode.DATE_RANGE && daysRangeCount > 1)
                                "e.g. Diwali Break, Winter Vacation, Autumn Break"
                            else "e.g. Founder's Day, Festival, Sports Day"
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectionMode == HolidaySelectionMode.DATE_RANGE) {
                    // Start Date Picker row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { openStartDatePicker() }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Start Date",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = TimeUtils.formatDate(startDate, "EEEE, MMMM d, yyyy"),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Pick Start Date",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // End Date Picker row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { openEndDatePicker() }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "End Date",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = TimeUtils.formatDate(endDate, "EEEE, MMMM d, yyyy"),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Pick End Date",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Quick Range Preset Chips (+3d, +5d, +7d, +10d, +14d)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Quick Presets:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(3, 5, 7, 10, 14).forEach { days ->
                                FilterChip(
                                    selected = daysRangeCount == days.toLong(),
                                    onClick = {
                                        endDate = startDate.plusDays((days - 1).toLong())
                                    },
                                    label = { Text("+$days d", style = MaterialTheme.typography.labelSmall) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    // Range duration preview card
                    if (endDate.isBefore(startDate)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "⚠️ End date cannot be before start date",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🎉", style = MaterialTheme.typography.bodyLarge)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (daysRangeCount == 1L) "1 Day Holiday" else "$daysRangeCount Days Holiday",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = if (daysRangeCount == 1L)
                                            TimeUtils.formatDate(startDate, "MMMM d, yyyy")
                                        else
                                            "${TimeUtils.formatDate(startDate, "MMM d")} → ${TimeUtils.formatDate(endDate, "MMM d, yyyy")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // SPECIFIC_DATES: In-Dialog Interactive Multi-Date Calendar Grid
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Month Navigation Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { pickerMonth = pickerMonth.minusMonths(1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Previous Month",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Text(
                                    text = "${pickerMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${pickerMonth.year}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )

                                IconButton(
                                    onClick = { pickerMonth = pickerMonth.plusMonths(1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Next Month",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Day of Week Labels (Mon - Sun)
                            val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                            Row(modifier = Modifier.fillMaxWidth()) {
                                dayLabels.forEach { label ->
                                    Text(
                                        text = label,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // Calendar Days Grid (Tap dates to select/deselect multiple at once!)
                            val firstDay = pickerMonth.atDay(1)
                            val leadDays = firstDay.dayOfWeek.value - 1 // 0 for Monday ... 6 for Sunday
                            val daysInMonth = pickerMonth.lengthOfMonth()
                            val totalSlots = leadDays + daysInMonth
                            val numWeeks = (totalSlots + 6) / 7

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (week in 0 until numWeeks) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        for (dayCol in 0 until 7) {
                                            val slotIndex = week * 7 + dayCol
                                            val dayOfMonth = slotIndex - leadDays + 1
                                            if (dayOfMonth in 1..daysInMonth) {
                                                val date = pickerMonth.atDay(dayOfMonth)
                                                val isSelected = selectedDates.contains(date)
                                                val isToday = date == LocalDate.now()

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .aspectRatio(1f)
                                                        .clip(CircleShape)
                                                        .background(
                                                            when {
                                                                isSelected -> MaterialTheme.colorScheme.primary
                                                                isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                                                else -> Color.Transparent
                                                            }
                                                        )
                                                        .clickable {
                                                            if (isSelected) {
                                                                selectedDates.remove(date)
                                                            } else {
                                                                selectedDates.add(date)
                                                                selectedDates.sort()
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = dayOfMonth.toString(),
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                            color = when {
                                                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                                                isToday -> MaterialTheme.colorScheme.primary
                                                                else -> MaterialTheme.colorScheme.onSurface
                                                            }
                                                        )
                                                    )
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Selection Summary & Quick Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedDates.isEmpty()) "Tap dates above to select"
                            else "🎉 ${selectedDates.size} date(s) selected",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (selectedDates.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.primary
                            )
                        )

                        if (selectedDates.isNotEmpty()) {
                            TextButton(
                                onClick = { selectedDates.clear() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Clear All",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.error)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val isEnabled = if (selectionMode == HolidaySelectionMode.DATE_RANGE) {
                !endDate.isBefore(startDate)
            } else {
                selectedDates.isNotEmpty()
            }

            val buttonLabel = if (selectionMode == HolidaySelectionMode.DATE_RANGE) {
                if (daysRangeCount > 1L) "Add ($daysRangeCount Days)" else "Add Holiday"
            } else {
                if (selectedDates.size > 1) "Add (${selectedDates.size} Days)" else "Add Holiday"
            }

            Button(
                onClick = {
                    val finalName = holidayName.trim().ifEmpty { "Academic Holiday" }
                    val datesToAdd = if (selectionMode == HolidaySelectionMode.DATE_RANGE) {
                        val list = mutableListOf<LocalDate>()
                        var curr = startDate
                        while (!curr.isAfter(endDate)) {
                            list.add(curr)
                            curr = curr.plusDays(1)
                        }
                        list
                    } else {
                        selectedDates.toList().sorted()
                    }
                    onAddHolidays(datesToAdd, finalName)
                },
                enabled = isEnabled,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(buttonLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
