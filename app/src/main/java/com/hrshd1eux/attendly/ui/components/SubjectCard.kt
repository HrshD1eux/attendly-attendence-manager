package com.hrshd1eux.attendly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrshd1eux.attendly.domain.model.AttendanceStanding
import com.hrshd1eux.attendly.domain.model.SubjectAttendanceDetail
import com.hrshd1eux.attendly.ui.theme.LocalStatusColors
import com.hrshd1eux.attendly.ui.theme.SubjectColorPalette

@Composable
fun SubjectAttendanceCard(
    detail: SubjectAttendanceDetail,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onMarkPresent: (() -> Unit)? = null,
    onMarkAbsent: (() -> Unit)? = null,
    onMarkCancelled: (() -> Unit)? = null
) {
    val statusColors = LocalStatusColors.current
    val subject = detail.subject
    val summary = detail.summary

    val standingColor = when (summary.standing) {
        AttendanceStanding.BELOW_TARGET -> statusColors.absent
        AttendanceStanding.ON_TRACK -> statusColors.warning
        AttendanceStanding.NO_DATA -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> statusColors.present
    }

    val paletteColor = SubjectColorPalette[subject.colorKey] ?: standingColor

    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left Subject Palette Accent Bar
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(paletteColor)
            )

            // Content Column
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                // Top Row: Subject Info (Left) + Eye-Catching Percentage Dial (Right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        // Header tags: Avatar / Short Code, Location, Target
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(paletteColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (subject.shortCode.isNotEmpty()) {
                                    Text(
                                        text = subject.shortCode.take(3),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = paletteColor,
                                            fontSize = 10.sp
                                        )
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = null,
                                        tint = paletteColor,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }

                            if (subject.location.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.Place,
                                            contentDescription = null,
                                            modifier = Modifier.size(11.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = subject.location,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Target: ${subject.targetPercentage}%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Subject Name
                        Text(
                            text = subject.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Attendance Mini Stat Badges Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MiniStatBadge(
                                label = "Attended",
                                count = summary.presentUnits,
                                color = statusColors.present,
                                containerColor = statusColors.presentContainer.copy(alpha = 0.6f)
                            )
                            MiniStatBadge(
                                label = "Missed",
                                count = summary.absentUnits,
                                color = statusColors.absent,
                                containerColor = statusColors.absentContainer.copy(alpha = 0.6f)
                            )
                            MiniStatBadge(
                                label = "Total",
                                count = summary.totalEligibleUnits,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Right: 68dp Circular Progress Gauge
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(standingColor.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { (summary.percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxSize(),
                            color = standingColor,
                            trackColor = standingColor.copy(alpha = 0.18f),
                            strokeWidth = 6.5.dp,
                            strokeCap = StrokeCap.Round
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = summary.formattedPercentage,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = standingColor
                                ),
                                maxLines = 1
                            )
                            Text(
                                text = when (summary.standing) {
                                    AttendanceStanding.BELOW_TARGET -> "CRITICAL"
                                    AttendanceStanding.ON_TRACK -> "ON TRACK"
                                    AttendanceStanding.NO_DATA -> "NO LOGS"
                                    else -> "SAFE"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 8.sp,
                                    color = standingColor.copy(alpha = 0.9f)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mid: Target Comparison Linear Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = (summary.percentage / 100.0).toFloat().coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(standingColor)
                    )

                    // Target marker tick line
                    val targetFraction = (subject.targetPercentage / 100f).coerceIn(0.01f, 0.99f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = targetFraction)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .width(2.5.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Guidance Banner (Safe Bunks or Recovery classes)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when (summary.standing) {
                                AttendanceStanding.BELOW_TARGET -> statusColors.absentContainer.copy(alpha = 0.7f)
                                AttendanceStanding.ON_TRACK -> statusColors.warningContainer.copy(alpha = 0.7f)
                                else -> statusColors.presentContainer.copy(alpha = 0.7f)
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (summary.standing) {
                                AttendanceStanding.BELOW_TARGET -> Icons.Default.Warning
                                AttendanceStanding.ON_TRACK -> Icons.Default.Bolt
                                else -> Icons.Default.Shield
                            },
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = standingColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = summary.statusMessage,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = standingColor
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Bottom Section: Tactile Quick Mark Actions (P & A Buttons)
                if (onMarkPresent != null || onMarkAbsent != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        thickness = 0.75.dp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // [ P  Present (+1) ]
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = statusColors.present,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clickable { onMarkPresent?.invoke() }
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "P",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Present",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        }

                        // [ A  Absent (+1) ]
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = statusColors.absent,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clickable { onMarkAbsent?.invoke() }
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "A",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Absent",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        }

                        // [ C ] (Cancelled / Off)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(42.dp)
                                .clickable { onMarkCancelled?.invoke() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "C",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        // More Options (⋮) Dropdown
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More Options",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("View Subject Details") },
                                    onClick = {
                                        showMenu = false
                                        onClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Mark Cancelled (C)") },
                                    onClick = {
                                        showMenu = false
                                        onMarkCancelled?.invoke()
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

@Composable
private fun MiniStatBadge(
    label: String,
    count: Int,
    color: Color,
    containerColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(containerColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = color,
                    fontSize = 10.sp
                )
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = color.copy(alpha = 0.85f),
                    fontSize = 9.sp
                )
            )
        }
    }
}
