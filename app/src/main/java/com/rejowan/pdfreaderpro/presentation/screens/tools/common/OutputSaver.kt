package com.rejowan.pdfreaderpro.presentation.screens.tools.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rejowan.pdfreaderpro.R
import com.rejowan.pdfreaderpro.util.FileOperations
import com.rejowan.pdfreaderpro.util.SafOutputHelper

private class CreatePdfContract : ActivityResultContract<Pair<String, Uri?>, Uri?>() {
    override fun createIntent(context: Context, input: Pair<String, Uri?>): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, input.first)
            val initial = input.second
            if (initial != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, initial)
            }
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (resultCode == android.app.Activity.RESULT_OK) intent?.data else null
    }
}

internal enum class PendingKind { FILE, FOLDER, CHANGE }

/**
 * Decides where a tool output goes: silently into the remembered folder, into a freshly chosen
 * folder (first save), or through the system Save dialog when "ask every time" is on.
 * Create with [rememberOutputSaver]; show [OutputLocationRow] near the save button.
 */
@Stable
class OutputSaver internal constructor(
    private val context: Context,
    internal val store: OutputFolderStore
) {
    var folderLabel by mutableStateOf(store.folderLabel())
        private set
    var askEveryTime by mutableStateOf(store.askEveryTime)
        private set

    internal var onFile: (Uri) -> Unit = {}
    internal var onFolder: (Uri) -> Unit = {}
    internal var launchCreate: (Pair<String, Uri?>) -> Unit = {}
    internal var launchTree: (Uri?) -> Unit = {}

    internal var pendingDialog by mutableStateOf<PendingKind?>(null)
    private var pendingName: String = ""
    private var afterPick: PendingKind? = null

    fun refresh() {
        folderLabel = store.folderLabel()
        askEveryTime = store.askEveryTime
    }

    /** Single-file output (merge, rotate, lock, ...). Calls the file callback with a writable document URI. */
    fun saveFile(suggestedName: String) {
        val name = FileOperations.ensurePdfExtension(suggestedName)
        refresh()
        if (askEveryTime) {
            launchCreate(name to store.initialUri())
            return
        }
        val folder = store.getFolder()
        if (folder != null) {
            val uri = SafOutputHelper.createInFolder(context, folder, "application/pdf", name)
            if (uri != null) {
                onFile(uri)
                return
            }
            // Folder deleted or permission revoked: forget it and ask again without an error.
            store.clear()
            refresh()
        }
        pendingName = name
        pendingDialog = PendingKind.FILE
    }

    /** Multi-file output (split, PDF to images). Calls the folder callback with a writable tree URI. */
    fun requestFolder() {
        refresh()
        if (!askEveryTime) {
            val folder = store.getFolder()
            if (folder != null) {
                onFolder(folder)
                return
            }
            refresh()
        }
        pendingDialog = PendingKind.FOLDER
    }

    /** Lets the user pick a new remembered folder without saving anything. */
    fun changeFolder() {
        pendingDialog = PendingKind.CHANGE
    }

    internal fun confirmDialog() {
        val kind = pendingDialog ?: return
        pendingDialog = null
        afterPick = kind
        launchTree(store.initialUri())
    }

    internal fun dismissDialog() {
        pendingDialog = null
    }

    internal fun onTreePicked(uri: Uri?) {
        val kind = afterPick
        afterPick = null
        if (uri == null || kind == null) return
        store.setFolder(uri)
        refresh()
        when (kind) {
            PendingKind.FILE -> saveFile(pendingName)
            PendingKind.FOLDER -> onFolder(uri)
            PendingKind.CHANGE -> Unit
        }
    }

    internal fun onDocumentCreated(uri: Uri) {
        store.rememberSavedDocument(uri)
        onFile(uri)
    }
}

@Composable
fun rememberOutputSaver(
    onFileDestination: (Uri) -> Unit = {},
    onFolderDestination: (Uri) -> Unit = {}
): OutputSaver {
    val context = LocalContext.current
    val saver = remember { OutputSaver(context, OutputFolderStore(context)) }
    SideEffect {
        saver.onFile = onFileDestination
        saver.onFolder = onFolderDestination
    }
    val createLauncher = rememberLauncherForActivityResult(CreatePdfContract()) { uri ->
        uri?.let { saver.onDocumentCreated(it) }
    }
    val treeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        saver.onTreePicked(uri)
    }
    SideEffect {
        saver.launchCreate = { createLauncher.launch(it) }
        saver.launchTree = { treeLauncher.launch(it) }
    }
    return saver
}

/** "Save location: Download/PDFs · Change" line. Also hosts the folder explanation dialog. */
@Composable
fun OutputLocationRow(
    saver: OutputSaver,
    modifier: Modifier = Modifier,
    visible: Boolean = true
) {
    if (visible) {
        val label = when {
            saver.askEveryTime -> stringResource(R.string.tools_output_ask_every_time)
            else -> saver.folderLabel ?: stringResource(R.string.tools_output_not_chosen)
        }
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                stringResource(R.string.tools_output_location, label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            )
            TextButton(onClick = { saver.changeFolder() }) {
                Text(stringResource(R.string.tools_output_change), maxLines = 1)
            }
        }
    }
    if (saver.pendingDialog != null) {
        AlertDialog(
            onDismissRequest = { saver.dismissDialog() },
            title = { Text(stringResource(R.string.tools_output_dialog_title)) },
            text = { Text(stringResource(R.string.tools_output_dialog_message)) },
            confirmButton = {
                TextButton(onClick = { saver.confirmDialog() }) {
                    Text(stringResource(R.string.tools_output_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { saver.dismissDialog() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

/** Lays out tool [content] with the [OutputLocationRow] pinned below it. */
@Composable
fun ToolContentWithLocation(
    saver: OutputSaver,
    modifier: Modifier = Modifier,
    showLocation: Boolean = true,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.Column(modifier = modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) { content() }
        OutputLocationRow(
            saver = saver,
            modifier = Modifier.padding(horizontal = 16.dp),
            visible = showLocation
        )
    }
}
