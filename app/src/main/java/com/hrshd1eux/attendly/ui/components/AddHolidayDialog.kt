package com.hrshd1eux.attendly.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hrshd1eux.attendly.core.util.TimeUtils
import java.time.LocalDate
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

    // Specific dates state
    val specificDates = remember { mutableStateListOf(initialStartDate) }

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

    val openAddSpecificDatePicker = {
        val lastDate = specificDates.lastOrNull() ?: initialStartDate
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val picked = LocalDate.of(year, month + 1, dayOfMonth)
                if (!specificDates.contains(picked)) {
                    specificDates.add(picked)
                    specificDates.sort()
                }
            },
            lastDate.year,
            lastDate.monthValue - 1,
            lastDate.dayOfMonth
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
                    // SPECIFIC_DATES mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${specificDates.size} date(s) selected",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedButton(
                            onClick = openAddSpecificDatePicker,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Date", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    if (specificDates.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            specificDates.forEach { date ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = TimeUtils.formatDate(date, "EEE, MMMM d, yyyy"),
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                        )
                                    }

                                    if (specificDates.size > 1) {
                                        IconButton(
                                            onClick = { specificDates.remove(date) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove date",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
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
                specificDates.isNotEmpty()
            }

            val buttonLabel = if (selectionMode == HolidaySelectionMode.DATE_RANGE) {
                if (daysRangeCount > 1L) "Add ($daysRangeCount Days)" else "Add Holiday"
            } else {
                if (specificDates.size > 1) "Add (${specificDates.size} Days)" else "Add Holiday"
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
                        specificDates.toList().sorted()
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
