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
import com.hrshd1eux.attendly.core.model.TimeFormat
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
    val notificationsEnabled: Boolean = true
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
            notificationsEnabled = notificationsEnabled
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
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }
}
