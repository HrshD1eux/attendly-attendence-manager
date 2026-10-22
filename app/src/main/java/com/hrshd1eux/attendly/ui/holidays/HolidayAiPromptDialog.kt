package com.hrshd1eux.attendly.ui.holidays

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
import androidx.compose.material.icons.filled.AutoAwesome
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

val AI_HOLIDAY_PROMPT_TEMPLATE = """
I have uploaded an image or document containing my university's or college's academic calendar and list of holidays.

Extract every holiday, break, and non-working academic day and convert them into plain text that can be imported directly into an Android attendance tracker.

Requirements:
1. Extract all official university holidays, public holidays, and scheduled vacations.
2. For single-day holidays, write the date in YYYY-MM-DD format followed by the holiday name.
3. For multi-day breaks or vacations (e.g. Diwali Break, Autumn Break, Winter Vacation, Semester Break), write the date range as 'YYYY-MM-DD to YYYY-MM-DD | Holiday Name'.
4. Infer the correct academic calendar year from the document (e.g. 2026).
5. Do not include regular weekly days off (like standard Sundays) unless explicitly designated as a special calendar holiday.
6. Do not include markdown tables, explanations, code blocks, or preamble text.

Output the result strictly using this structure:

HOLIDAYS
FORMAT_VERSION: 1

YYYY-MM-DD | Holiday Name
YYYY-MM-DD to YYYY-MM-DD | Holiday Name

Example:
HOLIDAYS
FORMAT_VERSION: 1

2026-01-26 | Republic Day
2026-03-25 | Holi
2026-08-15 | Independence Day
2026-10-02 | Gandhi Jayanti
2026-10-20 to 2026-10-24 | Diwali Break
2026-12-25 | Christmas
""".trimIndent()

@Composable
fun HolidayAiPromptDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "AI Holiday Extraction Prompt",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Most universities publish holiday calendars as PDFs or circulars. Follow these easy steps:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "1. Tap \"Copy Prompt\" below.",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = "2. Open ChatGPT, Gemini, or Claude.",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = "3. Upload your college calendar PDF or image and paste this prompt.",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = "4. Copy the AI's plain-text response and paste it into Attendly (or save as .txt file to import)!",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Prompt Preview:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = AI_HOLIDAY_PROMPT_TEMPLATE,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Attendly Holiday AI Prompt", AI_HOLIDAY_PROMPT_TEMPLATE)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Holiday AI Prompt copied to clipboard!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text("Copy Prompt")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
