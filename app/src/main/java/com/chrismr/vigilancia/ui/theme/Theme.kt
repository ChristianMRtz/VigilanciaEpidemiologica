package com.chrismr.vigilancia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary            = MedBlue40,
    onPrimary          = Color.White,
    primaryContainer   = MedBlue90,
    onPrimaryContainer = MedBlue10,

    secondary            = Teal40,
    onSecondary          = Color.White,
    secondaryContainer   = Teal90,
    onSecondaryContainer = Color(0xFF002020),

    tertiary            = Violet40,
    onTertiary          = Color.White,
    tertiaryContainer   = Color(0xFFEADDFF),
    onTertiaryContainer = Color(0xFF21005D),

    error            = Color(0xFFBA1A1A),
    onError          = Color.White,
    errorContainer   = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background         = NeutralBg,
    onBackground       = Color(0xFF191C20),
    surface            = Color.White,
    onSurface          = Color(0xFF191C20),
    surfaceVariant     = NeutralBg2,
    onSurfaceVariant   = Color(0xFF44474F),
    outline            = Color(0xFF74777F),
    outlineVariant     = Color(0xFFC4C7CF),
    inverseSurface     = Color(0xFF2E3135),
    inverseOnSurface   = Color(0xFFF0F0F7),
    inversePrimary     = MedBlue80,
)

private val DarkColorScheme = darkColorScheme(
    primary            = MedBlue80,
    onPrimary          = MedBlue20,
    primaryContainer   = Color(0xFF004A97),
    onPrimaryContainer = MedBlue90,

    secondary            = Teal80,
    onSecondary          = Color(0xFF003737),
    secondaryContainer   = Color(0xFF004F50),
    onSecondaryContainer = Teal90,

    tertiary            = Violet80,
    onTertiary          = Color(0xFF381E72),
    tertiaryContainer   = Color(0xFF4F378B),
    onTertiaryContainer = Color(0xFFEADDFF),

    error            = Color(0xFFFFB4AB),
    onError          = Color(0xFF690005),
    errorContainer   = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background       = Color(0xFF111318),
    onBackground     = Color(0xFFE2E2E9),
    surface          = Color(0xFF1A1C22),
    onSurface        = Color(0xFFE2E2E9),
    surfaceVariant   = Color(0xFF44474F),
    onSurfaceVariant = Color(0xFFC4C7CF),
    outline          = Color(0xFF8E9099),
    outlineVariant   = Color(0xFF44474F),
)

@Composable
fun VigilanciaEpidemiologicaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}