package com.bangkoktransit.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val TransitBlue = Color(0xFF365F91)
val TransitGreen = Color(0xFF1F806A)
val TransitCoral = Color(0xFFC65C50)
val TransitInk = Color(0xFF14211F)
val TransitMuted = Color(0xFF68736F)
val TransitSurface = Color(0xFFF5F7F4)
val TransitPanel = Color(0xFFFFFFFF)
val TransitLine = Color(0xFFE1E7E2)
val TransitSoftBlue = Color(0xFFEAF1F8)
val TransitSoftGreen = Color(0xFFE7F3EF)
val TransitSoftCoral = Color(0xFFF8ECEA)

private val TransitColorScheme = lightColorScheme(
    primary = TransitBlue,
    onPrimary = Color.White,
    secondary = TransitGreen,
    onSecondary = Color(0xFF062314),
    tertiary = TransitCoral,
    background = TransitSurface,
    onBackground = TransitInk,
    surface = TransitPanel,
    onSurface = TransitInk,
    surfaceVariant = Color(0xFFE8EDEA),
    onSurfaceVariant = TransitMuted,
    error = Color(0xFFB3261E),
    outline = TransitLine,
)

private val TransitTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 34.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontSize = 26.sp,
        lineHeight = 31.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontSize = 22.sp,
        lineHeight = 27.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontSize = 20.sp,
        lineHeight = 25.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 23.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontSize = 13.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
    ),
)

@Composable
fun BangkoktransitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TransitColorScheme,
        typography = TransitTypography,
        content = content,
    )
}
