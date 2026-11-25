package com.rejowan.pdfreaderpro.presentation.screens.tools.common

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rejowan.pdfreaderpro.R
import com.rejowan.pdfreaderpro.util.FileOperations
import com.rejowan.pdfreaderpro.util.SafOutputHelper
import java.io.File

/** A tool output that was written to the user's chosen destination. */
data class CommittedOutput(
    /** Cache copy named after the real saved file (used for "Open in app" and sharing). */
    val localCopy: File,
    /** Real name of the saved document (DISPLAY_NAME). */
    val displayName: String,
    /** Readable folder the document was saved to. */
    val locationLabel: String
)

object ToolOutputWriter {

    /**
     * Copies [tempFile] into [destinationUri]. On failure the (empty) destination is deleted so no
     * orphan file stays behind, and null is returned. [tempFile] is always removed.
     */
    fun commit(context: Context, tempFile: File, destinationUri: Uri): CommittedOutput? {
        try {
            if (!FileOperations.writeFileToUri(context, tempFile, destinationUri)) {
                SafOutputHelper.deleteDocumentQuietly(context, destinationUri)
                return null
            }
            val name = SafOutputHelper.displayName(context, destinationUri)
                ?: destinationUri.lastPathSegment?.substringAfterLast('/')
                ?: tempFile.name
            val displayName = if (name.endsWith(".pdf", ignoreCase = true)) name else "$name.pdf"
            val local = SafOutputHelper.namedLocalCopy(context, tempFile, displayName)
            SafOutputHelper.scanSaved(context, listOf(destinationUri), "application/pdf")
            return CommittedOutput(
                localCopy = local,
                displayName = displayName,
                locationLabel = SafOutputHelper.readableParentOf(
                    destinationUri,
                    context.getString(R.string.tools_output_root)
                )
            )
        } finally {
            tempFile.delete()
        }
    }
}

/** One-line "Save location" caption for result cards; renders nothing when [label] is blank. */
@Composable
fun ResultLocationText(label: String, modifier: Modifier = Modifier) {
    if (label.isBlank()) return
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            stringResource(R.string.tools_output_location, label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
