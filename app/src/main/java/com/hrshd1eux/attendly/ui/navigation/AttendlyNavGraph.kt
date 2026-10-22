package com.hrshd1eux.attendly.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hrshd1eux.attendly.ui.calendar.CalendarScreen
import com.hrshd1eux.attendly.ui.components.AttendlyBottomBar
import com.hrshd1eux.attendly.ui.home.HomeScreen
import com.hrshd1eux.attendly.ui.onboarding.OnboardingScreen
import com.hrshd1eux.attendly.ui.settings.BackupRestoreScreen
import com.hrshd1eux.attendly.ui.settings.SettingsScreen
import com.hrshd1eux.attendly.ui.simulator.BunkSimulatorScreen
import com.hrshd1eux.attendly.ui.subjects.SubjectDetailScreen
import com.hrshd1eux.attendly.ui.subjects.SubjectsScreen
import com.hrshd1eux.attendly.ui.holidays.HolidaysScreen
import com.hrshd1eux.attendly.ui.timetable.ImportTimetableScreen
import com.hrshd1eux.attendly.ui.timetable.TimetableScreen

@Composable
fun AttendlyAppNavigation(
    isOnboardingCompleted: Boolean
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarRoutes = listOf(
        Screen.Home.route,
        Screen.Subjects.route,
        Screen.Calendar.route,
        Screen.Settings.route
    )

    val showBottomBar = currentRoute in bottomBarRoutes

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                AttendlyBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        val startDestination = if (isOnboardingCompleted) Screen.Home.route else Screen.Onboarding.route

        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            // Onboarding
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinished = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            // Home
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToSubjectDetail = { id ->
                        navController.navigate(Screen.SubjectDetail.createRoute(id))
                    },
                    onNavigateToAddSubject = {
                        navController.navigate(Screen.Subjects.route)
                    },
                    onNavigateToTimetable = {
                        navController.navigate(Screen.Timetable.route)
                    },
                    onNavigateToCalendar = {
                        navController.navigate(Screen.Calendar.route)
                    },
                    onNavigateToHolidays = {
                        navController.navigate(Screen.Holidays.route)
                    },
                    onNavigateToSimulator = {
                        navController.navigate(Screen.Simulator.route)
                    }
                )
            }

            // Subjects
            composable(Screen.Subjects.route) {
                SubjectsScreen(
                    onNavigateToSubjectDetail = { id ->
                        navController.navigate(Screen.SubjectDetail.createRoute(id))
                    }
                )
            }

            // Subject Detail
            composable(
                route = Screen.SubjectDetail.route,
                arguments = listOf(navArgument("subjectId") { type = NavType.LongType })
            ) {
                SubjectDetailScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Calendar
            composable(Screen.Calendar.route) {
                CalendarScreen()
            }

            // Timetable
            composable(Screen.Timetable.route) {
                TimetableScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToImport = {
                        navController.navigate(Screen.ImportTimetable.route)
                    }
                )
            }

            // Import Timetable
            composable(Screen.ImportTimetable.route) {
                ImportTimetableScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Holidays
            composable(Screen.Holidays.route) {
                HolidaysScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Bunk Simulator
            composable(Screen.Simulator.route) {
                BunkSimulatorScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Settings
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToTimetable = {
                        navController.navigate(Screen.Timetable.route)
                    },
                    onNavigateToImportTimetable = {
                        navController.navigate(Screen.ImportTimetable.route)
                    },
                    onNavigateToHolidays = {
                        navController.navigate(Screen.Holidays.route)
                    },
                    onNavigateToBackupRestore = {
                        navController.navigate(Screen.BackupRestore.route)
                    }
                )
            }

            // Backup & Restore
            composable(Screen.BackupRestore.route) {
                BackupRestoreScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
