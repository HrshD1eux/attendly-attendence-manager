package com.hrshd1eux.attendly.ui.home

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
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Class
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hrshd1eux.attendly.core.model.AttendanceStatus
import com.hrshd1eux.attendly.ui.components.DashboardStatRow
import com.hrshd1eux.attendly.ui.components.LeaveConfirmationDialog
import com.hrshd1eux.attendly.ui.components.OnLeaveBanner
import com.hrshd1eux.attendly.ui.components.OverallAttendanceCard
import com.hrshd1eux.attendly.ui.components.SmartClassPromptCard
import com.hrshd1eux.attendly.ui.components.SubjectAttendanceCard
import com.hrshd1eux.attendly.ui.components.TodayScheduleCard
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HomeScreen(
    onNavigateToSubjectDetail: (Long) -> Unit,
    onNavigateToAddSubject: () -> Unit,
    onNavigateToTimetable: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLeaveDialog by remember { mutableStateOf(false) }

    if (showLeaveDialog) {
        LeaveConfirmationDialog(
            onDismiss = { showLeaveDialog = false },
            onConfirmLeave = { option ->
                showLeaveDialog = false
                viewModel.setOnLeaveToday(option)
            }
        )
    }

    // Listen to undo events
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is HomeUiEvent.ShowUndoSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoAction(event.classSessionId, event.previousStatus)
                    }
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Avatar + "Welcome back, {userName}" + Quick Actions
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // User Avatar Circle
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (uiState.userName.ifBlank { "User" }).take(1).uppercase(),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Welcome back,",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = uiState.userName,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Top Action Icons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onNavigateToCalendar,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Calendar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        FilledIconButton(
                            onClick = onNavigateToAddSubject,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Subject",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Leave Banner or Smart Class Prompt
            if (uiState.userPreferences.isOnLeaveToday) {
                item {
                    OnLeaveBanner(
                        onCancelLeave = { viewModel.cancelLeave() }
                    )
                }
            } else if (uiState.activeClassPrompt != null) {
                item {
                    SmartClassPromptCard(
                        classItem = uiState.activeClassPrompt!!,
                        timeFormat = uiState.userPreferences.timeFormat,
                        onMarkStatus = { item, status ->
                            viewModel.markTodayClass(item, status)
                        },
                        onOpenLeaveDialog = { showLeaveDialog = true }
                    )
                }
            }

            // Today's Schedule Card (Timeline view inspired by user mockup)
            item {
                TodayScheduleCard(
                    items = uiState.todayClasses,
                    currentDate = uiState.currentDate,
                    timeFormat = uiState.userPreferences.timeFormat,
                    onMarkStatus = { item, status ->
                        viewModel.markTodayClass(item, status)
                    },
                    onClearStatus = { item ->
                        viewModel.clearClassStatus(item)
                    },
                    onNavigateToTimetable = onNavigateToTimetable,
                    onOpenLeaveDialog = { showLeaveDialog = true }
                )
            }

            // Stat Cards Row (Attendance % and Safe Bunks cards inspired by user mockup)
            item {
                uiState.overallSummary?.let { summary ->
                    DashboardStatRow(summary = summary)
                }
            }

            // Overall Summary Card
            item {
                uiState.overallSummary?.let { summary ->
                    OverallAttendanceCard(summary = summary)
                }
            }

            // Quick Actions Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionButton(
                        icon = Icons.Default.Add,
                        label = "Add Subject",
                        onClick = onNavigateToAddSubject,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        icon = Icons.AutoMirrored.Filled.EventNote,
                        label = "Timetable",
                        onClick = onNavigateToTimetable,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        icon = Icons.Default.CalendarMonth,
                        label = "Calendar",
                        onClick = onNavigateToCalendar,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Subjects Summary Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Subjects",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (uiState.subjectDetails.isNotEmpty()) {
                        Text(
                            text = "${uiState.subjectDetails.size} total",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Subject Cards List
            if (uiState.subjectDetails.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Class,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No subjects added yet",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Add subjects to track attendance, targets, and safe absences.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(uiState.subjectDetails, key = { it.subject.id }) { detail ->
                    SubjectAttendanceCard(
                        detail = detail,
                        onClick = { onNavigateToSubjectDetail(detail.subject.id) },
                        onMarkPresent = {
                            viewModel.markSubjectAttendance(detail.subject.id, detail.subject.name, AttendanceStatus.PRESENT)
                        },
                        onMarkAbsent = {
                            viewModel.markSubjectAttendance(detail.subject.id, detail.subject.name, AttendanceStatus.ABSENT)
                        },
                        onMarkCancelled = {
                            viewModel.markSubjectAttendance(detail.subject.id, detail.subject.name, AttendanceStatus.CANCELLED)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
