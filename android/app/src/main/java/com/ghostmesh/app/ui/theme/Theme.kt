package com.ghostmesh.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.ghostmesh.app.data.model.AppearanceMode
import com.ghostmesh.app.data.model.ThemePreset

private fun createM3ColorScheme(aura: AuraColors): ColorScheme {
    return if (aura.isDark) {
        darkColorScheme(
            primary = aura.accent,
            onPrimary = aura.canvas,
            primaryContainer = aura.accentSubtle,
            onPrimaryContainer = aura.accent,
            secondary = aura.inkSecondary,
            onSecondary = aura.canvas,
            background = aura.canvas,
            onBackground = aura.ink,
            surface = aura.surface,
            onSurface = aura.ink,
            surfaceVariant = aura.surfaceSubtle,
            onSurfaceVariant = aura.inkSecondary,
            outline = aura.border,
            error = aura.danger,
            onError = aura.canvas
        )
    } else {
        lightColorScheme(
            primary = aura.accent,
            onPrimary = aura.surface,
            primaryContainer = aura.accentSubtle,
            onPrimaryContainer = aura.accentHover,
            secondary = aura.inkSecondary,
            onSecondary = aura.surface,
            background = aura.canvas,
            onBackground = aura.ink,
            surface = aura.surface,
            onSurface = aura.ink,
            surfaceVariant = aura.surfaceSubtle,
            onSurfaceVariant = aura.inkSecondary,
            outline = aura.border,
            error = aura.danger,
            onError = aura.surface
        )
    }
}

@Composable
fun PlexTheme(
    preset: ThemePreset = ThemePreset.CLAUDE_WARM,
    appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (appearanceMode) {
        AppearanceMode.SYSTEM -> isSystemDark
        AppearanceMode.LIGHT -> false
        AppearanceMode.DARK -> true
    }

    val auraColors = getThemeColors(preset, isDark)
    val m3ColorScheme = createM3ColorScheme(auraColors)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = auraColors.canvas.toArgb()
                window.navigationBarColor = auraColors.canvas.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalAuraColors provides auraColors
    ) {
        MaterialTheme(
            colorScheme = m3ColorScheme,
            typography = AuraTypography,
            content = content
        )
    }
}

// Backwards-compatible alias for existing code
@Composable
fun AuraRadarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    PlexTheme(
        preset = ThemePreset.CLAUDE_WARM,
        appearanceMode = if (darkTheme) AppearanceMode.DARK else AppearanceMode.LIGHT,
        content = content
    )
}

// Convenient accessor
object AuraTheme {
    val colors: AuraColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAuraColors.current

    val typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography
}

// Backwards-compatible theme alias
@Composable
fun GhostMeshTheme(content: @Composable () -> Unit) {
    PlexTheme(content = content)
}
