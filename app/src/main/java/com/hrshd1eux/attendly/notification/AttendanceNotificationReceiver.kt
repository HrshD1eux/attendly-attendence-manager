package com.hrshd1eux.attendly.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.hrshd1eux.attendly.MainActivity
import com.hrshd1eux.attendly.R
import com.hrshd1eux.attendly.core.model.AttendanceStatus
import com.hrshd1eux.attendly.core.model.TimeFormat
import com.hrshd1eux.attendly.core.util.TimeUtils
import com.hrshd1eux.attendly.data.local.AttendlyDatabase
import com.hrshd1eux.attendly.data.local.entity.ClassSessionEntity
import com.hrshd1eux.attendly.data.preferences.PreferencesManager
import com.hrshd1eux.attendly.widget.WidgetUpdater
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationEntryPoint {
    fun database(): AttendlyDatabase
    fun preferencesManager(): PreferencesManager
}

class AttendanceNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "attendance_prompts"
        const val CHANNEL_NAME = "Class Reminders & Prompts"

        const val ACTION_CHECK_CLASS_PROMPT = "com.hrshd1eux.attendly.ACTION_CHECK_CLASS_PROMPT"
        const val ACTION_MARK_ATTENDANCE = "com.hrshd1eux.attendly.ACTION_MARK_ATTENDANCE"
        const val ACTION_MARK_LEAVE = "com.hrshd1eux.attendly.ACTION_MARK_LEAVE"

        const val EXTRA_SUBJECT_ID = "extra_subject_id"
        const val EXTRA_SUBJECT_NAME = "extra_subject_name"
        const val EXTRA_TIMETABLE_SESSION_ID = "extra_timetable_session_id"
        const val EXTRA_START_MINUTE = "extra_start_minute"
        const val EXTRA_END_MINUTE = "extra_end_minute"
        const val EXTRA_LOCATION = "extra_location"
        const val EXTRA_STATUS = "extra_status"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Prompts to mark attendance when scheduled classes end"
                    enableVibration(true)
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        createChannel(context)

        val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, NotificationEntryPoint::class.java)
        val db = entryPoint.database()
        val prefsManager = entryPoint.preferencesManager()

        when (intent.action) {
            ACTION_MARK_ATTENDANCE -> {
                val subjectId = intent.getLongExtra(EXTRA_SUBJECT_ID, 0L)
                val statusStr = intent.getStringExtra(EXTRA_STATUS) ?: AttendanceStatus.PRESENT.name
                val ttSessionId = intent.getLongExtra(EXTRA_TIMETABLE_SESSION_ID, 0L).takeIf { it != 0L }
                val startMinute = intent.getIntExtra(EXTRA_START_MINUTE, 0)
                val endMinute = intent.getIntExtra(EXTRA_END_MINUTE, 0)
                val location = intent.getStringExtra(EXTRA_LOCATION) ?: ""
                val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

                // Dismiss notification
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.cancel(notificationId)

                CoroutineScope(Dispatchers.IO).launch {
                    val dateStr = LocalDate.now().toString()
                    val existing = if (ttSessionId != null) {
                        db.classSessionDao().findSessionByTimetableAndDate(ttSessionId, dateStr)
                    } else {
                        db.classSessionDao().findSessionBySubjectDateAndTime(subjectId, dateStr, startMinute)
                    }

                    if (existing != null) {
                        db.classSessionDao().updateSession(
                            existing.copy(status = statusStr, updatedAt = System.currentTimeMillis())
                        )
                    } else {
                        db.classSessionDao().insertSession(
                            ClassSessionEntity(
                                subjectId = subjectId,
                                timetableSessionId = ttSessionId,
                                date = dateStr,
                                startMinute = startMinute,
                                endMinute = endMinute,
                                location = location,
                                status = statusStr,
                                attendanceUnits = 1
                            )
                        )
                    }

                    WidgetUpdater.notifyDataChanged(context)
                }
            }

            ACTION_MARK_LEAVE -> {
                val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.cancel(notificationId)

                CoroutineScope(Dispatchers.IO).launch {
                    prefsManager.setOnLeaveToday(true, "CANCELLED")
                    WidgetUpdater.notifyDataChanged(context)
                }
            }

            ACTION_CHECK_CLASS_PROMPT -> {
                val subjectId = intent.getLongExtra(EXTRA_SUBJECT_ID, 0L)
                val subjectName = intent.getStringExtra(EXTRA_SUBJECT_NAME) ?: "Class"
                val ttSessionId = intent.getLongExtra(EXTRA_TIMETABLE_SESSION_ID, 0L)
                val startMinute = intent.getIntExtra(EXTRA_START_MINUTE, 0)
                val endMinute = intent.getIntExtra(EXTRA_END_MINUTE, 0)
                val location = intent.getStringExtra(EXTRA_LOCATION) ?: ""

                // Refresh widgets when class ends so widget immediately transitions to next class
                WidgetUpdater.notifyDataChanged(context)

                CoroutineScope(Dispatchers.IO).launch {
                    val prefs = prefsManager.userPreferencesFlow.first()
                    // Don't bother user if on leave or notifications disabled
                    if (prefs.isOnLeaveToday || !prefs.notificationsEnabled) return@launch

                    val dateStr = LocalDate.now().toString()
                    // Don't bother user on university holidays
                    if (db.holidayDao().getHolidayForDateSync(dateStr) != null) return@launch

                    val existing = if (ttSessionId != 0L) {
                        db.classSessionDao().findSessionByTimetableAndDate(ttSessionId, dateStr)
                    } else {
                        db.classSessionDao().findSessionBySubjectDateAndTime(subjectId, dateStr, startMinute)
                    }

                    // If already marked, don't notify
                    if (existing != null) return@launch

                    val notificationId = (((if (ttSessionId != 0L) ttSessionId.hashCode() else subjectId.hashCode()) * 31 + startMinute) and 0x7FFFFFFF)

                    // Present Action
                    val presentIntent = Intent(context, AttendanceNotificationReceiver::class.java).apply {
                        action = ACTION_MARK_ATTENDANCE
                        data = Uri.parse("attendly://notification/action/present/$notificationId")
                        putExtra(EXTRA_SUBJECT_ID, subjectId)
                        putExtra(EXTRA_STATUS, AttendanceStatus.PRESENT.name)
                        putExtra(EXTRA_TIMETABLE_SESSION_ID, ttSessionId)
                        putExtra(EXTRA_START_MINUTE, startMinute)
                        putExtra(EXTRA_END_MINUTE, endMinute)
                        putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                    }
                    val presentPi = PendingIntent.getBroadcast(
                        context,
                        (notificationId % 100000) * 10 + 1,
                        presentIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    // Absent Action
                    val absentIntent = Intent(context, AttendanceNotificationReceiver::class.java).apply {
                        action = ACTION_MARK_ATTENDANCE
                        data = Uri.parse("attendly://notification/action/absent/$notificationId")
                        putExtra(EXTRA_SUBJECT_ID, subjectId)
                        putExtra(EXTRA_STATUS, AttendanceStatus.ABSENT.name)
                        putExtra(EXTRA_TIMETABLE_SESSION_ID, ttSessionId)
                        putExtra(EXTRA_START_MINUTE, startMinute)
                        putExtra(EXTRA_END_MINUTE, endMinute)
                        putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                    }
                    val absentPi = PendingIntent.getBroadcast(
                        context,
                        (notificationId % 100000) * 10 + 2,
                        absentIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    // Cancelled Action
                    val cancelledIntent = Intent(context, AttendanceNotificationReceiver::class.java).apply {
                        action = ACTION_MARK_ATTENDANCE
                        data = Uri.parse("attendly://notification/action/cancelled/$notificationId")
                        putExtra(EXTRA_SUBJECT_ID, subjectId)
                        putExtra(EXTRA_STATUS, AttendanceStatus.CANCELLED.name)
                        putExtra(EXTRA_TIMETABLE_SESSION_ID, ttSessionId)
                        putExtra(EXTRA_START_MINUTE, startMinute)
                        putExtra(EXTRA_END_MINUTE, endMinute)
                        putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                    }
                    val cancelledPi = PendingIntent.getBroadcast(
                        context,
                        (notificationId % 100000) * 10 + 3,
                        cancelledIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    // Content intent (open app)
                    val openAppIntent = Intent(context, MainActivity::class.java)
                    val contentPi = PendingIntent.getActivity(
                        context,
                        0,
                        openAppIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val timeStr = "${TimeUtils.minutesToTimeString(startMinute, prefs.timeFormat)} - ${TimeUtils.minutesToTimeString(endMinute, prefs.timeFormat)}"
                    val locStr = if (location.isNotEmpty()) " • $location" else ""

                    val largeIcon = try {
                        BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
                    } catch (e: Exception) {
                        null
                    }

                    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setColor(0xFF0849A8.toInt())
                        .apply {
                            if (largeIcon != null) {
                                setLargeIcon(largeIcon)
                            }
                        }
                        .setContentTitle("Did you attend $subjectName?")
                        .setContentText("$timeStr$locStr")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(contentPi)
                        .addAction(0, "✓ Attended", presentPi)
                        .addAction(0, "✕ Missed", absentPi)
                        .addAction(0, "🚫 Cancelled", cancelledPi)

                    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(notificationId, builder.build())
                }
            }
        }
    }
}
