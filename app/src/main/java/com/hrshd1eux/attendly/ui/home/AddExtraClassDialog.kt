package com.hrshd1eux.attendly.ui.home

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hrshd1eux.attendly.core.model.AttendanceStatus
import com.hrshd1eux.attendly.core.model.TimeFormat
import com.hrshd1eux.attendly.core.util.TimeUtils
import com.hrshd1eux.attendly.domain.model.Subject
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun AddExtraClassDialog(
    activeSubjects: List<Subject>,
    timeFormat: TimeFormat,
    defaultDate: LocalDate = LocalDate.now(),
    onSave: (
        subjectId: Long,
        date: LocalDate,
        startMinute: Int,
        endMinute: Int,
        location: String,
        status: AttendanceStatus,
        notes: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedSubject by remember { mutableStateOf(activeSubjects.firstOrNull()) }
    var subjectDropdownExpanded by remember { mutableStateOf(false) }

    val now = LocalTime.now()
    var startMinute by remember { mutableStateOf(now.hour * 60 + now.minute) }
    var endMinute by remember { mutableStateOf(now.hour * 60 + now.minute + 60) }

    var location by remember { mutableStateOf(selectedSubject?.location ?: "") }
    var notes by remember { mutableStateOf("Extra lecture") }
    var chosenStatus by remember { mutableStateOf(AttendanceStatus.PRESENT) }
    var statusDropdownExpanded by remember { mutableStateOf(false) }

    fun showTimePicker(initialMinute: Int, onTimeSet: (Int) -> Unit) {
        val h = initialMinute / 60
        val m = initialMinute % 60
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                onTimeSet(hourOfDay * 60 + minute)
            },
            h,
            m,
            timeFormat == TimeFormat.FORMAT_24H
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Extra / Substitute Class",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Log an ad-hoc or rescheduled class for today without changing your master timetable.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Subject Picker
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedSubject?.name ?: "Select Subject",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subject") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.clickable { subjectDropdownExpanded = true }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { subjectDropdownExpanded = true },
                        shape = RoundedCornerShape(12.dp)
                    )
                    DropdownMenu(
                        expanded = subjectDropdownExpanded,
                        onDismissRequest = { subjectDropdownExpanded = false }
                    ) {
                        activeSubjects.forEach { sub ->
                            DropdownMenuItem(
                                text = { Text(sub.name) },
                                onClick = {
                                    selectedSubject = sub
                                    if (location.isBlank() && sub.location.isNotBlank()) {
                                        location = sub.location
                                    }
                                    subjectDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Time Pickers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            showTimePicker(startMinute) { newStart ->
                                startMinute = newStart
                                if (endMinute <= newStart) endMinute = newStart + 60
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(TimeUtils.minutesToTimeString(startMinute, timeFormat))
                    }

                    OutlinedButton(
                        onClick = {
                            showTimePicker(endMinute) { newEnd ->
                                endMinute = newEnd
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(TimeUtils.minutesToTimeString(endMinute, timeFormat))
                    }
                }

                // Status Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    val statusLabel = when (chosenStatus) {
                        AttendanceStatus.PRESENT -> "✓ Present"
                        AttendanceStatus.ABSENT -> "✕ Absent"
                        AttendanceStatus.CANCELLED -> "🚫 Cancelled"
                        AttendanceStatus.ON_DUTY -> "🎖️ On-Duty (OD)"
                        AttendanceStatus.MEDICAL_LEAVE -> "🏥 Medical Leave"
                    }
                    OutlinedTextField(
                        value = statusLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Attendance Status") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.clickable { statusDropdownExpanded = true }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { statusDropdownExpanded = true },
                        shape = RoundedCornerShape(12.dp)
                    )
                    DropdownMenu(
                        expanded = statusDropdownExpanded,
                        onDismissRequest = { statusDropdownExpanded = false }
                    ) {
                        AttendanceStatus.entries.forEach { st ->
                            val label = when (st) {
                                AttendanceStatus.PRESENT -> "✓ Present"
                                AttendanceStatus.ABSENT -> "✕ Absent"
                                AttendanceStatus.CANCELLED -> "🚫 Cancelled"
                                AttendanceStatus.ON_DUTY -> "🎖️ On-Duty (OD)"
                                AttendanceStatus.MEDICAL_LEAVE -> "🏥 Medical Leave"
                            }
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    chosenStatus = st
                                    statusDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Location Field
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location / Room (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Note Field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Note / Topic (Optional)") },
                    singleLine = true,
                    placeholder = { Text("e.g. Substitute lecture, Extra lab") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sub = selectedSubject ?: return@Button
                    onSave(
                        sub.id,
                        defaultDate,
                        startMinute,
                        endMinute,
                        location.trim(),
                        chosenStatus,
                        notes.trim()
                    )
                },
                enabled = selectedSubject != null
            ) {
                Text("Record Class")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
