package com.hrshd1eux.attendly.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hrshd1eux.attendly.widget.WidgetUpdater
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BootReceiverEntryPoint {
    fun classAlarmScheduler(): ClassAlarmScheduler
}

class BootReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SCHEDULE_DAILY_ALARMS = "com.hrshd1eux.attendly.ACTION_SCHEDULE_DAILY_ALARMS"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == "android.intent.action.TIME_SET" ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == ACTION_SCHEDULE_DAILY_ALARMS
        ) {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                BootReceiverEntryPoint::class.java
            )
            val scheduler = entryPoint.classAlarmScheduler()

            CoroutineScope(Dispatchers.IO).launch {
                scheduler.scheduleTodayClassAlarms(context.applicationContext)
                WidgetUpdater.notifyDataChanged(context.applicationContext)
            }
        }
    }
}
