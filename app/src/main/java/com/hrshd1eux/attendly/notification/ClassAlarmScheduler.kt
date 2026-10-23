package com.hrshd1eux.attendly.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
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
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Always schedule tomorrow's midnight rescheduling alarm to maintain continuous daily cycles
        scheduleDailyMidnightAlarm(context, alarmManager)

        val prefs = preferencesManager.userPreferencesFlow.first()
        if (prefs.isOnLeaveToday || !prefs.notificationsEnabled) return

        val today = LocalDate.now()
        // Do not schedule class alarms on university holidays
        if (holidayDao.getHolidayForDateSync(today.toString()) != null) return

        AttendanceNotificationReceiver.createChannel(context)

        val dayOfWeek = DayOfWeek.fromJavaDayOfWeek(today.dayOfWeek)
        val sessions = timetableDao.getSessionsForDaySync(dayOfWeek.isoValue)
        val subjects = subjectDao.getActiveSubjects().first().associateBy { it.id }

        val now = LocalDateTime.now()
        val offsetMinutes = prefs.notificationOffsetMinutes.coerceIn(0, 30).toLong()

        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        for (session in sessions) {
            val subject = subjects[session.subjectId] ?: continue

            // Class trigger time: at end minute of the class minus configured offset
            val classEndTime = today.atTime(session.endMinute / 60, session.endMinute % 60)
            val triggerTime = classEndTime.minusMinutes(offsetMinutes)

            // Only schedule if trigger time is in the future today
            if (triggerTime.isAfter(now)) {
                val triggerMillis = triggerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                val intent = Intent(context, AttendanceNotificationReceiver::class.java).apply {
                    action = AttendanceNotificationReceiver.ACTION_CHECK_CLASS_PROMPT
                    data = Uri.parse("attendly://alarm/session/${session.id}?start=${session.startMinute}&end=${session.endMinute}")
                    putExtra(AttendanceNotificationReceiver.EXTRA_SUBJECT_ID, subject.id)
                    putExtra(AttendanceNotificationReceiver.EXTRA_SUBJECT_NAME, subject.name)
                    putExtra(AttendanceNotificationReceiver.EXTRA_TIMETABLE_SESSION_ID, session.id)
                    putExtra(AttendanceNotificationReceiver.EXTRA_START_MINUTE, session.startMinute)
                    putExtra(AttendanceNotificationReceiver.EXTRA_END_MINUTE, session.endMinute)
                    putExtra(AttendanceNotificationReceiver.EXTRA_LOCATION, session.location)
                }

                val requestCode = ((session.id.hashCode() * 31 + session.startMinute) and 0x7FFFFFFF)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                scheduleAlarmCompat(alarmManager, triggerMillis, pendingIntent, canScheduleExact)
            }
        }
    }

    private fun scheduleDailyMidnightAlarm(context: Context, alarmManager: AlarmManager) {
        val tomorrowMidnight = LocalDate.now().plusDays(1).atTime(0, 1)
        val triggerMillis = tomorrowMidnight.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val intent = Intent(context, BootReceiver::class.java).apply {
            action = BootReceiver.ACTION_SCHEDULE_DAILY_ALARMS
            data = Uri.parse("attendly://alarm/daily_reschedule")
        }
        val requestCode = 999999
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scheduleAlarmCompat(
        alarmManager: AlarmManager,
        triggerMillis: Long,
        pendingIntent: PendingIntent,
        canScheduleExact: Boolean
    ) {
        try {
            if (canScheduleExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            } catch (fallbackEx: Exception) {
                fallbackEx.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
