package com.chrismr.vigilancia.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Roboto (SansSerif del sistema) con escala tipográfica completa Material 3
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.Bold,
        fontSize      = 57.sp,
        lineHeight    = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineLarge = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.Bold,
        fontSize      = 34.sp,
        lineHeight    = 42.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 30.sp,
        lineHeight    = 38.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 26.sp,
        lineHeight    = 34.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 22.sp,
        lineHeight    = 30.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 19.sp,
        lineHeight    = 26.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.Medium,
        fontSize      = 17.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.Normal,
        fontSize      = 18.sp,
        lineHeight    = 26.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.Normal,
        fontSize      = 16.sp,
        lineHeight    = 23.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.Normal,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.Medium,
        fontSize      = 16.sp,
        lineHeight    = 22.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.Medium,
        fontSize      = 15.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.Medium,
        fontSize      = 13.sp,
        lineHeight    = 18.sp,
        letterSpacing = 0.5.sp
    ),
)