package com.nxyn.aiclient.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.nxyn.aiclient.domain.model.ThemeMode

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8B5CF6),
    onPrimary = Color.White,
    secondary = Color(0xFF60A5FA),
    onSecondary = Color(0xFF0F172A),
    tertiary = Color(0xFF22D3EE),
    background = Color(0xFF0F1117),
    onBackground = Color(0xFFE5E7EB),
    surface = Color(0xFF171923),
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = Color(0xFF1F2430),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF334155),
    error = Color(0xFFF87171)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF7C3AED),
    onPrimary = Color.White,
    secondary = Color(0xFF2563EB),
    onSecondary = Color.White,
    tertiary = Color(0xFF0891B2),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFEEF2FF),
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626)
)

object AppGradients {
    val accent = Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFF2563EB)))
    val glass = Brush.verticalGradient(
        listOf(Color(0x332563EB), Color(0x11111827))
    )
}

@Composable
fun AIClientTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
