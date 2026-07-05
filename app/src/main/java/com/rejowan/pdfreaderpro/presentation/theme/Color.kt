package com.rejowan.pdfreaderpro.presentation.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// DARK THEME - SoftCloud blue/purple brand palette
// ============================================================================

val primaryDark = Color(0xFFA78BFA)
val onPrimaryDark = Color(0xFF1E1B4B)
val primaryContainerDark = Color(0xFF5B21B6)
val onPrimaryContainerDark = Color(0xFFEDE9FE)

val secondaryDark = Color(0xFF60A5FA)
val onSecondaryDark = Color(0xFF1E3A8A)
val secondaryContainerDark = Color(0xFF1D4ED8)
val onSecondaryContainerDark = Color(0xFFDBEAFE)

val tertiaryDark = Color(0xFF4ADE80)
val onTertiaryDark = Color(0xFF14532D)
val tertiaryContainerDark = Color(0xFF16A34A)
val onTertiaryContainerDark = Color(0xFFDCFCE7)

val inversePrimaryDark = Color(0xFF7C3AED)

// ============================================================================
// SHARED DARK SURFACE COLORS
// ============================================================================

object DarkSurfaces {
    val background = Color(0xFF0F172A)
    val onBackground = Color(0xFFE2E8F0)
    val surface = Color(0xFF0F172A)
    val onSurface = Color(0xFFE2E8F0)
    val surfaceVariant = Color(0xFF334155)
    val onSurfaceVariant = Color(0xFFCBD5E1)
    val outline = Color(0xFF64748B)
    val outlineVariant = Color(0xFF475569)
    val scrim = Color(0xFF000000)
    val inverseSurface = Color(0xFFE2E8F0)
    val inverseOnSurface = Color(0xFF1E293B)
    val surfaceDim = Color(0xFF0F172A)
    val surfaceBright = Color(0xFF334155)
    val surfaceContainerLowest = Color(0xFF0B1120)
    val surfaceContainerLow = Color(0xFF111827)
    val surfaceContainer = Color(0xFF1E293B)
    val surfaceContainerHigh = Color(0xFF273449)
    val surfaceContainerHighest = Color(0xFF334155)
    val error = Color(0xFFD32F2F)
    val onError = Color(0xFFFFFFFF)
    val errorContainer = Color(0xFFEF5350)
    val onErrorContainer = Color(0xFFFFFFFF)
}

// ============================================================================
// SHARED BLACK (AMOLED) SURFACE COLORS
// ============================================================================

object BlackSurfaces {
    val background = Color(0xFF000000)
    val onBackground = Color(0xFFE8E5F0)
    val surface = Color(0xFF000000)
    val onSurface = Color(0xFFE8E5F0)
    val surfaceVariant = Color(0xFF1A191F)
    val onSurfaceVariant = Color(0xFFC9C5D4)
    val outline = Color(0xFF8C899A)
    val outlineVariant = Color(0xFF2A282F)
    val scrim = Color(0xFF000000)
    val inverseSurface = Color(0xFFE8E5F0)
    val inverseOnSurface = Color(0xFF1C1B1F)
    val surfaceDim = Color(0xFF000000)
    val surfaceBright = Color(0xFF1A191F)
    val surfaceContainerLowest = Color(0xFF000000)
    val surfaceContainerLow = Color(0xFF0A090D)
    val surfaceContainer = Color(0xFF101014)
    val surfaceContainerHigh = Color(0xFF17161B)
    val surfaceContainerHighest = Color(0xFF1D1C22)
    val error = Color(0xFFD32F2F)
    val onError = Color(0xFFFFFFFF)
    val errorContainer = Color(0xFFEF5350)
    val onErrorContainer = Color(0xFFFFFFFF)
}

// ============================================================================
// SHARED LIGHT SURFACE COLORS
// ============================================================================

object LightSurfaces {
    val background = Color(0xFFFFFBFE)
    val onBackground = Color(0xFF1C1B1F)
    val surface = Color(0xFFFFFBFE)
    val onSurface = Color(0xFF1C1B1F)
    val surfaceVariant = Color(0xFFE7E0EC)
    val onSurfaceVariant = Color(0xFF49454F)
    val outline = Color(0xFF79747E)
    val outlineVariant = Color(0xFFCAC4D0)
    val scrim = Color(0xFF000000)
    val inverseSurface = Color(0xFF313033)
    val inverseOnSurface = Color(0xFFF4EFF4)
    val surfaceDim = Color(0xFFDED8E1)
    val surfaceBright = Color(0xFFFFFBFE)
    val surfaceContainerLowest = Color(0xFFFFFFFF)
    val surfaceContainerLow = Color(0xFFF7F2FA)
    val surfaceContainer = Color(0xFFF3EDF7)
    val surfaceContainerHigh = Color(0xFFECE6F0)
    val surfaceContainerHighest = Color(0xFFE6E0E9)
    val error = Color(0xFFBA1A1A)
    val onError = Color(0xFFFFFFFF)
    val errorContainer = Color(0xFFFFDAD6)
    val onErrorContainer = Color(0xFF410002)
}

// ============================================================================
// LIGHT THEME - SoftCloud blue/purple brand palette
// ============================================================================

val primaryLight = Color(0xFF7C3AED)
val onPrimaryLight = Color(0xFFFFFFFF)
val primaryContainerLight = Color(0xFFEDE9FE)
val onPrimaryContainerLight = Color(0xFF4C1D95)

val secondaryLight = Color(0xFF3B82F6)
val onSecondaryLight = Color(0xFFFFFFFF)
val secondaryContainerLight = Color(0xFFDBEAFE)
val onSecondaryContainerLight = Color(0xFF1E3A8A)

val tertiaryLight = Color(0xFF22C55E)
val onTertiaryLight = Color(0xFFFFFFFF)
val tertiaryContainerLight = Color(0xFFDCFCE7)
val onTertiaryContainerLight = Color(0xFF14532D)

val inversePrimaryLight = Color(0xFFA78BFA)

// ============================================================================
// PDF READER THEMES
// ============================================================================

/**
 * Reader themes for PDF viewing with different background/text combinations.
 */
data class ReaderColors(
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color
)

object ReaderThemes {
    val White = ReaderColors(
        background = Color.White,
        onBackground = Color.Black,
        surface = Color(0xFFF5F5F5),
        onSurface = Color.Black
    )

    val Sepia = ReaderColors(
        background = Color(0xFFF5E6D3),
        onBackground = Color(0xFF5B4636),
        surface = Color(0xFFECDCC8),
        onSurface = Color(0xFF5B4636)
    )

    val Dark = ReaderColors(
        background = Color(0xFF1E1E1E),
        onBackground = Color(0xFFE0E0E0),
        surface = Color(0xFF2A2A2A),
        onSurface = Color(0xFFE0E0E0)
    )

    val Black = ReaderColors(
        background = Color.Black,
        onBackground = Color.White,
        surface = Color(0xFF121212),
        onSurface = Color.White
    )
}

/**
 * Enum for reader theme selection
 */
enum class ReaderTheme {
    WHITE, SEPIA, DARK, BLACK;

    fun toColors(): ReaderColors = when (this) {
        WHITE -> ReaderThemes.White
        SEPIA -> ReaderThemes.Sepia
        DARK -> ReaderThemes.Dark
        BLACK -> ReaderThemes.Black
    }
}
