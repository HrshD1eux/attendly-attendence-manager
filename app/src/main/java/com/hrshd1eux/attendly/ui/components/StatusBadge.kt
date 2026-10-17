package com.hrshd1eux.attendly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hrshd1eux.attendly.core.model.AttendanceStatus
import com.hrshd1eux.attendly.domain.model.AttendanceStanding
import com.hrshd1eux.attendly.ui.theme.LocalStatusColors

@Composable
fun AttendanceStatusBadge(
    status: AttendanceStatus,
    modifier: Modifier = Modifier
) {
    val statusColors = LocalStatusColors.current

    val (bg, fg, icon, label) = when (status) {
        AttendanceStatus.PRESENT -> Quad(
            statusColors.presentContainer,
            statusColors.onPresentContainer,
            Icons.Default.CheckCircle,
            "Present"
        )
        AttendanceStatus.ABSENT -> Quad(
            statusColors.absentContainer,
            statusColors.onAbsentContainer,
            Icons.Default.Close,
            "Absent"
        )
        AttendanceStatus.CANCELLED -> Quad(
            statusColors.cancelledContainer,
            statusColors.onCancelledContainer,
            Icons.Default.Cancel,
            "Cancelled"
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = fg,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = fg,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StandingBadge(
    standing: AttendanceStanding,
    modifier: Modifier = Modifier
) {
    val statusColors = LocalStatusColors.current

    val (bg, fg, icon) = when (standing) {
        AttendanceStanding.EXCELLENT, AttendanceStanding.ABOVE_TARGET -> Triple(
            statusColors.presentContainer,
            statusColors.onPresentContainer,
            Icons.Default.CheckCircle
        )
        AttendanceStanding.ON_TRACK -> Triple(
            statusColors.warningContainer,
            statusColors.onWarningContainer,
            Icons.Default.Info
        )
        AttendanceStanding.BELOW_TARGET -> Triple(
            statusColors.absentContainer,
            statusColors.onAbsentContainer,
            Icons.Default.Warning
        )
        AttendanceStanding.NO_DATA -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.Info
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = standing.label,
                tint = fg,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = standing.label,
                color = fg,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
