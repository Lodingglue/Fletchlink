package com.toolkit.fletchlink.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryOrangeRed,
    onPrimary = DarkSurface,
    primaryContainer = PrimaryOrangeRedDark,
    onPrimaryContainer = LightText,
    secondary = SecondaryOrange,
    onSecondary = DarkSurface,
    secondaryContainer = SecondaryOrangeDark,
    onSecondaryContainer = LightText,
    tertiary = TertiaryRed,
    onTertiary = DarkSurface,
    tertiaryContainer = TertiaryRedDark,
    onTertiaryContainer = LightText,
    error = ErrorRed,
    onError = LightText,
    errorContainer = ErrorRedDark,
    onErrorContainer = LightText,
    background = DarkBackground,
    onBackground = LightText,
    surface = DarkSurface,
    onSurface = LightText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    scrim = DarkScrim,
    inverseSurface = LightSurface,
    inverseOnSurface = DarkText,
    inversePrimary = PrimaryOrangeRedLight,
    surfaceDim = DarkSurfaceDim,
    surfaceBright = DarkSurfaceBright,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryOrangeRedLight,
    onPrimary = LightSurface,
    primaryContainer = PrimaryOrangeRedContainer,
    onPrimaryContainer = DarkText,
    secondary = SecondaryOrangeLight,
    onSecondary = LightSurface,
    secondaryContainer = SecondaryOrangeContainer,
    onSecondaryContainer = DarkText,
    tertiary = TertiaryRedLight,
    onTertiary = LightSurface,
    tertiaryContainer = TertiaryRedContainer,
    onTertiaryContainer = DarkText,
    error = ErrorRedLight,
    onError = LightSurface,
    errorContainer = ErrorRedContainer,
    onErrorContainer = DarkText,
    background = LightBackground,
    onBackground = DarkText,
    surface = LightSurface,
    onSurface = DarkText,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    scrim = LightScrim,
    inverseSurface = DarkSurface,
    inverseOnSurface = LightText,
    inversePrimary = PrimaryOrangeRed,
    surfaceDim = LightSurfaceDim,
    surfaceBright = LightSurfaceBright,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
)

@Composable
fun FletchlinkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    
    dynamicColor: Boolean = false, 
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}