package com.rejowan.pdfreaderpro.presentation.screens.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rejowan.pdfreaderpro.R
import com.rejowan.pdfreaderpro.presentation.screens.tools.common.OutputFolderStore
import com.rejowan.pdfreaderpro.presentation.theme.AccentTone
import com.rejowan.pdfreaderpro.presentation.theme.accents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.rejowan.pdfreaderpro.presentation.components.AppDragHandle
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role

/**
 * Settings row + options sheet for the default save folder of the PDF tools.
 *
 * Backed by [OutputFolderStore], the same store the tool screens use, so a folder chosen
 * here is what tools save into, and a folder chosen from a tool shows up here on return.
 */
@Composable
internal fun OutputFolderSetting(
    accentColor: AccentTone,
    animationDelay: Int
) {
    val context = LocalContext.current
    val store = remember { OutputFolderStore(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // The store is not observable; re-read it whenever this screen resumes or after an edit.
    var refreshTick by remember { mutableIntStateOf(0) }
    var folderLabel by remember { mutableStateOf<String?>(null) }
    var askEveryTime by remember { mutableStateOf(store.askEveryTime) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(refreshTick) {
        // folderLabel() touches the document provider; keep it off the main thread.
        folderLabel = withContext(Dispatchers.IO) { store.folderLabel() }
        askEveryTime = store.askEveryTime
    }

    var showSheet by remember { mutableStateOf(false) }
    var showExplainDialog by remember { mutableStateOf(false) }

    val treeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            store.setFolder(uri)
            if (store.getFolder() == null) {
                // The grant could not be kept (or the folder is not writable): tell the user.
                Toast.makeText(context, R.string.settings_output_folder_failed, Toast.LENGTH_LONG).show()
            }
            refreshTick++
        }
    }

    val noneLabel = stringResource(R.string.settings_output_folder_none)
    val currentLabel = folderLabel ?: noneLabel
    val subtitle = if (askEveryTime) {
        stringResource(R.string.settings_output_folder_ask, currentLabel)
    } else {
        currentLabel
    }

    SettingsOptionItem(
        icon = Icons.Rounded.Folder,
        title = stringResource(R.string.settings_output_folder_title),
        subtitle = subtitle,
        accentColor = accentColor,
        onClick = { showSheet = true },
        animationDelay = animationDelay
    )

    if (showSheet) {
        OutputFolderSheet(
            accentColor = accentColor,
            folderLabel = currentLabel,
            canReset = folderLabel != null || askEveryTime,
            askEveryTime = askEveryTime,
            onChangeFolder = {
                showSheet = false
                showExplainDialog = true
            },
            onAskEveryTimeChange = {
                store.askEveryTime = it
                askEveryTime = it
            },
            onReset = {
                store.resetToDefaults()
                askEveryTime = false
                refreshTick++
                showSheet = false
            },
            onDismiss = { showSheet = false }
        )
    }

    if (showExplainDialog) {
        // Same explanation the tools show before the system folder picker opens.
        AlertDialog(
            onDismissRequest = { showExplainDialog = false },
            title = { Text(stringResource(R.string.tools_output_dialog_title)) },
            text = { Text(stringResource(R.string.tools_output_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showExplainDialog = false
                    treeLauncher.launch(store.initialUri())
                }) {
                    Text(stringResource(R.string.tools_output_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExplainDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OutputFolderSheet(
    accentColor: AccentTone,
    folderLabel: String,
    canReset: Boolean,
    askEveryTime: Boolean,
    onChangeFolder: () -> Unit,
    onAskEveryTimeChange: (Boolean) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    // Open fully so the last row is never left under the navigation bar.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { AppDragHandle() },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBox(icon = Icons.Rounded.Save, accentColor = accentColor)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_output_folder_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = folderLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            OutputOptionRow(
                icon = Icons.Rounded.FolderOpen,
                title = stringResource(R.string.settings_output_change_folder),
                description = stringResource(R.string.settings_output_change_folder_desc),
                accentColor = accentColor,
                onClick = onChangeFolder
            )

            OutputOptionRow(
                icon = Icons.Rounded.Save,
                title = stringResource(R.string.tools_output_ask_every_time),
                description = stringResource(R.string.settings_output_ask_desc),
                accentColor = accentColor,
                onClick = { onAskEveryTimeChange(!askEveryTime) },
                toggleState = askEveryTime,
                trailing = {
                    Switch(
                        checked = askEveryTime,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = accentColor.onColor,
                            checkedTrackColor = accentColor.color
                        )
                    )
                }
            )

            OutputOptionRow(
                icon = Icons.Rounded.RestartAlt,
                title = stringResource(R.string.settings_output_reset),
                description = stringResource(R.string.settings_output_reset_desc),
                accentColor = MaterialTheme.accents.red,
                enabled = canReset,
                onClick = onReset
            )
        }
    }
}

@Composable
private fun OutputOptionRow(
    icon: ImageVector,
    title: String,
    description: String,
    accentColor: AccentTone,
    onClick: () -> Unit,
    enabled: Boolean = true,
    toggleState: Boolean? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                when {
                    !enabled -> Modifier
                    toggleState != null -> Modifier.toggleable(
                        value = toggleState,
                        role = Role.Switch,
                        onValueChange = { onClick() }
                    )
                    else -> Modifier.clickable(onClick = onClick)
                }
            ),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBox(icon = icon, accentColor = accentColor, enabled = enabled)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = if (enabled) onSurface else onSurface.copy(alpha = 0.38f)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        onSurface.copy(alpha = 0.38f)
                    }
                )
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailing()
            }
        }
    }
}

@Composable
private fun IconBox(
    icon: ImageVector,
    accentColor: AccentTone,
    enabled: Boolean = true
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (enabled) accentColor.container else onSurface.copy(alpha = 0.12f)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .padding(8.dp)
                .size(20.dp),
            tint = if (enabled) accentColor.color else onSurface.copy(alpha = 0.38f)
        )
    }
}
