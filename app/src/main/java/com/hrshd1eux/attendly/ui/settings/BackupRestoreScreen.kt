package com.hrshd1eux.attendly.ui.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hrshd1eux.attendly.core.util.TimeUtils
import com.hrshd1eux.attendly.ui.components.AttendlyTopBar
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.LocalDate

@Composable
fun BackupRestoreScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val validationResult by viewModel.backupValidationResult.collectAsState()
    val isResetDialogOpen by viewModel.isResetConfirmDialogOpen.collectAsState()
    val prefs by viewModel.uiState.collectAsState()

    var replaceExisting by remember { mutableStateOf(true) }

    // SAF Document Creator for JSON Backup
    val createJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val json = viewModel.getExportBackupJson()
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        OutputStreamWriter(stream).use { it.write(json) }
                    }
                    Toast.makeText(context, "Backup successfully saved!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to export backup: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // SAF Document Creator for CSV
    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val csv = viewModel.getExportAttendanceCsv()
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        OutputStreamWriter(stream).use { it.write(csv) }
                    }
                    Toast.makeText(context, "Attendance CSV exported!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to export CSV: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // SAF Document Picker for Restoring JSON
    val openJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val reader = BufferedReader(InputStreamReader(stream))
                    val content = reader.readText()
                    viewModel.validateRestoreFile(content)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read backup file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            AttendlyTopBar(title = "Backup & Restore", canNavigateBack = true, onNavigateBack = onNavigateBack)
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
            item {
                Text(
                    text = "Backup & Export Options",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Save your data safely to local storage or restore from an earlier backup.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Storage Auto Backup Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Auto Backup to Device Storage",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = "Snapshots are automatically saved to Downloads/Attendly/attendly_auto_backup.json so your data persists even if you uninstall and reinstall the app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    viewModel.performStorageAutoBackup { success, path ->
                                        Toast.makeText(context, if (success) "Saved backup to $path" else "Backup failed", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Backup Now")
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.restoreStorageAutoBackup { success ->
                                        Toast.makeText(context, if (success) "Data successfully restored!" else "No auto-backup file found in storage", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Restore Auto Backup")
                            }
                        }
                    }
                }
            }

            // Create Backup JSON Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val dateStr = LocalDate.now().toString()
                            createJsonLauncher.launch("attendly_backup_$dateStr.json")
                        },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Create Local Backup (JSON)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Includes all subjects, timetable, and attendance records.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (prefs.lastBackupTimestamp > 0) {
                                Text(
                                    text = "Last backup: ${TimeUtils.formatDate(LocalDate.now(), "MMM d, yyyy")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Restore Backup Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            openJsonLauncher.launch(arrayOf("application/json", "text/*"))
                        },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Restore from Backup",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Restore your attendance data from an Attendly backup file.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Export CSV Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val dateStr = LocalDate.now().toString()
                            createCsvLauncher.launch("attendly_records_$dateStr.csv")
                        },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Export Attendance to CSV",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Open records in Excel, Google Sheets, or view detailed history.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Danger Zone: Reset
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.openResetConfirmDialog() },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reset All Attendance Data",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            )
                            Text(
                                text = "Permanently deletes all subjects, timetables, and attendance logs.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Restore Preview & Confirmation Dialog
        validationResult?.let { result ->
            AlertDialog(
                onDismissRequest = viewModel::cancelRestore,
                title = { Text(if (result.isValid) "Restore Backup Preview" else "Invalid Backup File") },
                text = {
                    if (result.isValid) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("The backup file contains:")
                            Text("• ${result.subjectCount} Subjects")
                            Text("• ${result.timetableSessionCount} Timetable Sessions")
                            Text("• ${result.classSessionCount} Class Attendance Records")

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Restoration Mode:", fontWeight = FontWeight.SemiBold)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { replaceExisting = true },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = replaceExisting, onClick = { replaceExisting = true })
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Replace current data")
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { replaceExisting = false },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = !replaceExisting, onClick = { replaceExisting = false })
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Merge with current data")
                            }
                        }
                    } else {
                        Text("Error: ${result.errorMessage ?: "Unknown error"}")
                    }
                },
                confirmButton = {
                    if (result.isValid) {
                        Button(
                            onClick = {
                                viewModel.confirmRestore(replaceExisting) {
                                    Toast.makeText(context, "Data restored successfully!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("Confirm Restore")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::cancelRestore) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Reset Confirmation Dialog
        if (isResetDialogOpen) {
            AlertDialog(
                onDismissRequest = viewModel::closeResetConfirmDialog,
                title = { Text("Reset All Data?") },
                text = {
                    Text("This will permanently wipe all your subjects, timetables, and attendance history. This action cannot be reversed.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.resetAllData {
                                Toast.makeText(context, "All data reset.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Reset Everything")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::closeResetConfirmDialog) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
