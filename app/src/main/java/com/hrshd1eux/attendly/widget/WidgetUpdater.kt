package com.hrshd1eux.attendly.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object WidgetUpdater {
    private val scope = CoroutineScope(Dispatchers.Default)

    fun notifyDataChanged(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
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
        }
    }
}
