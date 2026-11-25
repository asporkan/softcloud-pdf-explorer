package com.rejowan.pdfreaderpro.presentation.components

import android.content.Context
import android.widget.Toast
import com.rejowan.pdfreaderpro.R

/** Tells the user whether a single-file delete worked, so failures are never silent. */
fun Context.showDeleteResult(success: Boolean) {
    val message = if (success) R.string.file_deleted else R.string.delete_failed
    Toast.makeText(this, message, if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
}

/** Result message for batch deletes. */
fun Context.showBatchDeleteResult(successCount: Int, failCount: Int) {
    val message = if (failCount == 0) {
        getString(R.string.files_deleted_count, successCount)
    } else if (successCount == 0) {
        getString(R.string.delete_failed)
    } else {
        getString(R.string.files_delete_partial, successCount, failCount)
    }
    Toast.makeText(this, message, if (failCount == 0) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
}
