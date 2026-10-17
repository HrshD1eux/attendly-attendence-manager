package com.hrshd1eux.attendly.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class LeaveOption {
    CANCELLED, // Marks today's classes as cancelled (doesn't hurt %)
    ABSENT,    // Marks all today's classes as absent
    MUTE_ONLY  // Just pause prompts, leave attendance blank for now
}

@Composable
fun LeaveConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirmLeave: (LeaveOption) -> Unit
) {
    var selectedOption by remember { mutableStateOf(LeaveOption.CANCELLED) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.BeachAccess,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Taking Leave Today?",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select how you'd like Attendly to treat today's scheduled classes so you aren't disturbed by notifications:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                LeaveOptionRow(
                    title = "College Holiday / Off",
                    description = "Marks classes as Cancelled. Does not drop your attendance %.",
                    selected = selectedOption == LeaveOption.CANCELLED,
                    onClick = { selectedOption = LeaveOption.CANCELLED }
                )

                Spacer(modifier = Modifier.height(8.dp))

                LeaveOptionRow(
                    title = "Personal Absence",
                    description = "Marks all today's classes as Absent.",
                    selected = selectedOption == LeaveOption.ABSENT,
                    onClick = { selectedOption = LeaveOption.ABSENT }
                )

                Spacer(modifier = Modifier.height(8.dp))

                LeaveOptionRow(
                    title = "Just Mute Prompts",
                    description = "Pause notifications for today without modifying past records.",
                    selected = selectedOption == LeaveOption.MUTE_ONLY,
                    onClick = { selectedOption = LeaveOption.MUTE_ONLY }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmLeave(selectedOption) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Confirm Leave")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun LeaveOptionRow(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
