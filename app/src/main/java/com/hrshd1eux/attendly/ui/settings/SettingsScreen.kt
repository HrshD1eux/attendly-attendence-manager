package com.hrshd1eux.attendly.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.hrshd1eux.attendly.BuildConfig
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.draw.clip
import com.hrshd1eux.attendly.core.util.ApkInstaller
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hrshd1eux.attendly.core.model.AppTheme
import com.hrshd1eux.attendly.core.model.AttendanceUnitPolicy
import com.hrshd1eux.attendly.core.model.DayOfWeek
import com.hrshd1eux.attendly.core.model.TimeFormat
import com.hrshd1eux.attendly.ui.components.AttendlyTopBar
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onNavigateToTimetable: () -> Unit,
    onNavigateToImportTimetable: () -> Unit,
    onNavigateToHolidays: () -> Unit,
    onNavigateToBackupRestore: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val prefs by viewModel.uiState.collectAsState()
    val isEditNameOpen by viewModel.isEditNameDialogOpen.collectAsState()
    val isExplanationOpen by viewModel.isExplanationDialogOpen.collectAsState()
    val updateState by viewModel.updateCheckState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AttendlyTopBar(title = "Settings")
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // User Profile Section
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = "Your Name",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = prefs.userName.ifEmpty { "Student" },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(onClick = viewModel::openEditNameDialog) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Name")
                        }
                    }
                }
            }

            // Appearance Section
            item {
                SettingsSectionHeader("Appearance")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "Theme",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            ThemeOption("System", prefs.appTheme == AppTheme.SYSTEM) { viewModel.setTheme(AppTheme.SYSTEM) }
                            ThemeOption("Light", prefs.appTheme == AppTheme.LIGHT) { viewModel.setTheme(AppTheme.LIGHT) }
                            ThemeOption("Dark", prefs.appTheme == AppTheme.DARK) { viewModel.setTheme(AppTheme.DARK) }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Time Format",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { viewModel.setTimeFormat(TimeFormat.FORMAT_12H) }
                            ) {
                                RadioButton(
                                    selected = prefs.timeFormat == TimeFormat.FORMAT_12H,
                                    onClick = { viewModel.setTimeFormat(TimeFormat.FORMAT_12H) }
                                )
                                Text("12-Hour (AM/PM)")
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { viewModel.setTimeFormat(TimeFormat.FORMAT_24H) }
                            ) {
                                RadioButton(
                                    selected = prefs.timeFormat == TimeFormat.FORMAT_24H,
                                    onClick = { viewModel.setTimeFormat(TimeFormat.FORMAT_24H) }
                                )
                                Text("24-Hour")
                            }
                        }
                    }
                }
            }

            // Attendance Target Section
            item {
                SettingsSectionHeader("Attendance Rules")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Default Target Percentage",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "${prefs.defaultTargetPercentage}%",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        Slider(
                            value = prefs.defaultTargetPercentage.toFloat(),
                            onValueChange = { viewModel.setDefaultTarget(it.toInt()) },
                            valueRange = 50f..100f,
                            steps = 49,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        SettingsRowItem(
                            icon = Icons.AutoMirrored.Filled.HelpOutline,
                            title = "How Attendance is Calculated",
                            subtitle = "Learn how percentages and safe bunks work",
                            onClick = viewModel::openExplanationDialog
                        )
                    }
                }
            }

            // Timetable Management Section
            item {
                SettingsSectionHeader("Timetable")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        SettingsRowItem(
                            icon = Icons.Default.Schedule,
                            title = "Manage Timetable",
                            subtitle = "View and edit your weekly classes",
                            onClick = onNavigateToTimetable
                        )
                        SettingsRowItem(
                            icon = Icons.Default.AutoAwesome,
                            title = "Import Timetable / AI Prompt",
                            subtitle = "Paste schedules or convert with AI prompt",
                            onClick = onNavigateToImportTimetable
                        )
                        SettingsRowItem(
                            icon = Icons.Default.Celebration,
                            title = "Academic Holidays & AI Prompt",
                            subtitle = "Import university calendar, dates & manage holidays",
                            onClick = onNavigateToHolidays
                        )
                        SettingsRowItem(
                            icon = Icons.Default.AutoStories,
                            title = "Start Next Semester",
                            subtitle = "Archive current subjects to past semesters & reset timetable",
                            onClick = viewModel::openStartSemesterDialog
                        )
                    }
                }
            }

            // Data & Backup Section
            item {
                SettingsSectionHeader("Data & Backup")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        SettingsRowItem(
                            icon = Icons.Default.Backup,
                            title = "Backup & Restore",
                            subtitle = "Export JSON, restore, or export CSV records",
                            onClick = onNavigateToBackupRestore
                        )
                    }
                }
            }

            // About Section
            item {
                SettingsSectionHeader("About Attendly")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        SettingsRowItem(
                            icon = Icons.Default.Info,
                            title = "About Attendly",
                            subtitle = "Version ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE}) • Offline-first Attendance Tracker",
                            onClick = { showAboutDialog = true }
                        )
                        SettingsRowItem(
                            icon = Icons.Default.School,
                            title = "Privacy Policy",
                            subtitle = "100% offline, zero tracking or background telemetry",
                            onClick = { showPrivacyDialog = true }
                        )
                        SettingsRowItem(
                            icon = Icons.Default.SystemUpdate,
                            title = "Check for Updates",
                            subtitle = "Current: v${BuildConfig.VERSION_NAME} • Tap to check GitHub releases",
                            onClick = { viewModel.checkForUpdates() }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Edit Name Dialog
        if (isEditNameOpen) {
            var tempName by remember { mutableStateOf(prefs.userName) }
            AlertDialog(
                onDismissRequest = viewModel::closeEditNameDialog,
                title = { Text("Edit Your Name") },
                text = {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Your Name") },
                        placeholder = { Text("e.g. Raghav") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.setUserName(tempName) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::closeEditNameDialog) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Attendance Calculation Explanation Dialog
        if (isExplanationOpen) {
            AlertDialog(
                onDismissRequest = viewModel::closeExplanationDialog,
                title = { Text("Attendance Calculations") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "• Attendance Percentage: (Attended classes / Eligible classes) × 100.\nCancelled classes are excluded from the count.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "• Safe Absences: The exact maximum number of additional classes you can safely miss while keeping your attendance at or above your target percentage.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "• Recovery Classes: The minimum number of consecutive upcoming classes you must attend to recover your attendance back to your target.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = viewModel::closeExplanationDialog) {
                        Text("Understood")
                    }
                }
            )
        }

        // About Dialog
        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                title = { Text("Attendly v${BuildConfig.VERSION_NAME}") },
                text = {
                    Text(
                        "Attendly is a native, modern, offline-first attendance tracking application built with Kotlin, Jetpack Compose, and Room.\n\nCrafted specifically for students to effortlessly monitor class attendance, calculate safe absences, and maintain academic goals.\n\nVersion: ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
                    )
                },
                confirmButton = {
                    Button(onClick = { showAboutDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }

        // Update Checking Dialogs
        when (val state = updateState) {
            is UpdateCheckState.Checking -> {
                AlertDialog(
                    onDismissRequest = { /* Don't dismiss while checking */ },
                    title = { Text("Checking for Updates") },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                            Text("Verifying with GitHub Releases...")
                        }
                    },
                    confirmButton = {}
                )
            }
            is UpdateCheckState.UpdateAvailable -> {
                AlertDialog(
                    onDismissRequest = viewModel::dismissUpdateDialog,
                    title = { Text("Update Available! 🎉") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "A new version of Attendly is ready for download.",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Current: v${state.currentVersion} → Latest: v${state.latestVersion}",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            if (state.releaseNotes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Release Notes:",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = state.releaseNotes.take(300) + if (state.releaseNotes.length > 300) "..." else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.downloadAndInstallUpdate(
                                    context = context,
                                    downloadUrl = state.downloadUrl,
                                    version = state.latestVersion
                                )
                            }
                        ) {
                            Text("Download & Install")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = viewModel::dismissUpdateDialog) {
                            Text("Later")
                        }
                    }
                )
            }
            is UpdateCheckState.Downloading -> {
                AlertDialog(
                    onDismissRequest = { /* Keep active during download */ },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text("Downloading Update")
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Downloading Attendly v${state.version}...",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            if (state.progress >= 0f) {
                                LinearProgressIndicator(
                                    progress = { state.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val downloadedMb = String.format(
                                        java.util.Locale.US,
                                        "%.1f",
                                        state.bytesDownloaded / (1024f * 1024f)
                                    )
                                    val totalMb = String.format(
                                        java.util.Locale.US,
                                        "%.1f",
                                        state.totalBytes / (1024f * 1024f)
                                    )
                                    Text(
                                        text = "$downloadedMb MB / $totalMb MB",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${(state.progress * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                )
                                val downloadedMb = String.format(
                                    java.util.Locale.US,
                                    "%.1f",
                                    state.bytesDownloaded / (1024f * 1024f)
                                )
                                Text(
                                    text = "$downloadedMb MB downloaded",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = viewModel::cancelDownload) {
                            Text("Cancel")
                        }
                    }
                )
            }
            is UpdateCheckState.DownloadComplete -> {
                AlertDialog(
                    onDismissRequest = viewModel::dismissUpdateDialog,
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text("Ready to Install! 🚀")
                        }
                    },
                    text = {
                        Text(
                            text = "Attendly v${state.version} is downloaded and ready to install. Tap 'Install Now' to proceed."
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                ApkInstaller.installApk(context, state.apkFile)
                            }
                        ) {
                            Text("Install Now")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = viewModel::dismissUpdateDialog) {
                            Text("Later")
                        }
                    }
                )
            }
            is UpdateCheckState.UpToDate -> {
                AlertDialog(
                    onDismissRequest = viewModel::dismissUpdateDialog,
                    title = { Text("You're Up to Date! ✨") },
                    text = {
                        Text("Attendly v${state.version} is currently the latest version. No updates are required.")
                    },
                    confirmButton = {
                        Button(onClick = viewModel::dismissUpdateDialog) {
                            Text("Great")
                        }
                    }
                )
            }
            is UpdateCheckState.Error -> {
                AlertDialog(
                    onDismissRequest = viewModel::dismissUpdateDialog,
                    title = { Text("Update Check") },
                    text = {
                        Text("Could not reach GitHub Releases (${state.message}). You can manually check GitHub in your browser.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.dismissUpdateDialog()
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.releasePageUrl))
                                context.startActivity(intent)
                            }
                        ) {
                            Text("Open Releases")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = viewModel::dismissUpdateDialog) {
                            Text("Close")
                        }
                    }
                )
            }
            UpdateCheckState.Idle -> { /* do nothing */ }
        }

        // Privacy Policy Dialog
        if (showPrivacyDialog) {
            AlertDialog(
                onDismissRequest = { showPrivacyDialog = false },
                title = { Text("Privacy Policy") },
                text = {
                    Text(
                        "Attendly does not collect, transmit, or share any personal academic information. All subjects, attendance records, timetables, and preferences are stored exclusively on your local device."
                    )
                },
                confirmButton = {
                    Button(onClick = { showPrivacyDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // Start Next Semester Dialog
        val isStartSemesterDialogOpen by viewModel.isStartSemesterDialogOpen.collectAsState()
        if (isStartSemesterDialogOpen) {
            com.hrshd1eux.attendly.ui.subjects.StartNextSemesterDialog(
                activeSubjectCount = 0,
                onDismiss = viewModel::closeStartSemesterDialog,
                onConfirm = viewModel::startNextSemester
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun ThemeOption(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onClick() }
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp)
        )
    }
}
