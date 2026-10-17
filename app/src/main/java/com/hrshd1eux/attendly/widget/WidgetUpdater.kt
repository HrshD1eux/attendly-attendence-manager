package com.hrshd1eux.attendly.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object WidgetUpdater {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun notifyDataChanged(context: Context) {
        scope.launch {
            try {
                AttendlyGlanceWidget().updateAll(context)
                QuickAttendanceGlanceWidget().updateAll(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
