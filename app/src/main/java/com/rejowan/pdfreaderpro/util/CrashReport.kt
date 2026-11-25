package com.rejowan.pdfreaderpro.util

import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Builds the short, privacy-safe crash record that is shown to the user on the next launch.
 *
 * The record deliberately contains only: time, thread name, exception class names (the throwable
 * and up to [MAX_CAUSES] causes) and a truncated stack of class/method/source-file:line frames.
 * Exception messages are never written because they may contain file paths or PDF names.
 */
object CrashReportFormatter {

    const val MAX_FRAMES = 8
    const val MAX_CAUSES = 3
    private const val MAX_LENGTH = 2000

    fun format(throwable: Throwable, threadName: String?, timeMillis: Long): String {
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(timeMillis))

        val text = buildString {
            appendLine("time: $time")
            appendLine("thread: ${threadName ?: "?"}")
            appendLine("exception: ${throwable.javaClass.name}")

            var cause = throwable.cause
            var depth = 0
            while (cause != null && cause !== cause.cause && depth < MAX_CAUSES) {
                appendLine("cause: ${cause.javaClass.name}")
                cause = cause.cause
                depth++
            }

            appendLine("stack:")
            throwable.stackTrace.take(MAX_FRAMES).forEach { frame ->
                val source = when {
                    frame.isNativeMethod -> "Native Method"
                    frame.fileName == null -> "Unknown Source"
                    frame.lineNumber >= 0 -> "${frame.fileName}:${frame.lineNumber}"
                    else -> frame.fileName
                }
                appendLine("  at ${frame.className}.${frame.methodName}($source)")
            }
        }
        return text.take(MAX_LENGTH)
    }
}

/**
 * Single-record crash store backed by a small file. [save] is synchronous on purpose: it runs
 * inside the uncaught-exception handler right before the process dies.
 */
class CrashReportStore(private val file: File) {

    /** Never throws: a failing store must not mask the original crash. */
    fun save(report: String): Boolean = try {
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { out ->
            out.write(report.toByteArray(Charsets.UTF_8))
            out.fd.sync()
        }
        true
    } catch (_: Throwable) {
        false
    }

    fun read(): String? = try {
        if (file.isFile) file.readText(Charsets.UTF_8).takeIf { it.isNotBlank() } else null
    } catch (_: Exception) {
        null
    }

    fun clear() {
        try {
            file.delete()
        } catch (_: Exception) {
            // best effort
        }
    }

    companion object {
        const val FILE_NAME = "last_crash.txt"
    }
}
