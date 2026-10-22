package com.hrshd1eux.attendly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import android.os.Build
import android.content.pm.PackageManager
import androidx.lifecycle.lifecycleScope
import com.hrshd1eux.attendly.data.preferences.PreferencesManager
import com.hrshd1eux.attendly.data.preferences.UserPreferences
import com.hrshd1eux.attendly.notification.ClassAlarmScheduler
import com.hrshd1eux.attendly.ui.navigation.AttendlyAppNavigation
import com.hrshd1eux.attendly.ui.theme.AttendlyTheme
import com.hrshd1eux.attendly.widget.WidgetUpdater
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var classAlarmScheduler: ClassAlarmScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        lifecycleScope.launch {
            classAlarmScheduler.scheduleTodayClassAlarms(applicationContext)
        }

        setContent {
            val userPreferences by preferencesManager.userPreferencesFlow.collectAsState(
                initial = UserPreferences()
            )

            AttendlyTheme(appTheme = userPreferences.appTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AttendlyAppNavigation(
                        isOnboardingCompleted = userPreferences.isOnboardingCompleted
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        WidgetUpdater.notifyDataChanged(applicationContext)
    }
}
