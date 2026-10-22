package com.hrshd1eux.attendly.ui.subjects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hrshd1eux.attendly.domain.model.Subject
import com.hrshd1eux.attendly.ui.theme.SubjectColorPalette

@Composable
fun AddEditSubjectDialog(
    subjectToEdit: Subject?,
    defaultTarget: Int,
    onDismiss: () -> Unit,
    onSave: (name: String, shortCode: String, colorKey: String, target: Int, location: String, initialP: Int, initialA: Int) -> Unit
) {
    var name by remember { mutableStateOf(subjectToEdit?.name ?: "") }
    var shortCode by remember { mutableStateOf(subjectToEdit?.shortCode ?: "") }
    var location by remember { mutableStateOf(subjectToEdit?.location ?: "") }
    var selectedColorKey by remember { mutableStateOf(subjectToEdit?.colorKey ?: "EMERALD") }
    var targetPercentage by remember {
        mutableFloatStateOf((subjectToEdit?.targetPercentage ?: defaultTarget).toFloat())
    }
    var initialPresent by remember {
        mutableStateOf(subjectToEdit?.initialPresent?.takeIf { it > 0 }?.toString() ?: "")
    }
    var initialAbsent by remember {
        mutableStateOf(subjectToEdit?.initialAbsent?.takeIf { it > 0 }?.toString() ?: "")
    }

    var nameError by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (subjectToEdit == null) "Add Subject" else "Edit Subject",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // Subject Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = false
                    },
                    label = { Text("Subject Name *") },
                    placeholder = { Text("e.g. Data Structures") },
                    isError = nameError,
                    supportingText = if (nameError) { { Text("Subject name is required") } } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Short Code & Location
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = shortCode,
                        onValueChange = { shortCode = it.take(6).uppercase() },
                        label = { Text("Short Code") },
                        placeholder = { Text("DSA") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        placeholder = { Text("Room 204") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.5f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Target Percentage Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Target Attendance",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "${targetPercentage.toInt()}%",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Slider(
                    value = targetPercentage,
                    onValueChange = { targetPercentage = it },
                    valueRange = 50f..100f,
                    steps = 49,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Color Selection
                Text(
                    text = "Color Accent",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SubjectColorPalette.forEach { (key, color) ->
                        val isSelected = key == selectedColorKey
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColorKey = key }
                                .then(
                                    if (isSelected) Modifier.border(
                                        2.dp,
                                        MaterialTheme.colorScheme.onSurface,
                                        CircleShape
                                    ) else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = androidx.compose.ui.graphics.Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Mid-semester Initial Counts Section
                Text(
                    text = "Starting Totals (Mid-Semester)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "If you're starting Attendly mid-semester, enter previously attended and missed classes so percentages remain accurate.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = initialPresent,
                        onValueChange = { initialPresent = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Attended") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = initialAbsent,
                        onValueChange = { initialAbsent = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Missed") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                    } else {
                        onSave(
                            name,
                            shortCode,
                            selectedColorKey,
                            targetPercentage.toInt(),
                            location,
                            initialPresent.toIntOrNull() ?: 0,
                            initialAbsent.toIntOrNull() ?: 0
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (subjectToEdit == null) "Add" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
