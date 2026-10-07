package com.mark.moodlogger.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Honeycomb palette
private val Honey = Color(0xFFE9A319)      // primary in light mode
private val HoneyLight = Color(0xFFF2B94A) // primary in dark mode
private val OnHoney = Color(0xFF241A03)    // dark ink on honey

// Muted-honey "container" tones so selected chips, segmented buttons, and the
// time/date pickers stay in theme instead of falling back to Material purple.
private val HoneyContainerLight = Color(0xFFF7E4B8)
private val OnHoneyContainerLight = Color(0xFF3D2E05)
private val HoneyContainerDark = Color(0xFF54401C)
private val OnHoneyContainerDark = Color(0xFFF6DFB0)

private val LightColors = lightColorScheme(
    primary = Honey,
    onPrimary = OnHoney,
    primaryContainer = HoneyContainerLight,
    onPrimaryContainer = OnHoneyContainerLight,
    secondary = Color(0xFFC98A12),
    onSecondary = OnHoney,
    secondaryContainer = HoneyContainerLight,
    onSecondaryContainer = OnHoneyContainerLight,
    tertiary = Color(0xFFC98A12),
    onTertiary = OnHoney,
    tertiaryContainer = HoneyContainerLight,
    onTertiaryContainer = OnHoneyContainerLight,
)

private val DarkColors = darkColorScheme(
    primary = HoneyLight,
    onPrimary = OnHoney,
    primaryContainer = HoneyContainerDark,
    onPrimaryContainer = OnHoneyContainerDark,
    secondary = Color(0xFFE3B45C),
    onSecondary = OnHoney,
    secondaryContainer = HoneyContainerDark,
    onSecondaryContainer = OnHoneyContainerDark,
    tertiary = Color(0xFFE3B45C),
    onTertiary = OnHoney,
    tertiaryContainer = HoneyContainerDark,
    onTertiaryContainer = OnHoneyContainerDark,
)

@Composable
fun MoodLoggerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
