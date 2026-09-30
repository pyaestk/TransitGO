package com.bangkoktransit.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val TransitBlue = Color(0xFF005696)
val TransitGreen = Color(0xFF107C41)
val TransitCoral = Color(0xFF9C2444)
val TransitInk = Color(0xFF0F172A)
val TransitMuted = Color(0xFF64748B)
val TransitSurface = Color(0xFFF8FAFC)
val TransitPanel = Color(0xFFFFFFFF)
val TransitLine = Color(0xFFE2E8F0)
val TransitSoftBlue = Color(0xFFEAF3FA)
val TransitSoftGreen = Color(0xFFE9F5EE)
val TransitSoftCoral = Color(0xFFF8EDEF)

private val TransitLightColorScheme = lightColorScheme(
    primary = TransitBlue,
    onPrimary = Color.White,
    primaryContainer = TransitSoftBlue,
    onPrimaryContainer = TransitInk,
    secondary = TransitGreen,
    onSecondary = Color.White,
    secondaryContainer = TransitSoftGreen,
    onSecondaryContainer = Color(0xFF062314),
    tertiary = TransitCoral,
    onTertiary = Color.White,
    tertiaryContainer = TransitSoftCoral,
    onTertiaryContainer = TransitInk,
    background = TransitSurface,
    onBackground = TransitInk,
    surface = TransitPanel,
    onSurface = TransitInk,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TransitMuted,
    error = Color(0xFFB3261E),
    outline = TransitLine,
)

private val TransitDarkColorScheme = darkColorScheme(
    primary = Color(0xFF8FCBFF),
    onPrimary = Color(0xFF003353),
    primaryContainer = Color(0xFF004A75),
    onPrimaryContainer = Color(0xFFD0E8FF),
    secondary = Color(0xFF7BDFA8),
    onSecondary = Color(0xFF003921),
    secondaryContainer = Color(0xFF0B5132),
    onSecondaryContainer = Color(0xFF9DFCC3),
    tertiary = Color(0xFFFFB1C2),
    onTertiary = Color(0xFF65002A),
    tertiaryContainer = Color(0xFF85133E),
    onTertiaryContainer = Color(0xFFFFD9E1),
    background = Color(0xFF09111F),
    onBackground = Color(0xFFE7EDF7),
    surface = Color(0xFF111C2D),
    onSurface = Color(0xFFE7EDF7),
    surfaceVariant = Color(0xFF1B293B),
    onSurfaceVariant = Color(0xFFB7C2D2),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    outline = Color(0xFF3A4B62),
)

private val TransitTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontSize = 26.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontSize = 20.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
    ),
    titleSmall = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 23.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
    ),
    bodySmall = TextStyle(
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
    ),
)

@Composable
fun BangkoktransitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) TransitDarkColorScheme else TransitLightColorScheme,
        typography = TransitTypography,
        content = content,
    )
}
