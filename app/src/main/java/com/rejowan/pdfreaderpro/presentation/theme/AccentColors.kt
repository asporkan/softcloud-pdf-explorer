package com.rejowan.pdfreaderpro.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * One semantic accent (purple, red, ...) resolved for the active theme.
 *
 * Use the three fields together so contrast holds in Light, Dark and Black:
 *  - [container]   opaque tinted fill (icon boxes, badges, selected chips, error boxes)
 *  - [onContainer] text/icons placed on [container] (>= 7:1 on the container)
 *  - [color]       the accent itself for icons, text and strokes that sit on a
 *                  surface OR on [container] (>= 4.5:1 on both)
 *  - [onColor]     content drawn on a solid fill of [color] (filled buttons, solid
 *                  chips): white in Light, the dark container tone in Dark/Black
 *
 * Do not draw `color.copy(alpha = 0.1f)` as a background with `color` on top of it;
 * that is exactly what produced low-contrast purple-on-purple in the dark theme.
 */
@Immutable
class AccentTone(
    val color: Color,
    val container: Color,
    val onContainer: Color,
    val onColor: Color
)

/** Semantic accent palette. Access it through `MaterialTheme.accents`. */
@Immutable
class AccentColors(
    val red: AccentTone,
    val orange: AccentTone,
    val amber: AccentTone,
    val green: AccentTone,
    val teal: AccentTone,
    val cyan: AccentTone,
    val blue: AccentTone,
    val indigo: AccentTone,
    val purple: AccentTone,
    val pink: AccentTone,
    val gray: AccentTone
)

private fun lightTone(color: Long, container: Long, onContainer: Long) =
    AccentTone(Color(color), Color(container), Color(onContainer), Color.White)

private fun darkTone(color: Long, container: Long, onContainer: Long) =
    AccentTone(Color(color), Color(container), Color(onContainer), Color(container))

val LightAccentColors = AccentColors(
    red = lightTone(0xFFB3261E, 0xFFFDE3E0, 0xFF8C1D18),
    orange = lightTone(0xFFA64300, 0xFFFFE6D6, 0xFF7A3300),
    amber = lightTone(0xFF8F5C00, 0xFFFFEFCC, 0xFF5E3C00),
    green = lightTone(0xFF27702B, 0xFFDDF3DE, 0xFF14501A),
    teal = lightTone(0xFF00705F, 0xFFD3F1EC, 0xFF00504A),
    cyan = lightTone(0xFF006E80, 0xFFD0F1F7, 0xFF004D5A),
    blue = lightTone(0xFF1565C0, 0xFFDCEAFE, 0xFF0D47A1),
    indigo = lightTone(0xFF4F46C8, 0xFFE3E5FF, 0xFF2C2A8F),
    purple = lightTone(0xFF6D4BB8, 0xFFEDE4FF, 0xFF4A2A8C),
    pink = lightTone(0xFFB0245F, 0xFFFFDDE9, 0xFF7D1342),
    gray = lightTone(0xFF4B636E, 0xFFE8EDF0, 0xFF37474F)
)

val DarkAccentColors = AccentColors(
    red = darkTone(0xFFFFB4AB, 0xFF5C1D1D, 0xFFFFDAD6),
    orange = darkTone(0xFFFFB59A, 0xFF5E2A12, 0xFFFFDBCB),
    amber = darkTone(0xFFFFC866, 0xFF56400A, 0xFFFFE9B8),
    green = darkTone(0xFF86D98B, 0xFF1F4A24, 0xFFC8F2CB),
    teal = darkTone(0xFF6FD8C8, 0xFF0F4A44, 0xFFBDF2EA),
    cyan = darkTone(0xFF6CD6EA, 0xFF0A4653, 0xFFBDEFFA),
    blue = darkTone(0xFF9CC8FF, 0xFF1C3F6E, 0xFFD6E7FF),
    indigo = darkTone(0xFFB6B9FF, 0xFF2E306F, 0xFFE0E1FF),
    purple = darkTone(0xFFD0BCFF, 0xFF3B2A6B, 0xFFE9DDFF),
    pink = darkTone(0xFFFFB0CF, 0xFF5E1D3C, 0xFFFFD9E7),
    gray = darkTone(0xFFB0BEC5, 0xFF37424A, 0xFFDDE5E9)
)

/** Same accent tones as dark, with deeper containers so they sit on true black. */
val BlackAccentColors = AccentColors(
    red = darkTone(0xFFFFB4AB, 0xFF4A1414, 0xFFFFDAD6),
    orange = darkTone(0xFFFFB59A, 0xFF4D210D, 0xFFFFDBCB),
    amber = darkTone(0xFFFFC866, 0xFF453308, 0xFFFFE9B8),
    green = darkTone(0xFF86D98B, 0xFF18391C, 0xFFC8F2CB),
    teal = darkTone(0xFF6FD8C8, 0xFF0B3B36, 0xFFBDF2EA),
    cyan = darkTone(0xFF6CD6EA, 0xFF083944, 0xFFBDEFFA),
    blue = darkTone(0xFF9CC8FF, 0xFF163359, 0xFFD6E7FF),
    indigo = darkTone(0xFFB6B9FF, 0xFF25275C, 0xFFE0E1FF),
    purple = darkTone(0xFFD0BCFF, 0xFF30225A, 0xFFE9DDFF),
    pink = darkTone(0xFFFFB0CF, 0xFF4C1730, 0xFFFFD9E7),
    gray = darkTone(0xFFB0BEC5, 0xFF2B343A, 0xFFDDE5E9)
)

val LocalAccentColors = staticCompositionLocalOf { DarkAccentColors }

/** Theme-aware semantic accents: `MaterialTheme.accents.purple.container` etc. */
val MaterialTheme.accents: AccentColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAccentColors.current
