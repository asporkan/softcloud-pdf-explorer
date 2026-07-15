package com.rejowan.pdfreaderpro.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rejowan.pdfreaderpro.R

/**
 * Shows a load/blocked error when a tool could not accept the selected PDF
 * (e.g. password-protected) instead of silently returning to the empty picker.
 */
@Composable
fun ToolLoadErrorDialog(
    message: String?,
    onDismiss: () -> Unit
) {
    if (message.isNullOrBlank()) return

    val passwordBlocked = message == stringResource(R.string.tool_error_pdf_password_protected)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (passwordBlocked) R.string.password_protected else R.string.error_generic
                )
            )
        },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}
