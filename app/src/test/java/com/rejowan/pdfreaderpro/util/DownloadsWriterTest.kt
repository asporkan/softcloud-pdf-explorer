package com.rejowan.pdfreaderpro.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadsWriterTest {

    @Test
    fun `plain names are kept`() {
        assertEquals("report.pdf", DownloadsWriter.sanitizeFileName("report.pdf"))
    }

    @Test
    fun `directory parts and traversal are stripped`() {
        assertEquals("evil.txt", DownloadsWriter.sanitizeFileName("../../etc/evil.txt"))
        assertEquals("evil.txt", DownloadsWriter.sanitizeFileName("..\\..\\evil.txt"))
        assertEquals("a.txt", DownloadsWriter.sanitizeFileName("/sdcard/Download/a.txt"))
    }

    @Test
    fun `reserved and control characters become underscores`() {
        assertEquals("a_b_c.txt", DownloadsWriter.sanitizeFileName("a:b*c.txt"))
        assertEquals("a_b.txt", DownloadsWriter.sanitizeFileName("a\u0000b.txt"))
    }

    @Test
    fun `leading dots are removed so hidden or relative names cannot be created`() {
        assertEquals("hidden", DownloadsWriter.sanitizeFileName(".hidden"))
        assertEquals("attachment", DownloadsWriter.sanitizeFileName(".."))
    }

    @Test
    fun `blank or missing names fall back to attachment`() {
        assertEquals("attachment", DownloadsWriter.sanitizeFileName(null))
        assertEquals("attachment", DownloadsWriter.sanitizeFileName("   "))
        assertEquals("attachment", DownloadsWriter.sanitizeFileName("folder/"))
    }

    @Test
    fun `declared mime type wins`() {
        assertEquals("text/plain", DownloadsWriter.resolveMimeType("whatever.bin", "text/plain"))
    }

    @Test
    fun `free name is returned unchanged`() {
        assertEquals("a.txt", DownloadsWriter.uniqueName("a.txt") { false })
    }

    @Test
    fun `taken names get an incrementing suffix before the extension`() {
        val taken = setOf("a.txt", "a (1).txt")
        assertEquals("a (2).txt", DownloadsWriter.uniqueName("a.txt") { it in taken })
    }

    @Test
    fun `names without extension get the suffix at the end`() {
        assertEquals("notes (1)", DownloadsWriter.uniqueName("notes") { it == "notes" })
    }
}
