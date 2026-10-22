package com.hrshd1eux.attendly.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrshd1eux.attendly.core.model.AttendanceStatus
import com.hrshd1eux.attendly.core.util.TimeUtils
import com.hrshd1eux.attendly.domain.model.ClassSession
import com.hrshd1eux.attendly.domain.model.Subject
import com.hrshd1eux.attendly.domain.model.TimetableSession
import com.hrshd1eux.attendly.ui.components.AttendanceStatusBadge
import com.hrshd1eux.attendly.ui.theme.LocalStatusColors
import com.hrshd1eux.attendly.ui.theme.SubjectColorPalette
import java.time.LocalDate

@Composable
fun RecordDateClassesDialog(
    selectedDate: LocalDate,
    scheduledSessions: List<TimetableSession>,
    alreadyRecordedSessions: List<ClassSession>,
    activeSubjects: List<Subject>,
    onDismiss: () -> Unit,
    onMarkAllPresent: () -> Unit,
    onRecordSelected: (List<Pair<TimetableSession, AttendanceStatus>>) -> Unit,
    onRecordAdHoc: (subjectId: Long, status: AttendanceStatus) -> Unit
) {
    val statusColors = LocalStatusColors.current

    val recordedMap = remember(alreadyRecordedSessions) {
        alreadyRecordedSessions.associateBy { it.timetableSessionId }
    }

    // Multi-selection state: sessionId -> isSelected
    val selectedSessionsMap = remember(scheduledSessions) {
        val map = mutableStateMapOf<Long, Boolean>()
        scheduledSessions.forEach { s ->
            // Pre-select if not already recorded
            map[s.id] = !recordedMap.containsKey(s.id)
        }
        map
    }

    var chosenStatus by remember { mutableStateOf(AttendanceStatus.PRESENT) }
    var isExtraClassExpanded by remember { mutableStateOf(scheduledSessions.isEmpty()) }
    var extraSubjectId by remember { mutableStateOf(activeSubjects.firstOrNull()?.id ?: 0L) }
    var extraStatus by remember { mutableStateOf(AttendanceStatus.PRESENT) }

    val selectedCount = selectedSessionsMap.count { it.value }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EventNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Record Attendance",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Text(
                    text = TimeUtils.formatDate(selectedDate, "EEEE, MMMM d, yyyy"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (scheduledSessions.isNotEmpty()) {
                    // Quick Action: "Mark All Present" Button
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = statusColors.presentContainer.copy(alpha = 0.6f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onMarkAllPresent()
                                    onDismiss()
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(statusColors.present.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = null,
                                        tint = statusColors.present,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Mark All ${scheduledSessions.size} Classes Present",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = statusColors.onPresentContainer
                                        )
                                    )
                                    Text(
                                        text = "1-tap record for entire day",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = statusColors.onPresentContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = statusColors.present,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Multi-select Timetable Sessions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Scheduled Classes (${scheduledSessions.size})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        TextButton(
                            onClick = {
                                val allSelected = selectedSessionsMap.values.all { it }
                                scheduledSessions.forEach { s ->
                                    selectedSessionsMap[s.id] = !allSelected
                                }
                            }
                        ) {
                            val allSelected = selectedSessionsMap.values.all { it }
                            Text(
                                text = if (allSelected) "Deselect All" else "Select All",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(scheduledSessions, key = { it.id }) { session ->
                            val isChecked = selectedSessionsMap[session.id] ?: false
                            val alreadyRecorded = recordedMap[session.id]
                            val subjectColor = SubjectColorPalette[session.subjectColorKey] ?: MaterialTheme.colorScheme.primary

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedSessionsMap[session.id] = !isChecked
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isChecked)
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                    else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            selectedSessionsMap[session.id] = checked
                                        }
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(width = 4.dp, height = 36.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(subjectColor)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = session.subjectName,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${TimeUtils.minutesToTimeString(session.startMinute)} - ${TimeUtils.minutesToTimeString(session.endMinute)}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (session.location.isNotBlank()) {
                                                Text(
                                                    text = "• ${session.location}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }

                                    if (alreadyRecorded != null) {
                                        AttendanceStatusBadge(status = alreadyRecorded.status)
                                    }
                                }
                            }
                        }
                    }

                    // Status Picker for Selected Items
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Mark Selected As:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AttendanceStatus.entries.forEach { status ->
                                val isSelected = chosenStatus == status
                                val (bg, fg) = when (status) {
                                    AttendanceStatus.PRESENT -> statusColors.presentContainer to statusColors.onPresentContainer
                                    AttendanceStatus.ABSENT -> statusColors.absentContainer to statusColors.onAbsentContainer
                                    AttendanceStatus.CANCELLED -> statusColors.cancelledContainer to statusColors.onCancelledContainer
                                    AttendanceStatus.ON_DUTY -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                                    AttendanceStatus.MEDICAL_LEAVE -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) bg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .clickable { chosenStatus = status }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = status.name.lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) fg else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No timetable classes scheduled for ${TimeUtils.formatDate(selectedDate, "EEEE")}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                // Expandable Section: Extra / Unscheduled Class
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isExtraClassExpanded = !isExtraClassExpanded }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "+ Record Extra / Unscheduled Class",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = if (isExtraClassExpanded) "Hide" else "Show",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (isExtraClassExpanded && activeSubjects.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Select Subject:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                        )

                        activeSubjects.take(5).forEach { sub ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { extraSubjectId = sub.id }
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = extraSubjectId == sub.id,
                                    onClick = { extraSubjectId = sub.id }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = sub.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                AttendanceStatus.entries.forEach { st ->
                                    val isSel = extraStatus == st
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                            )
                                            .clickable { extraStatus = st }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = st.name.take(1),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    if (extraSubjectId != 0L) {
                                        onRecordAdHoc(extraSubjectId, extraStatus)
                                        onDismiss()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Add Class")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (scheduledSessions.isNotEmpty()) {
                Button(
                    onClick = {
                        val pairs = scheduledSessions
                            .filter { selectedSessionsMap[it.id] == true }
                            .map { it to chosenStatus }
                        if (pairs.isNotEmpty()) {
                            onRecordSelected(pairs)
                        } else {
                            onDismiss()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    enabled = selectedCount > 0
                ) {
                    Text("Save Selected ($selectedCount)")
                }
            } else {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
