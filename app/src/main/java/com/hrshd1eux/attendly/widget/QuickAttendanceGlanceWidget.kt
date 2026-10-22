package com.hrshd1eux.attendly.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
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
import com.hrshd1eux.attendly.core.model.DayOfWeek
import com.hrshd1eux.attendly.core.model.TimeFormat
import com.hrshd1eux.attendly.core.util.TimeUtils
import com.hrshd1eux.attendly.data.local.entity.ClassSessionEntity
import com.hrshd1eux.attendly.domain.calculator.AttendanceCalculator
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime

val SubjectIdParamKey = ActionParameters.Key<Long>("subject_id")
val StatusParamKey = ActionParameters.Key<String>("status")
val TimetableSessionIdParamKey = ActionParameters.Key<Long>("timetable_session_id")
val StartMinuteParamKey = ActionParameters.Key<Int>("start_minute")
val EndMinuteParamKey = ActionParameters.Key<Int>("end_minute")
val OnLeaveParamKey = ActionParameters.Key<Boolean>("on_leave")

class QuickAttendanceGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val db = entryPoint.database()
        val prefsManager = entryPoint.preferencesManager()

        val prefs = prefsManager.userPreferencesFlow.first()
        val today = LocalDate.now()
        val todayStr = today.toString()
        val currentMinute = LocalTime.now().let { it.hour * 60 + it.minute }

        // 1. If on leave today, display On-Leave widget state
        if (prefs.isOnLeaveToday) {
            provideContent {
                GlanceTheme {
                    OnLeaveWidgetContent()
                }
            }
            return
        }

        // 2. If today is a declared holiday, display Holiday widget state
        val todayHoliday = db.holidayDao().getHolidayForDateSync(todayStr)
        if (todayHoliday != null) {
            provideContent {
                GlanceTheme {
                    TodayHolidayWidgetContent(holidayName = todayHoliday.name)
                }
            }
            return
        }

        val dayOfWeek = DayOfWeek.fromJavaDayOfWeek(today.dayOfWeek)
        val todaySessions = db.timetableSessionDao().getSessionsForDaySync(dayOfWeek.isoValue)
        val subjects = db.subjectDao().getActiveSubjectsSync()
        val subjectMap = subjects.associateBy { it.id }

        val markedSessions = db.classSessionDao().getSessionsForDateSync(todayStr)
        val markedTtIds = markedSessions.mapNotNull { it.timetableSessionId }.toSet()
        val markedKeys = markedSessions.map { "${it.subjectId}_${it.startMinute}" }.toSet()

        // Unmarked sessions for today
        val unmarkedSessions = todaySessions
            .filter { subjectMap.containsKey(it.subjectId) }
            .filter { !markedTtIds.contains(it.id) && !markedKeys.contains("${it.subjectId}_${it.startMinute}") }

        // Find candidate: Ongoing first, then ended, then upcoming
        val candidate = unmarkedSessions.firstOrNull { currentMinute in it.startMinute..it.endMinute }
            ?: unmarkedSessions.firstOrNull { currentMinute >= it.endMinute }
            ?: unmarkedSessions.firstOrNull()

        if (candidate != null) {
            val subject = subjectMap[candidate.subjectId]
            val subjectName = subject?.shortCode?.ifEmpty { subject?.name } ?: subject?.name ?: "Class"
            val timeRange = "${TimeUtils.minutesToTimeString(candidate.startMinute, prefs.timeFormat)} - ${TimeUtils.minutesToTimeString(candidate.endMinute, prefs.timeFormat)}"
            val isLiveNow = currentMinute in candidate.startMinute..candidate.endMinute

            // Calculate subject attendance percentage
            val subjectSessions = db.classSessionDao().getSessionsForSubjectSync(candidate.subjectId)
            var p = subject?.initialPresent ?: 0
            var a = subject?.initialAbsent ?: 0
            var c = 0
            for (s in subjectSessions) {
                when (s.status) {
                    "PRESENT" -> p += s.attendanceUnits
                    "ABSENT" -> a += s.attendanceUnits
                    "CANCELLED" -> c += s.attendanceUnits
                }
            }
            val summary = AttendanceCalculator.calculateSummary(p, a, c, subject?.targetPercentage ?: 75)

            provideContent {
                GlanceTheme {
                    ActivePromptWidgetContent(
                        subjectName = subjectName,
                        timeRange = timeRange,
                        location = candidate.location,
                        isLiveNow = isLiveNow,
                        subjectPercentage = summary.formattedPercentage,
                        subjectId = candidate.subjectId,
                        timetableSessionId = candidate.id,
                        startMinute = candidate.startMinute,
                        endMinute = candidate.endMinute
                    )
                }
            }
        } else {
            // No unmarked class today: show overall summary or all-caught-up
            val allSessions = db.classSessionDao().getAllClassSessionsSync()
            var totalP = 0
            var totalA = 0
            var totalC = 0
            for (s in subjects) {
                totalP += s.initialPresent
                totalA += s.initialAbsent
            }
            val activeIds = subjects.map { it.id }.toSet()
            for (cs in allSessions) {
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
                    AllDoneWidgetContent(
                        percentage = summary.formattedPercentage,
                        statusMessage = summary.statusMessage,
                        hasTimetableToday = todaySessions.isNotEmpty()
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivePromptWidgetContent(
    subjectName: String,
    timeRange: String,
    location: String,
    isLiveNow: Boolean,
    subjectPercentage: String,
    subjectId: Long,
    timetableSessionId: Long,
    startMinute: Int,
    endMinute: Int
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(20.dp)
            .background(GlanceTheme.colors.surface)
            .clickable(actionStartActivity<MainActivity>())
            .padding(14.dp)
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Header Row: Subject Name + Tag + Percentage
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subjectName,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 15.sp
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight()
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = if (isLiveNow) "• LIVE" else "• PROMPT",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.primary,
                        fontSize = 11.sp
                    )
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = subjectPercentage,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.primary,
                        fontSize = 15.sp
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(3.dp))

            // Time & Location
            val details = if (location.isNotEmpty()) "$timeRange • $location" else timeRange
            Text(
                text = details,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp
                ),
                maxLines = 1
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            // Main Prompt
            Text(
                text = "Did you attend this class?",
                style = TextStyle(
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 12.sp
                )
            )

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Row 1: [P  Present] and [A  Absent]
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Button(
                    text = "P  Present",
                    onClick = actionRunCallback<RecordAttendanceActionCallback>(
                        actionParametersOf(
                            SubjectIdParamKey to subjectId,
                            StatusParamKey to "PRESENT",
                            TimetableSessionIdParamKey to timetableSessionId,
                            StartMinuteParamKey to startMinute,
                            EndMinuteParamKey to endMinute
                        )
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )

                Spacer(modifier = GlanceModifier.width(8.dp))

                Button(
                    text = "A  Absent",
                    onClick = actionRunCallback<RecordAttendanceActionCallback>(
                        actionParametersOf(
                            SubjectIdParamKey to subjectId,
                            StatusParamKey to "ABSENT",
                            TimetableSessionIdParamKey to timetableSessionId,
                            StartMinuteParamKey to startMinute,
                            EndMinuteParamKey to endMinute
                        )
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Row 2: [✕  Cancelled] and [🏖️ On Leave]
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Button(
                    text = "✕  Cancelled",
                    onClick = actionRunCallback<RecordAttendanceActionCallback>(
                        actionParametersOf(
                            SubjectIdParamKey to subjectId,
                            StatusParamKey to "CANCELLED",
                            TimetableSessionIdParamKey to timetableSessionId,
                            StartMinuteParamKey to startMinute,
                            EndMinuteParamKey to endMinute
                        )
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )

                Spacer(modifier = GlanceModifier.width(8.dp))

                Button(
                    text = "🏖️ On Leave",
                    onClick = actionRunCallback<MarkLeaveActionCallback>(
                        actionParametersOf(OnLeaveParamKey to true)
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
            }
        }
    }
}

@Composable
private fun AllDoneWidgetContent(
    percentage: String,
    statusMessage: String,
    hasTimetableToday: Boolean
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(20.dp)
            .background(GlanceTheme.colors.surface)
            .clickable(actionStartActivity<MainActivity>())
            .padding(14.dp)
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
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
                    text = percentage,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 18.sp
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            Text(
                text = if (hasTimetableToday) "All today's classes marked! 🎉" else "No classes scheduled today",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 13.sp
                )
            )

            Spacer(modifier = GlanceModifier.height(2.dp))

            Text(
                text = statusMessage,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp
                ),
                maxLines = 1
            )

            Spacer(modifier = GlanceModifier.height(8.dp))

            Button(
                text = "🏖️ On Leave Today",
                onClick = actionRunCallback<MarkLeaveActionCallback>(
                    actionParametersOf(OnLeaveParamKey to true)
                ),
                modifier = GlanceModifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun OnLeaveWidgetContent() {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(20.dp)
            .background(GlanceTheme.colors.surface)
            .clickable(actionStartActivity<MainActivity>())
            .padding(14.dp)
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
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
                    text = "🏖️ On Leave Today",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.primary,
                        fontSize = 12.sp
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            Text(
                text = "Reminders and class prompt notifications are paused for today.",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp
                ),
                maxLines = 2
            )

            Spacer(modifier = GlanceModifier.height(10.dp))

            Button(
                text = "Resume Tracking",
                onClick = actionRunCallback<MarkLeaveActionCallback>(
                    actionParametersOf(OnLeaveParamKey to false)
                ),
                modifier = GlanceModifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TodayHolidayWidgetContent(holidayName: String) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(20.dp)
            .background(GlanceTheme.colors.surface)
            .clickable(actionStartActivity<MainActivity>())
            .padding(14.dp)
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
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
                    text = "🎉 Holiday Today",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.primary,
                        fontSize = 12.sp
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            Text(
                text = holidayName.ifBlank { "Holiday" },
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 15.sp
                ),
                maxLines = 1
            )

            Spacer(modifier = GlanceModifier.height(2.dp))

            Text(
                text = "Classes are suspended for today. Enjoy your holiday!",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp
                ),
                maxLines = 2
            )

            Spacer(modifier = GlanceModifier.height(8.dp))

            Text(
                text = "Tap to open Attendly",
                style = TextStyle(
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.primary,
                    fontSize = 11.sp
                )
            )
        }
    }
}

class RecordAttendanceActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val subjectId = parameters[SubjectIdParamKey] ?: return
        val status = parameters[StatusParamKey] ?: "PRESENT"
        val timetableSessionId = parameters[TimetableSessionIdParamKey]
        val startMinute = parameters[StartMinuteParamKey] ?: 0
        val endMinute = parameters[EndMinuteParamKey] ?: 0

        val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val db = entryPoint.database()

        val dateStr = LocalDate.now().toString()
        val existing = if (timetableSessionId != null && timetableSessionId != 0L) {
            db.classSessionDao().findSessionByTimetableAndDate(timetableSessionId, dateStr)
        } else {
            db.classSessionDao().findSessionBySubjectDateAndTime(subjectId, dateStr, startMinute)
        }

        if (existing != null) {
            db.classSessionDao().updateSession(
                existing.copy(status = status, updatedAt = System.currentTimeMillis())
            )
        } else {
            db.classSessionDao().insertSession(
                ClassSessionEntity(
                    subjectId = subjectId,
                    timetableSessionId = if (timetableSessionId != null && timetableSessionId != 0L) timetableSessionId else null,
                    date = dateStr,
                    startMinute = startMinute,
                    endMinute = endMinute,
                    status = status,
                    attendanceUnits = 1
                )
            )
        }

        WidgetUpdater.notifyDataChanged(context)
    }
}

class MarkLeaveActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val prefs = entryPoint.preferencesManager()
        val onLeave = parameters[OnLeaveParamKey] ?: true
        prefs.setOnLeaveToday(onLeave, if (onLeave) "CANCELLED" else "")
        WidgetUpdater.notifyDataChanged(context)
    }
}

class QuickAttendanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickAttendanceGlanceWidget()
}
