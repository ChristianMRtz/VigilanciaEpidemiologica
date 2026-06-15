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

    tertiary            = Gold40,
    onTertiary          = Color.White,
    tertiaryContainer   = Gold90,
    onTertiaryContainer = Color(0xFF3B2A10),

    error            = Color(0xFFBA1A1A),
    onError          = Color.White,
    errorContainer   = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background         = NeutralBg,
    onBackground       = Ink900,
    surface            = SandSurface,
    onSurface          = Ink900,
    surfaceVariant     = NeutralBg2,
    onSurfaceVariant   = Color(0xFF5E625E),
    outline            = Color(0xFF8B8377),
    outlineVariant     = Color(0xFFD8CFBF),
    inverseSurface     = Color(0xFF25323A),
    inverseOnSurface   = Color(0xFFF6F2EB),
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

    tertiary            = Gold90,
    onTertiary          = Color(0xFF402D12),
    tertiaryContainer   = Color(0xFF6D5122),
    onTertiaryContainer = Color(0xFFF9EACC),

    error            = Color(0xFFFFB4AB),
    onError          = Color(0xFF690005),
    errorContainer   = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background       = Color(0xFF12181D),
    onBackground     = Color(0xFFE8E2D8),
    surface          = Color(0xFF182127),
    onSurface        = Color(0xFFE8E2D8),
    surfaceVariant   = Color(0xFF313B41),
    onSurfaceVariant = Color(0xFFC5C0B6),
    outline          = Color(0xFF938C81),
    outlineVariant   = Color(0xFF444C50),
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
