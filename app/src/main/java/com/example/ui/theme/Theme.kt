package com.example.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF04111B),
    primaryContainer = Color(0xFF0D2F45),
    onPrimaryContainer = ElectricCyan,
    secondary = CobaltBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1D284E),
    onSecondaryContainer = Color(0xFFBAC8FF),
    tertiary = EmeraldSuccess,
    onTertiary = Color(0xFF031A14),
    tertiaryContainer = Color(0xFF0E382E),
    onTertiaryContainer = EmeraldSuccess,
    background = DeepSpaceBg,
    onBackground = DeepSpaceText,
    surface = DeepSpaceSurface,
    onSurface = DeepSpaceText,
    surfaceVariant = DeepSpacePanelElevated,
    onSurfaceVariant = DeepSpaceMuted,
    surfaceContainerLowest = DeepSpaceBg,
    surfaceContainerLow = DeepSpaceSurface,
    surfaceContainer = DeepSpacePanel,
    surfaceContainerHigh = DeepSpacePanelElevated,
    outline = DeepSpaceBorder,
    outlineVariant = DeepSpaceBorder,
    error = RoseError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimaryPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE4FF),
    onPrimaryContainer = Color(0xFF3A1A78),
    secondary = LightSecondaryBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBE7FF),
    onSecondaryContainer = Color(0xFF143699),
    tertiary = EmeraldSuccess,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF065F46),
    background = LightLavenderBg,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightPanelElevated,
    onSurfaceVariant = LightMuted,
    surfaceContainerLowest = LightLavenderBg,
    surfaceContainerLow = LightSurface,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightPanelElevated,
    outline = LightBorder,
    outlineVariant = LightBorder,
    error = RoseError,
    onError = Color.White
)

private val Idea2ImpactShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun Idea2ImpactTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        LaunchedEffect(view, darkTheme) {
            val window = view.context.findActivity()?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Idea2ImpactTypography,
        shapes = Idea2ImpactShapes,
        content = content
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
