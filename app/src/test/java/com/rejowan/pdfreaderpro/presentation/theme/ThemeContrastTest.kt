package com.rejowan.pdfreaderpro.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * WCAG 2.x contrast checks for the app theme: accent tokens (AccentColors) and the Material
 * roles that screens put text and icons on. Text needs >= 4.5:1, accent containers hold
 * text at >= 7:1, and containers must be distinguishable from the surface (>= 1.1:1).
 */
class ThemeContrastTest {

    private class Surfaces(val name: String, val surface: Color, val low: Color, val mid: Color, val high: Color)

    private val light = Surfaces(
        "Light", LightSurfaces.surface, LightSurfaces.surfaceContainerLow,
        LightSurfaces.surfaceContainer, LightSurfaces.surfaceContainerHigh
    )
    private val dark = Surfaces(
        "Dark", DarkSurfaces.surface, DarkSurfaces.surfaceContainerLow,
        DarkSurfaces.surfaceContainer, DarkSurfaces.surfaceContainerHigh
    )
    private val black = Surfaces(
        "Black", BlackSurfaces.surface, BlackSurfaces.surfaceContainerLow,
        BlackSurfaces.surfaceContainer, BlackSurfaces.surfaceContainerHigh
    )

    private fun channel(v: Int): Double {
        val c = v / 255.0
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(color: Color): Double {
        val argb = color.toArgb()
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)
    }

    private fun ratio(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        val hi = maxOf(la, lb)
        val lo = minOf(la, lb)
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun accents(colors: AccentColors): Map<String, AccentTone> = mapOf(
        "red" to colors.red, "orange" to colors.orange, "amber" to colors.amber,
        "green" to colors.green, "teal" to colors.teal, "cyan" to colors.cyan,
        "blue" to colors.blue, "indigo" to colors.indigo, "purple" to colors.purple,
        "pink" to colors.pink, "gray" to colors.gray
    )

    private fun checkAccents(surfaces: Surfaces, palette: AccentColors) {
        val failures = mutableListOf<String>()
        accents(palette).forEach { (name, tone) ->
            val onSurface = listOf(surfaces.surface, surfaces.low, surfaces.mid, surfaces.high)
                .minOf { ratio(tone.color, it) }
            val onOwnContainer = ratio(tone.color, tone.container)
            val contentOnContainer = ratio(tone.onContainer, tone.container)
            val onSolid = ratio(tone.onColor, tone.color)
            val containerVisible = ratio(tone.container, surfaces.surface)
            println(
                "[contrast] %-5s %-6s color/surface(min) %5.2f  color/container %5.2f  onContainer/container %5.2f  onColor/color %5.2f  container/surface %4.2f".format(
                    surfaces.name, name, onSurface, onOwnContainer, contentOnContainer, onSolid, containerVisible
                )
            )
            if (onSurface < 4.5) failures += "$name color on surface $onSurface"
            if (onOwnContainer < 4.5) failures += "$name color on container $onOwnContainer"
            if (contentOnContainer < 7.0) failures += "$name onContainer on container $contentOnContainer"
            if (onSolid < 4.5) failures += "$name onColor on color $onSolid"
            if (containerVisible < 1.1) failures += "$name container vs surface $containerVisible"
        }
        assertTrue("${surfaces.name} accent contrast failures: $failures", failures.isEmpty())
    }

    @Test
    fun lightAccentsMeetContrast() = checkAccents(light, LightAccentColors)

    @Test
    fun darkAccentsMeetContrast() = checkAccents(dark, DarkAccentColors)

    @Test
    fun blackAccentsMeetContrast() = checkAccents(black, BlackAccentColors)

    private fun check(failures: MutableList<String>, theme: String, label: String, fg: Color, bg: Color, min: Double = 4.5) {
        val r = ratio(fg, bg)
        println("[contrast] %-5s %-40s %5.2f".format(theme, label, r))
        if (r < min) failures += "$theme $label = $r (< $min)"
    }

    private fun checkScheme(
        s: Surfaces,
        primary: Color, onPrimary: Color, primaryContainer: Color, onPrimaryContainer: Color,
        secondary: Color, onSecondary: Color, secondaryContainer: Color, onSecondaryContainer: Color,
        tertiary: Color, onTertiary: Color, tertiaryContainer: Color, onTertiaryContainer: Color,
        error: Color, onError: Color, errorContainer: Color, onErrorContainer: Color,
        onSurface: Color, onSurfaceVariant: Color, surfaceVariant: Color
    ) {
        val f = mutableListOf<String>()
        val n = s.name
        check(f, n, "onPrimary/primary", onPrimary, primary)
        check(f, n, "onPrimaryContainer/primaryContainer", onPrimaryContainer, primaryContainer)
        check(f, n, "primary/surface (text)", primary, s.surface)
        check(f, n, "onSecondary/secondary", onSecondary, secondary)
        check(f, n, "onSecondaryContainer/secondaryContainer", onSecondaryContainer, secondaryContainer)
        check(f, n, "onTertiary/tertiary", onTertiary, tertiary)
        check(f, n, "onTertiaryContainer/tertiaryContainer", onTertiaryContainer, tertiaryContainer)
        check(f, n, "onError/error", onError, error)
        check(f, n, "onErrorContainer/errorContainer", onErrorContainer, errorContainer)
        check(f, n, "error/surface (text)", error, s.surface)
        check(f, n, "onSurface/surface", onSurface, s.surface)
        check(f, n, "onSurface/surfaceContainerHigh", onSurface, s.high)
        check(f, n, "onSurfaceVariant/surface", onSurfaceVariant, s.surface)
        check(f, n, "onSurfaceVariant/surfaceVariant", onSurfaceVariant, surfaceVariant)
        assertTrue("$n scheme contrast failures: $f", f.isEmpty())
    }

    @Test
    fun lightSchemeMeetsContrast() = checkScheme(
        light,
        primaryLight, onPrimaryLight, primaryContainerLight, onPrimaryContainerLight,
        secondaryLight, onSecondaryLight, secondaryContainerLight, onSecondaryContainerLight,
        tertiaryLight, onTertiaryLight, tertiaryContainerLight, onTertiaryContainerLight,
        LightSurfaces.error, LightSurfaces.onError, LightSurfaces.errorContainer, LightSurfaces.onErrorContainer,
        LightSurfaces.onSurface, LightSurfaces.onSurfaceVariant, LightSurfaces.surfaceVariant
    )

    @Test
    fun darkSchemeMeetsContrast() = checkScheme(
        dark,
        primaryDark, onPrimaryDark, primaryContainerDark, onPrimaryContainerDark,
        secondaryDark, onSecondaryDark, secondaryContainerDark, onSecondaryContainerDark,
        tertiaryDark, onTertiaryDark, tertiaryContainerDark, onTertiaryContainerDark,
        DarkSurfaces.error, DarkSurfaces.onError, DarkSurfaces.errorContainer, DarkSurfaces.onErrorContainer,
        DarkSurfaces.onSurface, DarkSurfaces.onSurfaceVariant, DarkSurfaces.surfaceVariant
    )

    @Test
    fun blackSchemeMeetsContrast() = checkScheme(
        black,
        primaryDark, onPrimaryDark, primaryContainerDark, onPrimaryContainerDark,
        secondaryDark, onSecondaryDark, secondaryContainerDark, onSecondaryContainerDark,
        tertiaryDark, onTertiaryDark, tertiaryContainerDark, onTertiaryContainerDark,
        BlackSurfaces.error, BlackSurfaces.onError, BlackSurfaces.errorContainer, BlackSurfaces.onErrorContainer,
        BlackSurfaces.onSurface, BlackSurfaces.onSurfaceVariant, BlackSurfaces.surfaceVariant
    )

    @Test
    fun pdfOutputSwatchCheckMarkIsReadable() {
        val all = PdfOutputColors.PageNumberPresets + PdfOutputColors.WatermarkPresets
        all.forEach { swatch ->
            assertTrue("check mark on $swatch", ratio(PdfOutputColors.contentOn(swatch), swatch) >= 3.0)
        }
    }
}
