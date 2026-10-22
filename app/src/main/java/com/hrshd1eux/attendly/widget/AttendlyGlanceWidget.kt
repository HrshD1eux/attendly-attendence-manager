package com.hrshd1eux.attendly.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.hrshd1eux.attendly.MainActivity
import com.hrshd1eux.attendly.data.local.AttendlyDatabase
import com.hrshd1eux.attendly.domain.calculator.AttendanceCalculator
import com.hrshd1eux.attendly.domain.model.AttendanceStanding
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.hrshd1eux.attendly.data.preferences.PreferencesManager

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun database(): AttendlyDatabase
    fun preferencesManager(): PreferencesManager
}

class AttendlyGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val db = entryPoint.database()

        val subjects = db.subjectDao().getActiveSubjectsSync()
        val sessions = db.classSessionDao().getAllClassSessionsSync()

        var totalP = 0
        var totalA = 0
        var totalC = 0

        for (s in subjects) {
            totalP += s.initialPresent
            totalA += s.initialAbsent
        }

        val activeIds = subjects.map { it.id }.toSet()
        for (cs in sessions) {
            if (!activeIds.contains(cs.subjectId)) continue
            when (cs.status) {
                "PRESENT" -> totalP += cs.attendanceUnits
                "ABSENT" -> totalA += cs.attendanceUnits
                "CANCELLED" -> totalC += cs.attendanceUnits
            }
        }

        val target = if (subjects.isNotEmpty()) (subjects.map { it.targetPercentage }.average()).toInt().coerceIn(1, 100) else 75
        val summary = AttendanceCalculator.calculateSummary(totalP, totalA, totalC, target)

        provideContent {
            GlanceTheme {
                AttendlyWidgetContent(
                    percentage = summary.formattedPercentage,
                    target = summary.targetPercentage,
                    presentCount = summary.presentUnits,
                    absentCount = summary.absentUnits,
                    statusText = summary.statusMessage,
                    standing = summary.standing
                )
            }
        }
    }
}

@Composable
fun AttendlyWidgetContent(
    percentage: String,
    target: Int,
    presentCount: Int,
    absentCount: Int,
    statusText: String,
    standing: AttendanceStanding
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(20.dp)
            .background(GlanceTheme.colors.surface)
            .clickable(actionStartActivity<MainActivity>())
            .padding(14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize()
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Attendly",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.primary,
                        fontSize = 14.sp
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "Target $target%",
                    style = TextStyle(
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = percentage,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 28.sp
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "✓ $presentCount attended",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = "✕ $absentCount missed",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            Text(
                text = statusText,
                style = TextStyle(
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.primary,
                    fontSize = 12.sp
                ),
                maxLines = 1
            )
        }
    }
}

class AttendlyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AttendlyGlanceWidget()
}
