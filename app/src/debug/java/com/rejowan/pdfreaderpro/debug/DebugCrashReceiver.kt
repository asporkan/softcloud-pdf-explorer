package com.rejowan.pdfreaderpro.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Debug-only crash trigger for QA (this source set is not part of the release build).
 *
 * adb shell am broadcast -n com.softcloud.pdfexplorer/com.rejowan.pdfreaderpro.debug.DebugCrashReceiver
 *
 * Throws on the main thread, so it exercises the uncaught-exception path.
 */
class DebugCrashReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        throw IllegalStateException("QA test crash (debug build)")
    }
}
