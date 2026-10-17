package com.hrshd1eux.attendly.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.hrshd1eux.attendly.core.model.DayOfWeek
import com.hrshd1eux.attendly.data.local.dao.HolidayDao
import com.hrshd1eux.attendly.data.local.dao.SubjectDao
import com.hrshd1eux.attendly.data.local.dao.TimetableSessionDao
import com.hrshd1eux.attendly.data.preferences.PreferencesManager
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClassAlarmScheduler @Inject constructor(
    private val timetableDao: TimetableSessionDao,
    private val subjectDao: SubjectDao,
    private val holidayDao: HolidayDao,
    private val preferencesManager: PreferencesManager
) {

    suspend fun scheduleTodayClassAlarms(context: Context) {
        val prefs = preferencesManager.userPreferencesFlow.first()
        if (prefs.isOnLeaveToday || !prefs.notificationsEnabled) return

        val today = LocalDate.now()
        // Do not schedule class alarms on university holidays
        if (holidayDao.getHolidayForDateSync(today.toString()) != null) return

        AttendanceNotificationReceiver.createChannel(context)

        val dayOfWeek = DayOfWeek.fromJavaDayOfWeek(today.dayOfWeek)
        val sessions = timetableDao.getSessionsForDaySync(dayOfWeek.isoValue)
        val subjects = subjectDao.getActiveSubjects().first().associateBy { it.id }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val now = LocalDateTime.now()

        for (session in sessions) {
            val subject = subjects[session.subjectId] ?: continue

            // Class trigger time: at end minute of the class
            val classEndTime = today.atTime(session.endMinute / 60, session.endMinute % 60)

            // Only schedule if class ends in the future today
            if (classEndTime.isAfter(now)) {
                val triggerMillis = classEndTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                val intent = Intent(context, AttendanceNotificationReceiver::class.java).apply {
                    action = AttendanceNotificationReceiver.ACTION_CHECK_CLASS_PROMPT
                    putExtra(AttendanceNotificationReceiver.EXTRA_SUBJECT_ID, subject.id)
                    putExtra(AttendanceNotificationReceiver.EXTRA_SUBJECT_NAME, subject.name)
                    putExtra(AttendanceNotificationReceiver.EXTRA_TIMETABLE_SESSION_ID, session.id)
                    putExtra(AttendanceNotificationReceiver.EXTRA_START_MINUTE, session.startMinute)
                    putExtra(AttendanceNotificationReceiver.EXTRA_END_MINUTE, session.endMinute)
                    putExtra(AttendanceNotificationReceiver.EXTRA_LOCATION, session.location)
                }

                val requestCode = (session.id * 100).toInt()
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                    }
                } catch (e: Exception) {
                    // Fallback to standard inexact alarm if exact alarm permission is restricted
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            }
        }
    }
}
