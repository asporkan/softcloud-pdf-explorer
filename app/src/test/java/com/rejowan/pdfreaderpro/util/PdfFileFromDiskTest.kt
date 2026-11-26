package com.rejowan.pdfreaderpro.util

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File

class PdfFileFromDiskTest {

    @Before
    fun setUp() {
        mockkStatic(android.net.Uri::class)
        every { android.net.Uri.fromFile(any()) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkStatic(android.net.Uri::class)
    }

    @Test
    fun `missing file returns null`() {
        assertNull(pdfFileFromDisk("/definitely/not/here/qa.pdf"))
    }

    @Test
    fun `existing file is mapped with name size and folder`() {
        val tmp = File.createTempFile("qa_recent", ".pdf").apply {
            writeBytes(ByteArray(1234))
            deleteOnExit()
        }
        val pdf = pdfFileFromDisk(tmp.absolutePath)!!
        assertEquals(tmp.name, pdf.name)
        assertEquals(1234L, pdf.size)
        assertEquals(tmp.parent, pdf.parentFolder)
        assertEquals(tmp.lastModified(), pdf.dateModified)
    }
}
