package com.rejowan.pdfreaderpro.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import com.rejowan.pdfreaderpro.util.FormattingUtils
import java.util.Locale

/**
 * File size text for the UI, in the language the app is shown in (decimal comma in Turkish,
 * decimal point in English). Every size shown in the app goes through here.
 */
@Composable
fun formatFileSize(bytes: Long): String {
    val locales = LocalConfiguration.current.locales
    val locale = if (locales.isEmpty) Locale.getDefault() else locales[0]
    return remember(bytes, locale) { FormattingUtils.formattedFileSize(bytes, locale) }
}
