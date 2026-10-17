package com.hrshd1eux.attendly.ui.timetable

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val AI_TIMETABLE_PROMPT_TEMPLATE = """
I have uploaded an image or document containing my college timetable.

Convert the timetable into plain text that can be imported into an Android attendance-tracking application.

Carefully inspect the timetable and extract every identifiable class session.

Requirements:

1. Recognize the days of the week.
2. Extract the start and end times of every class.
3. Extract subject names as accurately as possible.
4. Include room numbers or locations when clearly visible.
5. Preserve multiple-hour sessions and laboratory periods as continuous time ranges.
6. Do not split a two-hour or three-hour laboratory into separate sessions unless the timetable explicitly shows separate sessions.
7. Preserve the original order of classes.
8. If a cell is unclear, mark it as UNCERTAIN instead of inventing information.
9. If a subject name is unreadable, use UNKNOWN SUBJECT and explain which entry needs correction.
10. Do not invent missing times, rooms, subjects, or days.
11. If the timetable has multiple sections, ask which section applies if it cannot be determined.
12. If there are conflicting or overlapping entries, preserve them and flag the conflict.

Output the result using exactly this general structure:

TIMETABLE
FORMAT_VERSION: 1

DAY: MONDAY
09:00-10:00 | Mathematics | Room 101
10:00-11:00 | Physics | Room 204
11:00-13:00 | Chemistry Laboratory | Lab 2

DAY: TUESDAY
09:00-10:00 | English | Room 105
10:00-12:00 | Computer Laboratory | Lab 1

Continue for every day present in the original timetable.

Use 24-hour time in HH:mm format.

Use these day names:
MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY.

Each class entry must use this structure:

START_TIME-END_TIME | SUBJECT_NAME | LOCATION

If the location is unavailable, leave the location field empty:

09:00-10:00 | Mathematics |

Do not include explanations, markdown tables, code fences, or introductory text in the final output.

If any entry is uncertain, add an uncertainty comment immediately after the affected entry using:

COMMENT: UNCERTAIN - explain what needs verification

Only return the timetable and necessary uncertainty comments.
""".trimIndent()

@Composable
fun AiPromptDialog(
    onDismiss: () -> Unit,
    onNavigateToImport: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "AI Timetable Assistant",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = "How to convert your timetable image with AI:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))

                val steps = listOf(
                    "1. Copy the prompt below.",
                    "2. Open ChatGPT, Gemini, or Claude.",
                    "3. Upload a photo or PDF of your timetable.",
                    "4. Paste the prompt and send.",
                    "5. Copy the AI's response.",
                    "6. Return here and tap 'Paste & Import'."
                )

                steps.forEach { step ->
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Copy Prompt Button
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Attendly AI Prompt", AI_TIMETABLE_PROMPT_TEMPLATE)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Prompt copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Copy AI Prompt")
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Prompt Preview:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = AI_TIMETABLE_PROMPT_TEMPLATE,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onNavigateToImport()
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Paste & Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
