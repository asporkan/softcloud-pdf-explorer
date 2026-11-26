package com.rejowan.pdfreaderpro.data.pdf

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class UnicodePdfFontTest {

    @Test
    fun `falls back to Helvetica when no candidate font exists`() {
        val font = UnicodePdfFont.create(listOf("/nope/missing.ttf"))
        val name = font.fontProgram.fontNames.fontName
        assertTrue("expected Helvetica, got $name", name.contains("Helvetica"))
        // The fallback is exactly what dropped Turkish letters before.
        assertFalse(font.containsGlyph('İ'.code))
    }

    @Test
    fun `unicode system font keeps Turkish letters`() {
        // Any Unicode TrueType font available on the build machine stands in for the Android one.
        val candidate = listOf(
            "C:/Windows/Fonts/arial.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/System/Library/Fonts/Supplemental/Arial.ttf"
        ).firstOrNull { File(it).isFile }
        assumeTrue("no Unicode test font on this machine", candidate != null)

        val font = UnicodePdfFont.create(listOf(candidate!!))
        "İŞĞÜÖÇışğüöç".forEach { ch ->
            assertTrue("missing glyph for '$ch'", font.containsGlyph(ch.code))
        }
    }
}
