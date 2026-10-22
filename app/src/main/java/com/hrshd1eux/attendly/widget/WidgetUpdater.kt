package com.hrshd1eux.attendly.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object WidgetUpdater {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun notifyDataChanged(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            // 1. Directly trigger Glance composition updates (isolated try-catch for each widget)
            try {
                AttendlyGlanceWidget().updateAll(appContext)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                QuickAttendanceGlanceWidget().updateAll(appContext)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Explicitly send ACTION_APPWIDGET_UPDATE broadcasts so Android launcher hosts refresh immediately
            try {
                val appWidgetManager = AppWidgetManager.getInstance(appContext)

                val attendlyComponent = ComponentName(appContext, AttendlyWidgetReceiver::class.java)
                val attendlyIds = appWidgetManager.getAppWidgetIds(attendlyComponent)
                if (attendlyIds != null && attendlyIds.isNotEmpty()) {
                    val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                        component = attendlyComponent
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, attendlyIds)
                    }
                    appContext.sendBroadcast(intent)
                }

                val quickComponent = ComponentName(appContext, QuickAttendanceWidgetReceiver::class.java)
                val quickIds = appWidgetManager.getAppWidgetIds(quickComponent)
                if (quickIds != null && quickIds.isNotEmpty()) {
                    val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                        component = quickComponent
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, quickIds)
                    }
                    appContext.sendBroadcast(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
