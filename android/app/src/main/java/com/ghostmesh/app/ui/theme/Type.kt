package com.ghostmesh.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ghostmesh.app.R

val PlayfairFontFamily = FontFamily(
    Font(R.font.playfair_display_500, FontWeight.Normal),
    Font(R.font.playfair_display_500, FontWeight.Medium),
    Font(R.font.playfair_display_600, FontWeight.SemiBold),
    Font(R.font.playfair_display_600, FontWeight.Bold),
    Font(R.font.playfair_display_500_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.playfair_display_500_italic, FontWeight.Medium, FontStyle.Italic),
    Font(R.font.playfair_display_600_italic, FontWeight.SemiBold, FontStyle.Italic),
    Font(R.font.playfair_display_600_italic, FontWeight.Bold, FontStyle.Italic)
)

val DmSansFontFamily = FontFamily(
    Font(R.font.dm_sans_400, FontWeight.Normal),
    Font(R.font.dm_sans_500, FontWeight.Medium),
    Font(R.font.dm_sans_600, FontWeight.SemiBold),
    Font(R.font.dm_sans_700, FontWeight.Bold)
)

val DmMonoFontFamily = FontFamily(
    Font(R.font.dm_mono_400, FontWeight.Normal),
    Font(R.font.dm_mono_500, FontWeight.Medium),
    Font(R.font.dm_mono_500, FontWeight.SemiBold)
)

val AuraTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = PlayfairFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.03).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = PlayfairFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.03).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PlayfairFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.02).sp
    ),
    titleLarge = TextStyle(
        fontFamily = PlayfairFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.01).sp
    ),
    titleMedium = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    bodySmall = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = DmMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.06.sp
    ),
    labelSmall = TextStyle(
        fontFamily = DmMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 9.5.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.08.sp
    )
)
