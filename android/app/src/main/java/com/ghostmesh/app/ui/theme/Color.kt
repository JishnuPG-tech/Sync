package com.ghostmesh.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.ghostmesh.app.data.model.ThemePreset

@Immutable
data class AuraColors(
    val canvas: Color,
    val surface: Color,
    val surfaceSubtle: Color,
    val surfaceElevated: Color,
    val ink: Color,
    val inkSecondary: Color,
    val inkMuted: Color,
    val border: Color,
    val accent: Color,
    val accentHover: Color,
    val accentSubtle: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val focusRing: Color,
    val isDark: Boolean
)

// ==========================================
// THEME 1: Claude Warm Paper & Terracotta (Exact tokens from HTML Prototype)
// ==========================================
val ClaudeWarmLightColors = AuraColors(
    canvas = Color(0xFFF5F2EB),       // --bg
    surface = Color(0xFFFBFAF6),      // --surface
    surfaceSubtle = Color(0xFFEEEAE1),// --surface2
    surfaceElevated = Color(0xFFFFFFFF),
    ink = Color(0xFF151513),          // --text
    inkSecondary = Color(0xFF6F6D66), // --muted
    inkMuted = Color(0xFF9B988F),     // --faint
    border = Color(0xFFD9D5CC),       // --line
    accent = Color(0xFFCF7052),       // --accent
    accentHover = Color(0xFF9F4E37),  // --accent2
    accentSubtle = Color(0x1FCF7052), // rgba(207, 112, 82, 0.12)
    success = Color(0xFF6B8C72),      // --green
    warning = Color(0xFFC4863A),
    danger = Color(0xFFC05252),       // --red
    focusRing = Color(0xFFCF7052),
    isDark = false
)

val ClaudeWarmDarkColors = AuraColors(
    canvas = Color(0xFF0B0C0B),       // --bg
    surface = Color(0xFF111311),      // --surface
    surfaceSubtle = Color(0xFF171A17),// --surface2
    surfaceElevated = Color(0xFF1D211D),
    ink = Color(0xFFF3F0E9),          // --text
    inkSecondary = Color(0xFF9B9B94), // --muted
    inkMuted = Color(0xFF666963),     // --faint
    border = Color(0xFF292C28),       // --line
    accent = Color(0xFFE18462),       // --accent
    accentHover = Color(0xFFF0A083),  // --accent2
    accentSubtle = Color(0x28E18462), // rgba(225, 132, 98, 0.16)
    success = Color(0xFF8BAD91),      // --green
    warning = Color(0xFFE0B66E),
    danger = Color(0xFFE06D6D),       // --red
    focusRing = Color(0xFFE18462),
    isDark = true
)

// ==========================================
// THEME 2: Midnight Indigo & Royal Violet
// ==========================================
val MidnightIndigoLightColors = AuraColors(
    canvas = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceSubtle = Color(0xFFF1F5F9),
    surfaceElevated = Color(0xFFFFFFFF),
    ink = Color(0xFF0F172A),
    inkSecondary = Color(0xFF475569),
    inkMuted = Color(0xFF94A3B8),
    border = Color(0xFFE2E8F0),
    accent = Color(0xFF6366F1),
    accentHover = Color(0xFF4F46E5),
    accentSubtle = Color(0xFFEEF2FF),
    success = Color(0xFF10B981),
    warning = Color(0xFFF59E0B),
    danger = Color(0xFFEF4444),
    focusRing = Color(0xFF818CF8),
    isDark = false
)

val MidnightIndigoDarkColors = AuraColors(
    canvas = Color(0xFF0B0F19),
    surface = Color(0xFF151C2C),
    surfaceSubtle = Color(0xFF1E283D),
    surfaceElevated = Color(0xFF26334D),
    ink = Color(0xFFF8FAFC),
    inkSecondary = Color(0xFF94A3B8),
    inkMuted = Color(0xFF64748B),
    border = Color(0xFF243048),
    accent = Color(0xFF818CF8),
    accentHover = Color(0xFFA5B4FC),
    accentSubtle = Color(0xFF1E1B4B),
    success = Color(0xFF34D399),
    warning = Color(0xFFFBBF24),
    danger = Color(0xFFF87171),
    focusRing = Color(0xFFA5B4FC),
    isDark = true
)

// ==========================================
// THEME 3: Cyber Cyan & Emerald Abyss
// ==========================================
val CyberCyanLightColors = AuraColors(
    canvas = Color(0xFFF0FDF4),
    surface = Color(0xFFFFFFFF),
    surfaceSubtle = Color(0xFFE6F7F0),
    surfaceElevated = Color(0xFFFFFFFF),
    ink = Color(0xFF062E25),
    inkSecondary = Color(0xFF2D6A5F),
    inkMuted = Color(0xFF5C9488),
    border = Color(0xFFD1FAE5),
    accent = Color(0xFF0D9488),
    accentHover = Color(0xFF0F766E),
    accentSubtle = Color(0xFFCCFBF1),
    success = Color(0xFF059669),
    warning = Color(0xFFD97706),
    danger = Color(0xFFDC2626),
    focusRing = Color(0xFF14B8A6),
    isDark = false
)

val CyberCyanDarkColors = AuraColors(
    canvas = Color(0xFF0A0F1D),
    surface = Color(0xFF111827),
    surfaceSubtle = Color(0xFF1B2538),
    surfaceElevated = Color(0xFF24324A),
    ink = Color(0xFFECFDF5),
    inkSecondary = Color(0xFF6EE7B7),
    inkMuted = Color(0xFF4B5563),
    border = Color(0xFF1F2937),
    accent = Color(0xFF06B6D4),
    accentHover = Color(0xFF22D3EE),
    accentSubtle = Color(0xFF083344),
    success = Color(0xFF10B981),
    warning = Color(0xFFFBBF24),
    danger = Color(0xFFEF4444),
    focusRing = Color(0xFF06B6D4),
    isDark = true
)

// Convenience getters for any theme preset & mode
fun getThemeColors(preset: ThemePreset, isDark: Boolean): AuraColors {
    return when (preset) {
        ThemePreset.CLAUDE_WARM -> if (isDark) ClaudeWarmDarkColors else ClaudeWarmLightColors
        ThemePreset.MIDNIGHT_INDIGO -> if (isDark) MidnightIndigoDarkColors else MidnightIndigoLightColors
        ThemePreset.CYBER_CYAN -> if (isDark) CyberCyanDarkColors else CyberCyanLightColors
    }
}

// Backwards-compatible defaults
val AuraLightColors = ClaudeWarmLightColors
val AuraDarkColors = ClaudeWarmDarkColors

val LocalAuraColors = staticCompositionLocalOf { ClaudeWarmLightColors }
