package com.hrshd1eux.attendly.ui.subjects

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hrshd1eux.attendly.core.model.AttendanceStatus
import com.hrshd1eux.attendly.core.util.TimeUtils
import com.hrshd1eux.attendly.domain.model.AttendanceStanding
import com.hrshd1eux.attendly.domain.model.ClassSession
import com.hrshd1eux.attendly.ui.components.AttendanceStatusBadge
import com.hrshd1eux.attendly.ui.components.AttendlyTopBar
import com.hrshd1eux.attendly.ui.components.StandingBadge
import com.hrshd1eux.attendly.ui.theme.LocalStatusColors
import com.hrshd1eux.attendly.ui.theme.SubjectColorPalette

@Composable
fun SubjectDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: SubjectDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val detail = uiState.detail
    val statusColors = LocalStatusColors.current

    if (detail == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Subject not found")
        }
        return
    }

    val subject = detail.subject
    val summary = detail.summary
    val subjectColor = SubjectColorPalette[subject.colorKey] ?: MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            AttendlyTopBar(
                title = subject.name,
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(onClick = viewModel::openEditDialog) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = viewModel::toggleArchive) {
                        Icon(
                            imageVector = if (subject.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                            contentDescription = if (subject.isArchived) "Unarchive" else "Archive"
                        )
                    }
                    IconButton(onClick = viewModel::openDeleteDialog) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
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
            // Main Percentage Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (subject.shortCode.isNotEmpty()) "${subject.shortCode} • Target: ${subject.targetPercentage}%" else "Target: ${subject.targetPercentage}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = summary.formattedPercentage,
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = when (summary.standing) {
                                            AttendanceStanding.BELOW_TARGET -> statusColors.absent
                                            AttendanceStanding.ON_TRACK -> statusColors.warning
                                            AttendanceStanding.NO_DATA -> MaterialTheme.colorScheme.onSurfaceVariant
                                            else -> statusColors.present
                                        }
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            StandingBadge(standing = summary.standing)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        LinearProgressIndicator(
                            progress = { (summary.percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = when (summary.standing) {
                                AttendanceStanding.BELOW_TARGET -> statusColors.absent
                                AttendanceStanding.ON_TRACK -> statusColors.warning
                                else -> statusColors.present
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Stats Breakdown Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatMiniItem("Attended", summary.presentUnits.toString(), statusColors.present)
                            StatMiniItem("Missed", summary.absentUnits.toString(), statusColors.absent)
                            StatMiniItem("Cancelled", summary.cancelledUnits.toString(), statusColors.cancelled)
                            StatMiniItem("Total", summary.totalEligibleUnits.toString(), MaterialTheme.colorScheme.onSurface)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Safe Absence / Recovery Message Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (summary.standing == AttendanceStanding.BELOW_TARGET)
                                        statusColors.absentContainer
                                    else
                                        statusColors.presentContainer
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = summary.statusMessage,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (summary.standing == AttendanceStanding.BELOW_TARGET)
                                        statusColors.onAbsentContainer
                                    else
                                        statusColors.onPresentContainer
                                )
                            )
                        }
                    }
                }
            }

            // Quick Mark for Today
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Record Attendance Today",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // "P" Present Button
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = statusColors.present,
                                shadowElevation = 1.5.dp,
                                modifier = Modifier
                                    .size(width = 38.dp, height = 36.dp)
                                    .clickable { viewModel.recordAttendance(AttendanceStatus.PRESENT) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "P",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            fontSize = 16.sp
                                        )
                                    )
                                }
                            }

                            // "A" Absent Button
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = statusColors.absent,
                                shadowElevation = 1.5.dp,
                                modifier = Modifier
                                    .size(width = 38.dp, height = 36.dp)
                                    .clickable { viewModel.recordAttendance(AttendanceStatus.ABSENT) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "A",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            fontSize = 16.sp
                                        )
                                    )
                                }
                            }

                            // "C" Cancelled Button
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(width = 38.dp, height = 36.dp)
                                    .clickable { viewModel.recordAttendance(AttendanceStatus.CANCELLED) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "C",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 15.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Attendance History Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Attendance History",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${detail.recentSessions.size} sessions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (detail.recentSessions.isEmpty()) {
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
                                text = "No recorded sessions yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(detail.recentSessions, key = { it.id }) { session ->
                    SessionHistoryRow(
                        session = session,
                        onUpdateStatus = { newStatus -> viewModel.updateSessionStatus(session.id, newStatus) },
                        onDelete = { viewModel.deleteSession(session.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Edit Dialog
        if (uiState.isEditDialogOpen) {
            AddEditSubjectDialog(
                subjectToEdit = subject,
                defaultTarget = subject.targetPercentage,
                onDismiss = viewModel::closeEditDialog,
                onSave = viewModel::updateSubject
            )
        }

        // Delete Confirmation Dialog
        if (uiState.isDeleteDialogOpen) {
            AlertDialog(
                onDismissRequest = viewModel::closeDeleteDialog,
                title = { Text("Delete Subject?") },
                text = {
                    Text("Deleting \"${subject.name}\" will permanently remove all associated timetable sessions and attendance history. This action cannot be undone.")
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.deleteSubject(onNavigateBack) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::closeDeleteDialog) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun StatMiniItem(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SessionHistoryRow(
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
                    text = TimeUtils.formatDate(session.date, "EEE, MMM d, yyyy"),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                if (session.startMinute > 0 || session.endMinute > 0) {
                    Text(
                        text = "${TimeUtils.minutesToTimeString(session.startMinute)} - ${TimeUtils.minutesToTimeString(session.endMinute)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            text = { Text("Delete Record", color = MaterialTheme.colorScheme.error) },
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
