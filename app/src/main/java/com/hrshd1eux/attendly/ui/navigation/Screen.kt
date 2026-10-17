package com.hrshd1eux.attendly.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Subjects : Screen("subjects")
    object Calendar : Screen("calendar")
    object Settings : Screen("settings")
    object SubjectDetail : Screen("subject_detail/{subjectId}") {
        fun createRoute(subjectId: Long) = "subject_detail/$subjectId"
    }
    object Timetable : Screen("timetable")
    object ImportTimetable : Screen("import_timetable")
    object Holidays : Screen("holidays")
    object BackupRestore : Screen("backup_restore")
    object About : Screen("about")
}
