package com.hrshd1eux.attendly.ui.timetable

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hrshd1eux.attendly.core.util.TimeUtils
import com.hrshd1eux.attendly.domain.parser.TimetableTextParser
import com.hrshd1eux.attendly.domain.parser.TimetableGroupDetector
import com.hrshd1eux.attendly.ui.components.AttendlyTopBar
import com.hrshd1eux.attendly.ui.theme.LocalStatusColors
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun ImportTimetableScreen(
    onNavigateBack: () -> Unit,
    viewModel: TimetableViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val statusColors = LocalStatusColors.current

    var inputText by remember { mutableStateOf("") }
    var replaceExisting by remember { mutableStateOf(true) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val content = reader.readText()
                    inputText = content
                    viewModel.parseTimetableText(content)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Class Group Selection Dialog (Automatically pops up when timetable contains multiple groups e.g. G1, G2, G3)
    if (uiState.isGroupSelectionDialogOpen && uiState.detectedGroups.size > 1) {
        ClassGroupSelectionDialog(
            detectedGroups = uiState.detectedGroups,
            selectedGroup = uiState.selectedGroup,
            cleanSubjectNames = uiState.cleanSubjectNames,
            rawEntryCount = uiState.rawParseResult?.entries?.size ?: 0,
            onSelectGroup = { viewModel.selectGroupFilter(it) },
            onToggleClean = { viewModel.toggleCleanSubjectNames(it) },
            onDismiss = { viewModel.closeGroupSelectionDialog() }
        )
    }

    Scaffold(
        topBar = {
            AttendlyTopBar(
                title = "Import Timetable",
                canNavigateBack = true,
                onNavigateBack = {
                    viewModel.clearParseResult()
                    onNavigateBack()
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
            item {
                Text(
                    text = "Paste Timetable Text or Import .txt File",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Paste your timetable text below. If your timetable contains groups (like G1, G2, G3), Attendly will let you select your group and automatically ignore the rest.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            filePickerLauncher.launch(arrayOf("text/plain"))
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick .txt File")
                    }

                    Button(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.parseTimetableText(inputText)
                            } else {
                                Toast.makeText(context, "Please paste or import timetable text first", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Parse & Review")
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = {
                        inputText = it
                    },
                    label = { Text("Timetable Text") },
                    placeholder = {
                        Text("DAY: MONDAY\n09:00-10:00 | Mathematics | Room 101\n14:00-16:00 | DSA LAB G-1 | LAB-212")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(16.dp)
                )
            }

            // Parse Results Review Section
            uiState.parseResult?.let { result ->
                item {
                    Text(
                        text = "Review Recognized Timetable",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Group Filtering Card (Visible if multiple groups were detected)
                if (uiState.detectedGroups.size > 1) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Groups,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Active Group: ${uiState.selectedGroup?.let { TimetableGroupDetector.getGroupDisplayName(it) } ?: "All Groups"}",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    TextButton(
                                        onClick = { viewModel.openGroupSelectionDialog() }
                                    ) {
                                        Text("Change")
                                    }
                                }

                                Text(
                                    text = "Select which group you belong to. Sessions from other groups will be ignored automatically.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = uiState.selectedGroup == null,
                                        onClick = { viewModel.selectGroupFilter(null) },
                                        label = { Text("All Groups") }
                                    )
                                    uiState.detectedGroups.forEach { groupKey ->
                                        FilterChip(
                                            selected = uiState.selectedGroup == groupKey,
                                            onClick = { viewModel.selectGroupFilter(groupKey) },
                                            label = { Text(groupKey) }
                                        )
                                    }
                                }

                                if (uiState.ignoredSessionsCount > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Ignoring ${uiState.ignoredSessionsCount} classes from other groups",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                // Summary card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (result.entries.isNotEmpty()) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (result.entries.isNotEmpty()) statusColors.present else statusColors.absent
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "${result.entries.size} classes to import",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    if (uiState.ignoredSessionsCount > 0) {
                                        Text(
                                            text = "${uiState.ignoredSessionsCount} classes from other groups ignored",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (result.warnings.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Warnings (${result.warnings.size}):",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = statusColors.warning
                                    )
                                )
                                result.warnings.forEach { warning ->
                                    Text(
                                        text = "• $warning",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = statusColors.warning
                                    )
                                }
                            }

                            if (result.unrecognizedLines.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Unrecognized Lines (${result.unrecognizedLines.size}):",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = statusColors.absent
                                    )
                                )
                                result.unrecognizedLines.forEach { line ->
                                    Text(
                                        text = "• $line",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = statusColors.absent
                                    )
                                }
                            }
                        }
                    }
                }

                // Import option: Replace vs Merge
                item {
                    Text(
                        text = "Import Option:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { replaceExisting = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = replaceExisting,
                            onClick = { replaceExisting = true }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Replace existing timetable",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { replaceExisting = false },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !replaceExisting,
                            onClick = { replaceExisting = false }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Merge with existing timetable",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Clean subject names toggle
                if (uiState.detectedGroups.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleCleanSubjectNames(!uiState.cleanSubjectNames) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = uiState.cleanSubjectNames,
                                onCheckedChange = { viewModel.toggleCleanSubjectNames(it) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Clean group suffixes from subject names",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                )
                                Text(
                                    text = "e.g. \"DSA LAB G-1\" becomes \"DSA LAB\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Recognized Sessions List Preview
                items(result.entries) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "${entry.dayOfWeek.displayName}: ${entry.subjectName}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (entry.group != null) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = entry.group,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                val teacherDisplay = entry.teacher.ifEmpty {
                                    TimetableTextParser.extractTeacher(entry.notes) ?: ""
                                }
                                val detailParts = mutableListOf<String>()
                                detailParts.add("${TimeUtils.minutesToTimeString(entry.startMinute)} - ${TimeUtils.minutesToTimeString(entry.endMinute)}")
                                if (entry.location.isNotEmpty()) {
                                    detailParts.add(entry.location)
                                }
                                if (teacherDisplay.isNotEmpty()) {
                                    val formattedTeacher = if (teacherDisplay.startsWith("Teacher:", ignoreCase = true) ||
                                        teacherDisplay.startsWith("Instructor:", ignoreCase = true) ||
                                        teacherDisplay.startsWith("Guest:", ignoreCase = true)) {
                                        teacherDisplay
                                    } else {
                                        "Teacher: $teacherDisplay"
                                    }
                                    detailParts.add(formattedTeacher)
                                }
                                Text(
                                    text = detailParts.joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (entry.hasConflict && entry.conflictReason != null) {
                                    Text(
                                        text = "⚠ ${entry.conflictReason}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = statusColors.warning
                                    )
                                }
                            }
                        }
                    }
                }

                // Final Save Button
                item {
                    Button(
                        onClick = {
                            viewModel.applyImport(replaceExisting) {
                                Toast.makeText(context, "Timetable imported successfully!", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            }
                        },
                        enabled = result.entries.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "Confirm & Save Timetable (${result.entries.size} classes)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun ClassGroupSelectionDialog(
    detectedGroups: List<String>,
    selectedGroup: String?,
    cleanSubjectNames: Boolean,
    rawEntryCount: Int,
    onSelectGroup: (String?) -> Unit,
    onToggleClean: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var tempSelectedGroup by remember(selectedGroup) {
        mutableStateOf(selectedGroup ?: detectedGroups.firstOrNull())
    }
    var tempClean by remember(cleanSubjectNames) {
        mutableStateOf(cleanSubjectNames)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Which group are you in?",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "This timetable contains multiple groups (${detectedGroups.joinToString(", ")}). Select your group so Attendly can ignore the other groups:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    detectedGroups.forEach { groupKey ->
                        val isSelected = tempSelectedGroup == groupKey
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { tempSelectedGroup = groupKey },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { tempSelectedGroup = groupKey }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = TimetableGroupDetector.getGroupDisplayName(groupKey),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                    Text(
                                        text = "Imports $groupKey classes + common lectures",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // All groups option
                    val isAllSelected = tempSelectedGroup == null || tempSelectedGroup.equals("ALL", ignoreCase = true)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { tempSelectedGroup = null },
                        color = if (isAllSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isAllSelected,
                                onClick = { tempSelectedGroup = null }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "All Groups (No Filter)",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                                Text(
                                    text = "Keep all $rawEntryCount sessions without filtering",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { tempClean = !tempClean }
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = tempClean,
                        onCheckedChange = { tempClean = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Clean group suffixes (e.g. \"DSA LAB G-1\" → \"DSA LAB\")",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSelectGroup(tempSelectedGroup)
                    onToggleClean(tempClean)
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Confirm Group")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}
