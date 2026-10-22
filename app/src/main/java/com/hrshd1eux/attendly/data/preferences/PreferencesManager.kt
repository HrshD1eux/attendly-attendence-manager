package com.hrshd1eux.attendly.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hrshd1eux.attendly.core.model.AppTheme
import com.hrshd1eux.attendly.core.model.AttendanceUnitPolicy
import com.hrshd1eux.attendly.core.model.DayOfWeek
import com.hrshd1eux.attendly.core.model.MedicalLeavePolicy
import com.hrshd1eux.attendly.core.model.OnDutyPolicy
import com.hrshd1eux.attendly.core.model.TimeFormat
import com.hrshd1eux.attendly.widget.WidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "attendly_preferences")

data class UserPreferences(
    val userName: String = "",
    val defaultTargetPercentage: Int = 75,
    val appTheme: AppTheme = AppTheme.SYSTEM,
    val timeFormat: TimeFormat = TimeFormat.FORMAT_12H,
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val defaultLabUnitPolicy: AttendanceUnitPolicy = AttendanceUnitPolicy.ONE_PER_SESSION,
    val isOnboardingCompleted: Boolean = false,
    val lastBackupTimestamp: Long = 0L,
    val leaveDate: String = "",
    val leaveType: String = "CANCELLED",
    val notificationsEnabled: Boolean = true,
    val notificationOffsetMinutes: Int = 0,
    val onDutyPolicy: OnDutyPolicy = OnDutyPolicy.COUNT_AS_PRESENT,
    val medicalLeavePolicy: MedicalLeavePolicy = MedicalLeavePolicy.EXCUSED,
    val medicalConcessionTarget: Int = 65,
    val autoRestorePromptDismissed: Boolean = false
) {
    val isOnLeaveToday: Boolean
        get() = leaveDate == java.time.LocalDate.now().toString()
}

@Singleton
class PreferencesManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    private object PreferencesKeys {
        val USER_NAME = stringPreferencesKey("user_name")
        val DEFAULT_TARGET = intPreferencesKey("default_target")
        val APP_THEME = stringPreferencesKey("app_theme")
        val TIME_FORMAT = stringPreferencesKey("time_format")
        val FIRST_DAY_OF_WEEK = stringPreferencesKey("first_day_of_week")
        val LAB_UNIT_POLICY = stringPreferencesKey("lab_unit_policy")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
        val LEAVE_DATE = stringPreferencesKey("leave_date")
        val LEAVE_TYPE = stringPreferencesKey("leave_type")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val NOTIFICATION_OFFSET_MINUTES = intPreferencesKey("notification_offset_minutes")
        val ON_DUTY_POLICY = stringPreferencesKey("on_duty_policy")
        val MEDICAL_LEAVE_POLICY = stringPreferencesKey("medical_leave_policy")
        val MEDICAL_CONCESSION_TARGET = intPreferencesKey("medical_concession_target")
        val AUTO_RESTORE_PROMPT_DISMISSED = booleanPreferencesKey("auto_restore_prompt_dismissed")
    }

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data.map { preferences ->
        val userName = preferences[PreferencesKeys.USER_NAME] ?: ""
        val target = preferences[PreferencesKeys.DEFAULT_TARGET] ?: 75
        val themeStr = preferences[PreferencesKeys.APP_THEME] ?: AppTheme.SYSTEM.name
        val timeFormatStr = preferences[PreferencesKeys.TIME_FORMAT] ?: TimeFormat.FORMAT_12H.name
        val firstDayStr = preferences[PreferencesKeys.FIRST_DAY_OF_WEEK] ?: DayOfWeek.MONDAY.name
        val labPolicyStr = preferences[PreferencesKeys.LAB_UNIT_POLICY] ?: AttendanceUnitPolicy.ONE_PER_SESSION.name
        val onboarding = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
        val lastBackup = preferences[PreferencesKeys.LAST_BACKUP_TIMESTAMP] ?: 0L
        val leaveDate = preferences[PreferencesKeys.LEAVE_DATE] ?: ""
        val leaveType = preferences[PreferencesKeys.LEAVE_TYPE] ?: "CANCELLED"
        val notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true
        val notifOffset = preferences[PreferencesKeys.NOTIFICATION_OFFSET_MINUTES] ?: 0
        val odPolicyStr = preferences[PreferencesKeys.ON_DUTY_POLICY] ?: OnDutyPolicy.COUNT_AS_PRESENT.name
        val medPolicyStr = preferences[PreferencesKeys.MEDICAL_LEAVE_POLICY] ?: MedicalLeavePolicy.EXCUSED.name
        val medTarget = preferences[PreferencesKeys.MEDICAL_CONCESSION_TARGET] ?: 65
        val autoRestoreDismissed = preferences[PreferencesKeys.AUTO_RESTORE_PROMPT_DISMISSED] ?: false

        UserPreferences(
            userName = userName,
            defaultTargetPercentage = target,
            appTheme = runCatching { AppTheme.valueOf(themeStr) }.getOrDefault(AppTheme.SYSTEM),
            timeFormat = runCatching { TimeFormat.valueOf(timeFormatStr) }.getOrDefault(TimeFormat.FORMAT_12H),
            firstDayOfWeek = runCatching { DayOfWeek.valueOf(firstDayStr) }.getOrDefault(DayOfWeek.MONDAY),
            defaultLabUnitPolicy = runCatching { AttendanceUnitPolicy.valueOf(labPolicyStr) }.getOrDefault(AttendanceUnitPolicy.ONE_PER_SESSION),
            isOnboardingCompleted = onboarding,
            lastBackupTimestamp = lastBackup,
            leaveDate = leaveDate,
            leaveType = leaveType,
            notificationsEnabled = notificationsEnabled,
            notificationOffsetMinutes = notifOffset,
            onDutyPolicy = runCatching { OnDutyPolicy.valueOf(odPolicyStr) }.getOrDefault(OnDutyPolicy.COUNT_AS_PRESENT),
            medicalLeavePolicy = runCatching { MedicalLeavePolicy.valueOf(medPolicyStr) }.getOrDefault(MedicalLeavePolicy.EXCUSED),
            medicalConcessionTarget = medTarget,
            autoRestorePromptDismissed = autoRestoreDismissed
        )
    }

    suspend fun setUserName(name: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_NAME] = name.trim()
        }
    }

    suspend fun setDefaultTarget(target: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_TARGET] = target.coerceIn(1, 100)
        }
    }

    suspend fun setAppTheme(theme: AppTheme) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_THEME] = theme.name
        }
    }

    suspend fun setTimeFormat(format: TimeFormat) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TIME_FORMAT] = format.name
        }
        WidgetUpdater.notifyDataChanged(context)
    }

    suspend fun setFirstDayOfWeek(day: DayOfWeek) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FIRST_DAY_OF_WEEK] = day.name
        }
    }

    suspend fun setDefaultLabUnitPolicy(policy: AttendanceUnitPolicy) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAB_UNIT_POLICY] = policy.name
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setLastBackupTimestamp(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_BACKUP_TIMESTAMP] = timestamp
        }
    }

    suspend fun setOnLeaveToday(onLeave: Boolean, leaveType: String = "CANCELLED") {
        dataStore.edit { preferences ->
            if (onLeave) {
                preferences[PreferencesKeys.LEAVE_DATE] = java.time.LocalDate.now().toString()
                preferences[PreferencesKeys.LEAVE_TYPE] = leaveType
            } else {
                preferences[PreferencesKeys.LEAVE_DATE] = ""
            }
        }
        WidgetUpdater.notifyDataChanged(context)
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setNotificationOffsetMinutes(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_OFFSET_MINUTES] = minutes.coerceIn(0, 30)
        }
    }

    suspend fun setOnDutyPolicy(policy: OnDutyPolicy) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ON_DUTY_POLICY] = policy.name
        }
        WidgetUpdater.notifyDataChanged(context)
    }

    suspend fun setMedicalLeavePolicy(policy: MedicalLeavePolicy) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.MEDICAL_LEAVE_POLICY] = policy.name
        }
        WidgetUpdater.notifyDataChanged(context)
    }

    suspend fun setMedicalConcessionTarget(target: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.MEDICAL_CONCESSION_TARGET] = target.coerceIn(40, 100)
        }
        WidgetUpdater.notifyDataChanged(context)
    }

    suspend fun setAutoRestorePromptDismissed(dismissed: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_RESTORE_PROMPT_DISMISSED] = dismissed
        }
    }
}
