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
import android.app.TimePickerDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
import com.hrshd1eux.attendly.core.model.AttendanceUnitPolicy
import com.hrshd1eux.attendly.core.model.DayOfWeek
import com.hrshd1eux.attendly.core.util.TimeUtils
import com.hrshd1eux.attendly.domain.model.Subject
import com.hrshd1eux.attendly.domain.model.TimetableSession
import com.hrshd1eux.attendly.domain.parser.TimetableTextParser

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
    val context = LocalContext.current

    var selectedDay by remember { mutableStateOf(sessionToEdit?.dayOfWeek ?: defaultDay) }
    var selectedSubjectId by remember {
        mutableStateOf(sessionToEdit?.subjectId ?: subjects.firstOrNull()?.id ?: 0L)
    }

    val initialStartMin = sessionToEdit?.startMinute ?: 540
    val initialEndMin = sessionToEdit?.endMinute ?: 600

    var startTimeText by remember {
        mutableStateOf(String.format(java.util.Locale.US, "%02d:%02d", initialStartMin / 60, initialStartMin % 60))
    }
    var endTimeText by remember {
        mutableStateOf(String.format(java.util.Locale.US, "%02d:%02d", initialEndMin / 60, initialEndMin % 60))
    }

    var location by remember { mutableStateOf(sessionToEdit?.location ?: "") }
    var teacher by remember {
        mutableStateOf(
            TimetableTextParser.extractTeacher(sessionToEdit?.notes ?: "")
                ?: sessionToEdit?.notes?.takeIf { !it.startsWith("UNCERTAIN") }
                ?: ""
        )
    }
    var unitPolicy by remember {
        mutableStateOf(sessionToEdit?.attendanceUnitPolicy ?: AttendanceUnitPolicy.ONE_PER_SESSION)
    }

    var subjectExpanded by remember { mutableStateOf(false) }
    var dayExpanded by remember { mutableStateOf(false) }

    val parsedStart = TimeUtils.parseTimeToMinutes(startTimeText)
    val parsedEnd = TimeUtils.parseTimeToMinutes(endTimeText)

    val isStartValid = parsedStart != null
    val isEndValid = parsedEnd != null
    val isTimeRangeValid = isStartValid && isEndValid && parsedEnd!! > parsedStart!!
    val isFormValid = isTimeRangeValid && selectedSubjectId != 0L

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
                        value = startTimeText,
                        onValueChange = { startTimeText = it },
                        label = { Text("Start (HH:mm)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        isError = !isStartValid && startTimeText.isNotBlank(),
                        supportingText = if (!isStartValid && startTimeText.isNotBlank()) {
                            { Text("Format: HH:mm (e.g. 09:30)") }
                        } else null,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val h = parsedStart?.div(60) ?: (initialStartMin / 60)
                                    val m = parsedStart?.rem(60) ?: (initialStartMin % 60)
                                    TimePickerDialog(
                                        context,
                                        { _, selectedH, selectedM ->
                                            startTimeText = String.format(java.util.Locale.US, "%02d:%02d", selectedH, selectedM)
                                        },
                                        h,
                                        m,
                                        false
                                    ).show()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Pick start time",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )

                    OutlinedTextField(
                        value = endTimeText,
                        onValueChange = { endTimeText = it },
                        label = { Text("End (HH:mm)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        isError = (!isEndValid && endTimeText.isNotBlank()) || (isStartValid && isEndValid && !isTimeRangeValid),
                        supportingText = if (!isEndValid && endTimeText.isNotBlank()) {
                            { Text("Format: HH:mm (e.g. 10:30)") }
                        } else if (isStartValid && isEndValid && !isTimeRangeValid) {
                            { Text("End must be after start") }
                        } else null,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val h = parsedEnd?.div(60) ?: (initialEndMin / 60)
                                    val m = parsedEnd?.rem(60) ?: (initialEndMin % 60)
                                    TimePickerDialog(
                                        context,
                                        { _, selectedH, selectedM ->
                                            endTimeText = String.format(java.util.Locale.US, "%02d:%02d", selectedH, selectedM)
                                        },
                                        h,
                                        m,
                                        false
                                    ).show()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Pick end time",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                }

                // Classroom / Location with clear button
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Classroom / Location") },
                    placeholder = { Text("Room 101, Lab 3, LH-2") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = if (location.isNotEmpty()) {
                        {
                            IconButton(onClick = { location = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear location",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else null
                )

                // Teacher / Instructor with clear button
                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text("Teacher / Instructor (Optional)") },
                    placeholder = { Text("e.g. Dr. Navdeep Mor or Prof. Pallavi") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = if (teacher.isNotEmpty()) {
                        {
                            IconButton(onClick = { teacher = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear teacher",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else null
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
                    if (isFormValid && parsedStart != null && parsedEnd != null) {
                        onSave(
                            selectedSubjectId,
                            selectedDay,
                            parsedStart,
                            parsedEnd,
                            location.trim(),
                            unitPolicy,
                            teacher.trim()
                        )
                    }
                },
                enabled = isFormValid,
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
