package com.hrshd1eux.attendly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hrshd1eux.attendly.domain.model.AttendanceStanding
import com.hrshd1eux.attendly.domain.model.AttendanceSummary
import com.hrshd1eux.attendly.ui.theme.LocalStatusColors

@Composable
fun DashboardStatRow(
    summary: AttendanceSummary,
    modifier: Modifier = Modifier
) {
    val statusColors = LocalStatusColors.current

    val (attendanceColor, statusLabel) = when (summary.standing) {
        AttendanceStanding.EXCELLENT, AttendanceStanding.ABOVE_TARGET -> Pair(
            statusColors.present,
            summary.standing.label
        )
        AttendanceStanding.ON_TRACK -> Pair(
            statusColors.warning,
            summary.standing.label
        )
        AttendanceStanding.BELOW_TARGET -> Pair(
            statusColors.absent,
            summary.standing.label
        )
        AttendanceStanding.NO_DATA -> Pair(
            MaterialTheme.colorScheme.onSurfaceVariant,
            "No Data"
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Attendance Card
        Card(
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 125.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Attendance",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Icon(
                        imageVector = if (summary.standing == AttendanceStanding.BELOW_TARGET) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = attendanceColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = summary.formattedPercentage,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = attendanceColor
                    )
                )

                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        // Safe Absences or Recovery Card
        Card(
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 125.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                val isBelow = summary.standing == AttendanceStanding.BELOW_TARGET
                val title = if (isBelow) "Recovery" else "Safe Bunks"
                val statNumber = if (summary.totalEligibleUnits == 0) {
                    "-"
                } else if (isBelow) {
                    summary.recoveryNeeded?.toString() ?: "N/A"
                } else {
                    summary.safeAbsences.toString()
                }

                val statSubtitle = if (summary.totalEligibleUnits == 0) {
                    "No classes yet"
                } else if (isBelow) {
                    if (summary.isImpossible100) "Target 100% unreachable" else "Classes needed"
                } else {
                    if (summary.safeAbsences == 1) "Class can be missed" else "Classes can be missed"
                }

                val accentColor = if (isBelow) statusColors.absent else statusColors.present

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Icon(
                        imageVector = if (isBelow) Icons.Default.EventAvailable else Icons.Default.Shield,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = statNumber,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor
                    )
                )

                Text(
                    text = statSubtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun OverallAttendanceCard(
    summary: AttendanceSummary,
    modifier: Modifier = Modifier
) {
    val statusColors = LocalStatusColors.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Overall Summary",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Target: ${summary.targetPercentage}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StandingBadge(standing = summary.standing)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { (summary.percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when (summary.standing) {
                    AttendanceStanding.BELOW_TARGET -> statusColors.absent
                    AttendanceStanding.ON_TRACK -> statusColors.warning
                    else -> statusColors.present
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${summary.presentUnits} attended  •  ${summary.absentUnits} missed",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = summary.statusMessage,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
