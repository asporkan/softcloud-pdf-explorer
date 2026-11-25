package com.rejowan.pdfreaderpro.util

import android.content.Context
import android.os.Process
import kotlinx.coroutines.CoroutineExceptionHandler
import timber.log.Timber
import java.io.File
import kotlin.system.exitProcess

/**
 * Records a short crash report and then lets the system handle the crash.
 *
 * The previous default handler (Android's own) is always invoked afterwards so the crash is
 * reported to Play Console / Android vitals and the process terminates cleanly. The record
 * is shown to the user once on the next launch (see [CrashReportStore]).
 */
object GlobalErrorHandler {

    fun crashReportStore(context: Context): CrashReportStore =
        CrashReportStore(File(context.applicationContext.noBackupFilesDir, CrashReportStore.FILE_NAME))

    fun setup(context: Context) {
        val store = crashReportStore(context)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        // Avoid wrapping ourselves twice if setup is called again.
        if (previous is CrashRecordingHandler) return

        Thread.setDefaultUncaughtExceptionHandler(
            CrashRecordingHandler(
                store = store,
                previous = previous,
                terminate = {
                    Process.killProcess(Process.myPid())
                    exitProcess(10)
                }
            )
        )
        Timber.d("Global error handler initialized")
    }
}

/**
 * [Thread.UncaughtExceptionHandler] that writes a crash record synchronously and delegates to
 * [previous]; if there is none it calls [terminate]. Nothing here may throw.
 */
class CrashRecordingHandler(
    private val store: CrashReportStore,
    private val previous: Thread.UncaughtExceptionHandler?,
    private val terminate: () -> Unit,
    private val clock: () -> Long = System::currentTimeMillis
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            Timber.e(throwable, "Uncaught exception in thread: ${thread.name}")
            store.save(CrashReportFormatter.format(throwable, thread.name, clock()))
        } catch (_: Throwable) {
            // The crash must still reach the system handler.
        }

        if (previous != null) {
            previous.uncaughtException(thread, throwable)
        } else {
            terminate()
        }
    }
}

/**
 * CoroutineExceptionHandler for structured concurrency.
 * Use this in CoroutineScope to catch and log exceptions.
 */
val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
    Timber.e(throwable, "Coroutine exception")
}
