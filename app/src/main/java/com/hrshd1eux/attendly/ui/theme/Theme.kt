package com.hrshd1eux.attendly.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.hrshd1eux.attendly.core.model.AppTheme

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark
)

data class AttendlyStatusColors(
    val present: Color,
    val presentContainer: Color,
    val onPresentContainer: Color,
    val absent: Color,
    val absentContainer: Color,
    val onAbsentContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val cancelled: Color,
    val cancelledContainer: Color,
    val onCancelledContainer: Color
)

val LocalStatusColors = staticCompositionLocalOf {
    AttendlyStatusColors(
        present = StatusPresentLight,
        presentContainer = StatusPresentContainerLight,
        onPresentContainer = StatusOnPresentContainerLight,
        absent = StatusAbsentLight,
        absentContainer = StatusAbsentContainerLight,
        onAbsentContainer = StatusOnAbsentContainerLight,
        warning = StatusWarningLight,
        warningContainer = StatusWarningContainerLight,
        onWarningContainer = StatusOnWarningContainerLight,
        cancelled = StatusCancelledLight,
        cancelledContainer = StatusCancelledContainerLight,
        onCancelledContainer = StatusOnCancelledContainerLight
    )
}

@Composable
fun AttendlyTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (appTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme: ColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val statusColors = if (darkTheme) {
        AttendlyStatusColors(
            present = StatusPresentDark,
            presentContainer = StatusPresentContainerDark,
            onPresentContainer = StatusOnPresentContainerDark,
            absent = StatusAbsentDark,
            absentContainer = StatusAbsentContainerDark,
            onAbsentContainer = StatusOnAbsentContainerDark,
            warning = StatusWarningDark,
            warningContainer = StatusWarningContainerDark,
            onWarningContainer = StatusOnWarningContainerDark,
            cancelled = StatusCancelledDark,
            cancelledContainer = StatusCancelledContainerDark,
            onCancelledContainer = StatusOnCancelledContainerDark
        )
    } else {
        AttendlyStatusColors(
            present = StatusPresentLight,
            presentContainer = StatusPresentContainerLight,
            onPresentContainer = StatusOnPresentContainerLight,
            absent = StatusAbsentLight,
            absentContainer = StatusAbsentContainerLight,
            onAbsentContainer = StatusOnAbsentContainerLight,
            warning = StatusWarningLight,
            warningContainer = StatusWarningContainerLight,
            onWarningContainer = StatusOnWarningContainerLight,
            cancelled = StatusCancelledLight,
            cancelledContainer = StatusCancelledContainerLight,
            onCancelledContainer = StatusOnCancelledContainerLight
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalStatusColors provides statusColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
