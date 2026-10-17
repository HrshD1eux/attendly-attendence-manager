package com.hrshd1eux.attendly.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrshd1eux.attendly.core.model.AttendanceStatus
import com.hrshd1eux.attendly.core.model.TimeFormat
import com.hrshd1eux.attendly.core.util.TimeUtils
import com.hrshd1eux.attendly.domain.model.TodayClassItem
import com.hrshd1eux.attendly.ui.theme.LocalStatusColors
import com.hrshd1eux.attendly.ui.theme.SubjectColorPalette
import java.time.LocalDate

@Composable
fun TodayScheduleCard(
    items: List<TodayClassItem>,
    currentDate: LocalDate,
    timeFormat: TimeFormat,
    onMarkStatus: (TodayClassItem, AttendanceStatus) -> Unit,
    onClearStatus: (TodayClassItem) -> Unit,
    onNavigateToTimetable: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenLeaveDialog: (() -> Unit)? = null
) {
    val statusColors = LocalStatusColors.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header: "Today's Schedule" and date + Leave button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Today's Schedule",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = TimeUtils.formatDate(currentDate, "EEEE, MMM d"),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                if (onOpenLeaveDialog != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { onOpenLeaveDialog() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "🏖️ On Leave?",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No classes scheduled for today",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Manage Timetable",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.clickable { onNavigateToTimetable() }
                        )
                    }
                }
            } else {
                // Connected Timeline
                items.forEachIndexed { index, item ->
                    val isFirst = index == 0
                    val isLast = index == items.size - 1

                    TimelineClassRow(
                        item = item,
                        isFirst = isFirst,
                        isLast = isLast,
                        timeFormat = timeFormat,
                        onMarkStatus = { status -> onMarkStatus(item, status) },
                        onClearStatus = { onClearStatus(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineClassRow(
    item: TodayClassItem,
    isFirst: Boolean,
    isLast: Boolean,
    timeFormat: TimeFormat,
    onMarkStatus: (AttendanceStatus) -> Unit,
    onClearStatus: () -> Unit
) {
    val statusColors = LocalStatusColors.current
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    val nodeColor = when (item.status) {
        AttendanceStatus.PRESENT -> statusColors.present
        AttendanceStatus.ABSENT -> statusColors.absent
        AttendanceStatus.CANCELLED -> statusColors.cancelled
        null -> MaterialTheme.colorScheme.primary
    }

    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Vertical Timeline Track + Node
        Box(
            modifier = Modifier
                .width(28.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            Canvas(modifier = Modifier.fillMaxHeight().width(2.dp)) {
                val startY = if (isFirst) 14.dp.toPx() else 0f
                val endY = if (isLast) 14.dp.toPx() else size.height
                drawLine(
                    color = outlineColor,
                    start = Offset(size.width / 2, startY),
                    end = Offset(size.width / 2, endY),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Dot node
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(nodeColor)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Class details and Actions
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 4.dp else 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val currentMinute = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
                    val isLiveNow = currentMinute in item.startMinute..item.endMinute
                    val hasEnded = currentMinute > item.endMinute

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.subjectName,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.status == null && isLiveNow) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(statusColors.present.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "LIVE NOW",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 9.sp
                                    ),
                                    color = statusColors.present
                                )
                            }
                        } else if (item.status == null && hasEnded) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(statusColors.warning.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "UNMARKED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 9.sp
                                    ),
                                    color = statusColors.warning
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${TimeUtils.minutesToTimeString(item.startMinute, timeFormat)} - ${TimeUtils.minutesToTimeString(item.endMinute, timeFormat)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.location.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Place,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = item.location,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Status or Actions
                if (item.status != null) {
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showMenu = true }
                        ) {
                            AttendanceStatusBadge(status = item.status)
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (item.status != AttendanceStatus.PRESENT) {
                                DropdownMenuItem(
                                    text = { Text("Change to Present") },
                                    onClick = {
                                        showMenu = false
                                        onMarkStatus(AttendanceStatus.PRESENT)
                                    }
                                )
                            }
                            if (item.status != AttendanceStatus.ABSENT) {
                                DropdownMenuItem(
                                    text = { Text("Change to Absent") },
                                    onClick = {
                                        showMenu = false
                                        onMarkStatus(AttendanceStatus.ABSENT)
                                    }
                                )
                            }
                            if (item.status != AttendanceStatus.CANCELLED) {
                                DropdownMenuItem(
                                    text = { Text("Mark Cancelled") },
                                    onClick = {
                                        showMenu = false
                                        onMarkStatus(AttendanceStatus.CANCELLED)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Clear Status") },
                                onClick = {
                                    showMenu = false
                                    onClearStatus()
                                }
                            )
                        }
                    }
                } else {
                    // Action buttons (Present / Absent / Cancel)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Present Button (P)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = statusColors.present,
                            shadowElevation = 1.5.dp,
                            modifier = Modifier
                                .size(width = 36.dp, height = 34.dp)
                                .clickable { onMarkStatus(AttendanceStatus.PRESENT) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "P",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        fontSize = 15.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Absent Button (A)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = statusColors.absent,
                            shadowElevation = 1.5.dp,
                            modifier = Modifier
                                .size(width = 36.dp, height = 34.dp)
                                .clickable { onMarkStatus(AttendanceStatus.ABSENT) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "A",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        fontSize = 15.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Cancel / More option
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Cancel,
                                    contentDescription = "More Options",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Mark Class Cancelled") },
                                    onClick = {
                                        showMenu = false
                                        onMarkStatus(AttendanceStatus.CANCELLED)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
