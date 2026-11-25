package com.rejowan.pdfreaderpro.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * Contrast of the reader overlays that float over the PDF page.
 *
 * The page colour comes from the reading theme (Light / Sepia / Dark / Black), so the page
 * counter and scrubber sit on a translucent `inverseSurface` chip (alpha 0.9) with
 * `inverseOnSurface` text. The chip is composited over each reading-theme page colour here,
 * and the text must hold 4.5:1 in every app theme x reading theme combination.
 */
class ReaderChromeContrastTest {

    private class Scheme(
        val name: String,
        val inverseSurface: Color,
        val inverseOnSurface: Color,
        val inversePrimary: Color,
        val primary: Color,
        val onPrimary: Color
    )

    private val schemes = listOf(
        Scheme("Light", LightSurfaces.inverseSurface, LightSurfaces.inverseOnSurface, inversePrimaryLight, primaryLight, onPrimaryLight),
        Scheme("Dark", DarkSurfaces.inverseSurface, DarkSurfaces.inverseOnSurface, inversePrimaryDark, primaryDark, onPrimaryDark),
        Scheme("Black", BlackSurfaces.inverseSurface, BlackSurfaces.inverseOnSurface, inversePrimaryDark, primaryDark, onPrimaryDark)
    )

    /** Page background of each reading theme, as used by the PDF viewer. */
    private val pages = mapOf(
        "Light" to ReaderThemes.White.background,
        "Sepia" to ReaderThemes.Sepia.background,
        "Dark" to ReaderThemes.Dark.background,
        "Black" to ReaderThemes.Black.background
    )

    private val chipAlpha = 0.9f

    private fun channel(v: Int): Double {
        val c = v / 255.0
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(color: Color): Double {
        val argb = color.toArgb()
        return 0.2126 * channel((argb shr 16) and 0xFF) +
            0.7152 * channel((argb shr 8) and 0xFF) +
            0.0722 * channel(argb and 0xFF)
    }

    private fun ratio(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    @Test
    fun pageCounterTextHoldsContrastOnEveryReadingTheme() {
        val failures = mutableListOf<String>()
        schemes.forEach { s ->
            pages.forEach { (pageName, page) ->
                val chip = s.inverseSurface.copy(alpha = chipAlpha).compositeOver(page)
                val r = ratio(s.inverseOnSurface, chip)
                println("[reader] app=%-5s page=%-5s counter text/chip %5.2f".format(s.name, pageName, r))
                if (r < 4.5) failures += "${s.name}/$pageName counter text $r"
            }
        }
        assertTrue("Counter chip contrast failures: $failures", failures.isEmpty())
    }

    @Test
    fun scrubberProgressIsVisibleOnItsChip() {
        val failures = mutableListOf<String>()
        schemes.forEach { s ->
            pages.forEach { (pageName, page) ->
                val chip = s.inverseSurface.copy(alpha = chipAlpha).compositeOver(page)
                val r = ratio(s.inversePrimary, chip)
                println("[reader] app=%-5s page=%-5s progress/chip %5.2f".format(s.name, pageName, r))
                if (r < 3.0) failures += "${s.name}/$pageName progress $r"
            }
        }
        assertTrue("Scrubber progress contrast failures: $failures", failures.isEmpty())
    }

    @Test
    fun scrubberThumbNumberHoldsContrast() {
        val failures = mutableListOf<String>()
        schemes.forEach { s ->
            val r = ratio(s.onPrimary, s.primary)
            if (r < 4.5) failures += "${s.name} thumb number $r"
        }
        assertTrue("Scrubber thumb contrast failures: $failures", failures.isEmpty())
    }

    @Test
    fun readingThemeSwatchesHoldContrast() {
        val failures = mutableListOf<String>()
        mapOf(
            "Light" to ReaderSwatches.Light, "Dark" to ReaderSwatches.Dark,
            "Sepia" to ReaderSwatches.Sepia, "Black" to ReaderSwatches.Black
        ).forEach { (name, swatch) ->
            val r = ratio(swatch.text, swatch.page)
            println("[reader] swatch %-5s text/page %5.2f".format(name, r))
            if (r < 7.0) failures += "$name swatch $r"
        }
        assertTrue("Swatch contrast failures: $failures", failures.isEmpty())
    }

    /** Slider ticks use onColor on the active track and color on the faint inactive track. */
    @Test
    fun sliderTicksAreVisibleOnBothTracks() {
        val failures = mutableListOf<String>()
        listOf(
            "Light" to LightAccentColors, "Dark" to DarkAccentColors, "Black" to BlackAccentColors
        ).forEach { (name, palette) ->
            val surface = when (name) {
                "Light" -> LightSurfaces.surfaceContainerHigh
                "Dark" -> DarkSurfaces.surfaceContainerHigh
                else -> BlackSurfaces.surfaceContainerHigh
            }
            listOf(
                "teal" to palette.teal, "blue" to palette.blue, "amber" to palette.amber, "purple" to palette.purple
            ).forEach { (tone, t) ->
                val activeTick = ratio(t.onColor, t.color)
                val inactiveTrack = t.color.copy(alpha = 0.32f).compositeOver(surface)
                val inactiveTick = ratio(t.color, inactiveTrack)
                println("[reader] %-5s %-6s activeTick %5.2f inactiveTick %5.2f".format(name, tone, activeTick, inactiveTick))
                if (activeTick < 3.0) failures += "$name/$tone active tick $activeTick"
                if (inactiveTick < 3.0) failures += "$name/$tone inactive tick $inactiveTick"
            }
        }
        assertTrue("Slider tick contrast failures: $failures", failures.isEmpty())
    }
}
