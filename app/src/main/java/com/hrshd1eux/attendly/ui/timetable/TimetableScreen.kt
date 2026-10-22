package com.hrshd1eux.attendly.ui.timetable

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Place
import com.hrshd1eux.attendly.domain.parser.TimetableTextParser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hrshd1eux.attendly.core.model.AttendanceUnitPolicy
import com.hrshd1eux.attendly.core.model.DayOfWeek
import com.hrshd1eux.attendly.core.util.TimeUtils
import com.hrshd1eux.attendly.domain.model.TimetableSession
import com.hrshd1eux.attendly.ui.components.AttendlyTopBar

@Composable
fun TimetableScreen(
    onNavigateBack: () -> Unit,
    onNavigateToImport: () -> Unit,
    viewModel: TimetableViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showShareMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val item = clipboard?.primaryClip?.getItemAt(0)
            val clipText = item?.text?.toString()
            viewModel.checkClipboardForTimetable(clipText)
        } catch (e: Exception) {
            // Ignore clipboard access restrictions
        }
    }

    fun shareTimetable(exportedText: String, title: String) {
        if (exportedText.isBlank()) {
            Toast.makeText(context, "No classes scheduled to share", Toast.LENGTH_SHORT).show()
            return
        }
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, exportedText)
        }
        val chooser = Intent.createChooser(sendIntent, "Share Timetable")
        context.startActivity(chooser)
    }

    if (uiState.isQrShareDialogOpen && uiState.qrShareBitmap != null) {
        QrShareDialog(
            bitmap = uiState.qrShareBitmap!!,
            title = uiState.qrShareTitle,
            onDismiss = viewModel::closeQrShareDialog
        )
    }

    Scaffold(
        topBar = {
            AttendlyTopBar(
                title = "Timetable",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                actions = {
                    Box {
                        IconButton(onClick = { showShareMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Timetable"
                            )
                        }
                        DropdownMenu(
                            expanded = showShareMenu,
                            onDismissRequest = { showShareMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Share Text (Full Week)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    showShareMenu = false
                                    viewModel.exportTimetable(null) { exportedText ->
                                        shareTimetable(exportedText, "My Weekly Timetable")
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share Text (${uiState.selectedDay.displayName})") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Today,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    showShareMenu = false
                                    viewModel.exportTimetable(uiState.selectedDay) { exportedText ->
                                        shareTimetable(exportedText, "${uiState.selectedDay.displayName} Timetable")
                                    }
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Share as QR Code (Full Week)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.QrCode,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    showShareMenu = false
                                    viewModel.openQrShareDialog(null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share as QR (${uiState.selectedDay.displayName})") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.QrCode2,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    showShareMenu = false
                                    viewModel.openQrShareDialog(uiState.selectedDay)
                                }
                            )
                        }
                    }
                    IconButton(onClick = viewModel::openAiPromptDialog) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI Prompt", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onNavigateToImport) {
                        Icon(imageVector = Icons.Default.FileUpload, contentDescription = "Import")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.activeSubjects.isNotEmpty()) {
                FloatingActionButton(
                    onClick = viewModel::openAddDialog,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Class")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.showClipboardPrompt && uiState.detectedClipboardText != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "📋 Timetable detected in clipboard",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Review and load it into your schedule directly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = viewModel::dismissClipboardPrompt) {
                                Text("Dismiss")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    viewModel.importDetectedClipboard()
                                    onNavigateToImport()
                                }
                            ) {
                                Text("Import")
                            }
                        }
                    }
                }
            }

            // Day Selector Tabs
            ScrollableTabRow(
                selectedTabIndex = uiState.selectedDay.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 16.dp
            ) {
                DayOfWeek.entries.forEach { day ->
                    Tab(
                        selected = uiState.selectedDay == day,
                        onClick = { viewModel.selectDay(day) },
                        text = {
                            Text(
                                text = day.shortName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    )
                }
            }

            if (uiState.activeSubjects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No subjects created yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add at least one subject first before scheduling classes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (uiState.sessionsForSelectedDay.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No classes on ${uiState.selectedDay.displayName}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the + button to add a class session, or use AI Import to paste your schedule.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.sessionsForSelectedDay, key = { it.id }) { session ->
                        TimetableSessionRow(
                            session = session,
                            onEdit = { viewModel.openEditDialog(session) },
                            onDelete = { viewModel.deleteSession(session.id) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }

        // Add/Edit Dialog
        if (uiState.isAddEditDialogOpen) {
            AddEditTimetableSessionDialog(
                sessionToEdit = uiState.sessionToEdit,
                defaultDay = uiState.selectedDay,
                subjects = uiState.activeSubjects,
                onDismiss = viewModel::closeAddEditDialog,
                onSave = viewModel::saveSession
            )
        }

        // AI Prompt Dialog
        if (uiState.isAiPromptDialogOpen) {
            AiPromptDialog(
                onDismiss = viewModel::closeAiPromptDialog,
                onNavigateToImport = onNavigateToImport
            )
        }
    }
}

@Composable
private fun TimetableSessionRow(
    session: TimetableSession,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.subjectName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${TimeUtils.minutesToTimeString(session.startMinute)} - ${TimeUtils.minutesToTimeString(session.endMinute)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (session.location.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = session.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                val teacher = TimetableTextParser.extractTeacher(session.notes) ?: session.notes.takeIf { !it.startsWith("UNCERTAIN") }
                if (!teacher.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val teacherText = if (teacher.startsWith("Teacher:", ignoreCase = true) ||
                            teacher.startsWith("Instructor:", ignoreCase = true) ||
                            teacher.startsWith("Guest:", ignoreCase = true)) {
                            teacher
                        } else {
                            "Teacher: $teacher"
                        }
                        Text(
                            text = teacherText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (session.attendanceUnitPolicy == AttendanceUnitPolicy.ONE_PER_HOUR) {
                    Text(
                        text = "Counts: 1 unit per hour",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
