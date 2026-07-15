package com.rejowan.pdfreaderpro.util

import android.content.Context
import com.rejowan.pdfreaderpro.R
import com.rejowan.pdfreaderpro.domain.repository.PdfToolsRepository

/**
 * Shared gate for PDF tools that cannot process encrypted documents.
 * Returns a localized user message when [path] is password-protected; null when OK.
 */
suspend fun PdfToolsRepository.passwordProtectedBlockMessage(
    context: Context,
    path: String
): String? {
    val protected = isPasswordProtected(path).getOrDefault(false)
    return if (protected) {
        context.getString(R.string.tool_error_pdf_password_protected)
    } else {
        null
    }
}
