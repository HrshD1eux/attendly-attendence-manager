package com.hrshd1eux.attendly.ui.timetable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hrshd1eux.attendly.core.model.AttendanceUnitPolicy
import com.hrshd1eux.attendly.core.model.DayOfWeek
import com.hrshd1eux.attendly.core.util.TimeUtils
import com.hrshd1eux.attendly.domain.model.Subject
import com.hrshd1eux.attendly.domain.model.TimetableSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTimetableSessionDialog(
    sessionToEdit: TimetableSession?,
    defaultDay: DayOfWeek,
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onSave: (
        subjectId: Long,
        dayOfWeek: DayOfWeek,
        startMinute: Int,
        endMinute: Int,
        location: String,
        policy: AttendanceUnitPolicy,
        notes: String
    ) -> Unit
) {
    var selectedDay by remember { mutableStateOf(sessionToEdit?.dayOfWeek ?: defaultDay) }
    var selectedSubjectId by remember {
        mutableStateOf(sessionToEdit?.subjectId ?: subjects.firstOrNull()?.id ?: 0L)
    }

    // Default start 9:00 (540), end 10:00 (600)
    var startHour by remember { mutableIntStateOf((sessionToEdit?.startMinute ?: 540) / 60) }
    var startMinute by remember { mutableIntStateOf((sessionToEdit?.startMinute ?: 540) % 60) }
    var endHour by remember { mutableIntStateOf((sessionToEdit?.endMinute ?: 600) / 60) }
    var endMinute by remember { mutableIntStateOf((sessionToEdit?.endMinute ?: 600) % 60) }

    var location by remember { mutableStateOf(sessionToEdit?.location ?: "") }
    var unitPolicy by remember {
        mutableStateOf(sessionToEdit?.attendanceUnitPolicy ?: AttendanceUnitPolicy.ONE_PER_SESSION)
    }
    var notes by remember { mutableStateOf(sessionToEdit?.notes ?: "") }

    var subjectExpanded by remember { mutableStateOf(false) }
    var dayExpanded by remember { mutableStateOf(false) }

    val startTotal = startHour * 60 + startMinute
    val endTotal = endHour * 60 + endMinute
    val isTimeValid = endTotal > startTotal

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (sessionToEdit == null) "Add Class Session" else "Edit Class Session",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Day of Week Picker
                ExposedDropdownMenuBox(
                    expanded = dayExpanded,
                    onExpandedChange = { dayExpanded = !dayExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedDay.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Day of Week") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = dayExpanded,
                        onDismissRequest = { dayExpanded = false }
                    ) {
                        DayOfWeek.entries.forEach { day ->
                            DropdownMenuItem(
                                text = { Text(day.displayName) },
                                onClick = {
                                    selectedDay = day
                                    dayExpanded = false
                                }
                            )
                        }
                    }
                }

                // Subject Selector
                ExposedDropdownMenuBox(
                    expanded = subjectExpanded,
                    onExpandedChange = { subjectExpanded = !subjectExpanded }
                ) {
                    val currentSubjectName = subjects.find { it.id == selectedSubjectId }?.name ?: "Select Subject"
                    OutlinedTextField(
                        value = currentSubjectName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subject *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = subjectExpanded,
                        onDismissRequest = { subjectExpanded = false }
                    ) {
                        subjects.forEach { subject ->
                            DropdownMenuItem(
                                text = { Text(subject.name) },
                                onClick = {
                                    selectedSubjectId = subject.id
                                    subjectExpanded = false
                                }
                            )
                        }
                    }
                }

                // Time Pickers (Start & End)
                Text(
                    text = "Class Schedule Time",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = String.format("%02d:%02d", startHour, startMinute),
                        onValueChange = { str ->
                            TimeUtils.parseTimeToMinutes(str)?.let {
                                startHour = it / 60
                                startMinute = it % 60
                            }
                        },
                        label = { Text("Start (HH:mm)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = String.format("%02d:%02d", endHour, endMinute),
                        onValueChange = { str ->
                            TimeUtils.parseTimeToMinutes(str)?.let {
                                endHour = it / 60
                                endMinute = it % 60
                            }
                        },
                        label = { Text("End (HH:mm)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        isError = !isTimeValid,
                        supportingText = if (!isTimeValid) { { Text("End must be after start") } } else null
                    )
                }

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Classroom / Location") },
                    placeholder = { Text("Room 101 or Lab 3") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Attendance unit counting policy (Section 12.2)
                Text(
                    text = "How should this session count?",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "Configure how laboratories or multi-hour lectures contribute to attendance.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { unitPolicy = AttendanceUnitPolicy.ONE_PER_SESSION },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = unitPolicy == AttendanceUnitPolicy.ONE_PER_SESSION,
                        onClick = { unitPolicy = AttendanceUnitPolicy.ONE_PER_SESSION }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "1 unit for entire session (Default)",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { unitPolicy = AttendanceUnitPolicy.ONE_PER_HOUR },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = unitPolicy == AttendanceUnitPolicy.ONE_PER_HOUR,
                        onClick = { unitPolicy = AttendanceUnitPolicy.ONE_PER_HOUR }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "1 unit per hour (e.g. 3 units for 3h lab)",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isTimeValid && selectedSubjectId != 0L) {
                        onSave(
                            selectedSubjectId,
                            selectedDay,
                            startTotal,
                            endTotal,
                            location,
                            unitPolicy,
                            notes
                        )
                    }
                },
                enabled = isTimeValid && selectedSubjectId != 0L,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (sessionToEdit == null) "Add" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
