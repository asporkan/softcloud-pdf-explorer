package com.rejowan.pdfreaderpro.util

import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class CrashReportTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun store() = CrashReportStore(File(tmp.root, "sub/${CrashReportStore.FILE_NAME}"))

    private val sensitiveMessage = "Cannot open /storage/emulated/0/Download/Tax Return 2025.pdf"

    private fun crash(): Throwable =
        IllegalStateException(sensitiveMessage, IOException("C:\\Users\\bob\\secret.pdf"))

    // --- format ---

    @Test
    fun format_containsClassNamesTimeAndThread() {
        val report = CrashReportFormatter.format(crash(), "main", 0L)

        assertTrue(report.contains("time: 1970-01-01 00:00:00 UTC"))
        assertTrue(report.contains("thread: main"))
        assertTrue(report.contains("exception: java.lang.IllegalStateException"))
        assertTrue(report.contains("cause: java.io.IOException"))
        assertTrue(report.contains("stack:"))
        assertTrue(report.contains("CrashReportTest"))
    }

    @Test
    fun format_neverLeaksMessagePathsOrPdfNames() {
        val report = CrashReportFormatter.format(crash(), "main", 0L)

        assertFalse(report.contains("Tax Return"))
        // the package name contains ".pdf", so match only a file extension
        assertFalse(Regex("""\.pdf\b""").containsMatchIn(report))
        assertFalse(report.contains("/storage"))
        assertFalse(report.contains("secret"))
        assertFalse(report.contains("C:\\Users"))
    }

    @Test
    fun format_truncatesStackAndLength() {
        fun deep(n: Int): Nothing = if (n == 0) throw RuntimeException("x") else deep(n - 1)
        val t = runCatching { deep(40) }.exceptionOrNull()!!

        val report = CrashReportFormatter.format(t, "worker", 0L)

        assertEquals(CrashReportFormatter.MAX_FRAMES, report.lines().count { it.trimStart().startsWith("at ") })
        assertTrue(report.length <= 2000)
    }

    @Test
    fun format_survivesCauseCycle() {
        val a = RuntimeException("a")
        val b = RuntimeException("b", a)
        a.initCause(b)

        val report = CrashReportFormatter.format(a, null, 0L)

        assertTrue(report.contains("thread: ?"))
    }

    // --- store ---

    @Test
    fun store_saveReadClear() {
        val s = store()
        assertNull(s.read())

        assertTrue(s.save("hello"))
        assertEquals("hello", s.read())
        // read does not consume the record; only clear does
        assertEquals("hello", s.read())

        s.clear()
        assertNull(s.read())
    }

    @Test
    fun store_saveOverwritesPreviousRecord() {
        val s = store()
        s.save("first")
        s.save("second")
        assertEquals("second", s.read())
    }

    @Test
    fun store_saveFailureReturnsFalseAndDoesNotThrow() {
        val blocker = tmp.newFile("blocker")
        val s = CrashReportStore(File(blocker, "cannot/${CrashReportStore.FILE_NAME}"))
        assertFalse(s.save("x"))
        assertNull(s.read())
    }

    // --- handler ---

    @Test
    fun handler_writesRecordThenDelegatesToPreviousHandler() {
        val s = store()
        val previous = mockk<Thread.UncaughtExceptionHandler>(relaxed = true)
        var terminated = false
        val handler = CrashRecordingHandler(s, previous, { terminated = true }, { 0L })
        val thread = Thread("main-test")
        val error = crash()

        handler.uncaughtException(thread, error)

        val saved = s.read()
        assertNotNull(saved)
        assertTrue(saved!!.contains("thread: main-test"))
        assertFalse(saved.contains("Tax Return"))
        verify(exactly = 1) { previous.uncaughtException(thread, error) }
        assertFalse(terminated)
    }

    @Test
    fun handler_withoutPreviousHandlerTerminatesProcess() {
        val s = store()
        var terminated = false
        val handler = CrashRecordingHandler(s, null, { terminated = true })

        handler.uncaughtException(Thread.currentThread(), crash())

        assertTrue(terminated)
        assertNotNull(s.read())
    }

    @Test
    fun handler_storeFailureStillDelegatesToPreviousHandler() {
        val blocker = tmp.newFile("blocker2")
        val brokenStore = CrashReportStore(File(blocker, "x/${CrashReportStore.FILE_NAME}"))
        val previous = mockk<Thread.UncaughtExceptionHandler>(relaxed = true)
        val handler = CrashRecordingHandler(brokenStore, previous, {})
        val error = crash()

        handler.uncaughtException(Thread.currentThread(), error)

        verify(exactly = 1) { previous.uncaughtException(any(), error) }
    }

    @Test
    fun nextLaunch_readsRecordThenClearRemovesIt() {
        val s = store()
        CrashRecordingHandler(s, null, {}, { 0L }).uncaughtException(Thread.currentThread(), crash())

        // "next launch": a new store instance over the same file
        val nextLaunch = CrashReportStore(File(tmp.root, "sub/${CrashReportStore.FILE_NAME}"))
        assertNotNull(nextLaunch.read())
        nextLaunch.clear()
        assertNull(nextLaunch.read())
    }
}
