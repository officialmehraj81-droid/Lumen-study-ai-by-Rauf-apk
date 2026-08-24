package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GoldDarkScheme = darkColorScheme(
    primary = GoldPrimaryDark,
    onPrimary = GoldOnPrimaryDark,
    primaryContainer = GoldPrimaryContainerDark,
    onPrimaryContainer = GoldOnPrimaryContainerDark,
    secondary = GoldSecondaryDark,
    onSecondary = Color(0xFF0A0B0E),
    surface = GoldSurfaceDark,
    onSurface = SophisticatedDarkOnSurface,
    surfaceVariant = GoldSurfaceVariantDark,
    onSurfaceVariant = SophisticatedDarkOnSurfaceVariant,
    background = GoldBackgroundDark,
    onBackground = SophisticatedDarkOnSurface,
    outline = SophisticatedDarkOutline,
    outlineVariant = SophisticatedDarkOutlineVariant,
    surfaceContainer = SophisticatedDarkSurfaceElevated,
    error = ErrorRed,
    onError = Color.White
)

private val GoldLightScheme = lightColorScheme(
    primary = GoldPrimaryLight,
    onPrimary = GoldOnPrimaryLight,
    primaryContainer = GoldPrimaryContainerLight,
    onPrimaryContainer = GoldOnPrimaryContainerLight,
    secondary = GoldSecondaryLight,
    onSecondary = Color.White,
    surface = GoldSurfaceLight,
    onSurface = SophisticatedLightOnSurface,
    surfaceVariant = GoldSurfaceVariantLight,
    onSurfaceVariant = SophisticatedLightOnSurfaceVariant,
    background = GoldBackgroundLight,
    onBackground = SophisticatedLightOnSurface,
    outline = SophisticatedLightOutline,
    outlineVariant = SophisticatedLightOutlineVariant,
    surfaceContainer = SophisticatedLightSurfaceElevated,
    error = ErrorRed,
    onError = Color.White
)

private val IndigoDarkScheme = darkColorScheme(
    primary = IndigoPrimaryDark,
    onPrimary = IndigoOnPrimaryDark,
    primaryContainer = IndigoPrimaryContainerDark,
    onPrimaryContainer = IndigoOnPrimaryContainerDark,
    secondary = IndigoSecondaryDark,
    onSecondary = Color(0xFF0A0B0E),
    surface = IndigoSurfaceDark,
    onSurface = SophisticatedDarkOnSurface,
    surfaceVariant = IndigoSurfaceVariantDark,
    onSurfaceVariant = SophisticatedDarkOnSurfaceVariant,
    background = IndigoBackgroundDark,
    onBackground = SophisticatedDarkOnSurface,
    outline = SophisticatedDarkOutline,
    outlineVariant = SophisticatedDarkOutlineVariant,
    surfaceContainer = SophisticatedDarkSurfaceElevated,
    error = ErrorRed,
    onError = Color.White
)

private val IndigoLightScheme = lightColorScheme(
    primary = IndigoPrimaryLight,
    onPrimary = IndigoOnPrimaryLight,
    primaryContainer = IndigoPrimaryContainerLight,
    onPrimaryContainer = IndigoOnPrimaryContainerLight,
    secondary = IndigoSecondaryLight,
    onSecondary = Color.White,
    surface = IndigoSurfaceLight,
    onSurface = SophisticatedLightOnSurface,
    surfaceVariant = IndigoSurfaceVariantLight,
    onSurfaceVariant = SophisticatedLightOnSurfaceVariant,
    background = IndigoBackgroundLight,
    onBackground = SophisticatedLightOnSurface,
    outline = SophisticatedLightOutline,
    outlineVariant = SophisticatedLightOutlineVariant,
    surfaceContainer = SophisticatedLightSurfaceElevated,
    error = ErrorRed,
    onError = Color.White
)

private val EmeraldDarkScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = EmeraldOnPrimaryDark,
    primaryContainer = EmeraldPrimaryContainerDark,
    onPrimaryContainer = EmeraldOnPrimaryContainerDark,
    secondary = EmeraldSecondaryDark,
    onSecondary = Color(0xFF0A0B0E),
    surface = EmeraldSurfaceDark,
    onSurface = SophisticatedDarkOnSurface,
    surfaceVariant = EmeraldSurfaceVariantDark,
    onSurfaceVariant = SophisticatedDarkOnSurfaceVariant,
    background = EmeraldBackgroundDark,
    onBackground = SophisticatedDarkOnSurface,
    outline = SophisticatedDarkOutline,
    outlineVariant = SophisticatedDarkOutlineVariant,
    surfaceContainer = SophisticatedDarkSurfaceElevated,
    error = ErrorRed,
    onError = Color.White
)

private val EmeraldLightScheme = lightColorScheme(
    primary = EmeraldPrimaryLight,
    onPrimary = EmeraldOnPrimaryLight,
    primaryContainer = EmeraldPrimaryContainerLight,
    onPrimaryContainer = EmeraldOnPrimaryContainerLight,
    secondary = EmeraldSecondaryLight,
    onSecondary = Color.White,
    surface = EmeraldSurfaceLight,
    onSurface = SophisticatedLightOnSurface,
    surfaceVariant = EmeraldSurfaceVariantLight,
    onSurfaceVariant = SophisticatedLightOnSurfaceVariant,
    background = EmeraldBackgroundLight,
    onBackground = SophisticatedLightOnSurface,
    outline = SophisticatedLightOutline,
    outlineVariant = SophisticatedLightOutlineVariant,
    surfaceContainer = SophisticatedLightSurfaceElevated,
    error = ErrorRed,
    onError = Color.White
)

private val RoseDarkScheme = darkColorScheme(
    primary = RosePrimaryDark,
    onPrimary = RoseOnPrimaryDark,
    primaryContainer = RosePrimaryContainerDark,
    onPrimaryContainer = RoseOnPrimaryContainerDark,
    secondary = RoseSecondaryDark,
    onSecondary = Color(0xFF0A0B0E),
    surface = RoseSurfaceDark,
    onSurface = SophisticatedDarkOnSurface,
    surfaceVariant = RoseSurfaceVariantDark,
    onSurfaceVariant = SophisticatedDarkOnSurfaceVariant,
    background = RoseBackgroundDark,
    onBackground = SophisticatedDarkOnSurface,
    outline = SophisticatedDarkOutline,
    outlineVariant = SophisticatedDarkOutlineVariant,
    surfaceContainer = SophisticatedDarkSurfaceElevated,
    error = ErrorRed,
    onError = Color.White
)

private val RoseLightScheme = lightColorScheme(
    primary = RosePrimaryLight,
    onPrimary = RoseOnPrimaryLight,
    primaryContainer = RosePrimaryContainerLight,
    onPrimaryContainer = RoseOnPrimaryContainerLight,
    secondary = RoseSecondaryLight,
    onSecondary = Color.White,
    surface = RoseSurfaceLight,
    onSurface = SophisticatedLightOnSurface,
    surfaceVariant = RoseSurfaceVariantLight,
    onSurfaceVariant = SophisticatedLightOnSurfaceVariant,
    background = RoseBackgroundLight,
    onBackground = SophisticatedLightOnSurface,
    outline = SophisticatedLightOutline,
    outlineVariant = SophisticatedLightOutlineVariant,
    surfaceContainer = SophisticatedLightSurfaceElevated,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun LumenTheme(
    themeMode: String = "dark", // "dark", "light", "system"
    accentColor: String = "gold", // "gold", "indigo", "emerald", "rose"
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        "light" -> false
        "system" -> isSystemInDarkTheme()
        else -> true
    }

    val colorScheme = when (accentColor) {
        "indigo" -> if (isDark) IndigoDarkScheme else IndigoLightScheme
        "emerald" -> if (isDark) EmeraldDarkScheme else EmeraldLightScheme
        "rose" -> if (isDark) RoseDarkScheme else RoseLightScheme
        else -> if (isDark) GoldDarkScheme else GoldLightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
