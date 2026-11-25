package com.rejowan.pdfreaderpro.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rejowan.pdfreaderpro.R
import com.rejowan.pdfreaderpro.presentation.theme.AccentTone
import com.rejowan.pdfreaderpro.presentation.theme.accents
import androidx.compose.material3.minimumInteractiveComponentSize

private val SelectionBlue: AccentTone @Composable get() = MaterialTheme.accents.blue
private val AccentRed: AccentTone @Composable get() = MaterialTheme.accents.red
private val AccentPurple: AccentTone @Composable get() = MaterialTheme.accents.purple
private val AccentTeal: AccentTone @Composable get() = MaterialTheme.accents.teal

@Composable
fun SelectionActionBar(
    selectedCount: Int,
    totalCount: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onMerge: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        tonalElevation = 2.dp
    ) {
        // The surface runs under the system navigation bar; the content stays above it,
        // so the Merge/Share/Delete labels never collide with the gesture/3-button bar.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            // Top row: Close button, count, select all
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Close button
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_close_selection),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Selection count
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.selected_count_format, selectedCount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SelectionBlue.color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.of_files_count, totalCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Select All button
                Surface(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .padding(end = 4.dp)
                        .widthIn(max = 160.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onSelectAll),
                    color = if (selectedCount == totalCount) {
                        SelectionBlue.container
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.SelectAll,
                            contentDescription = stringResource(R.string.cd_select_all),
                            modifier = Modifier.size(18.dp),
                            tint = if (selectedCount == totalCount) SelectionBlue.color
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedCount == totalCount) {
                                stringResource(R.string.quick_select_all)
                            } else {
                                stringResource(R.string.select_all)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (selectedCount == totalCount) SelectionBlue.onContainer
                            else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Action buttons row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, start = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Merge button (only enabled with 2+ files)
                ActionButton(
                    icon = Icons.AutoMirrored.Filled.CallMerge,
                    label = stringResource(R.string.merge),
                    tone = AccentPurple,
                    enabled = selectedCount >= 2,
                    onClick = onMerge,
                    modifier = Modifier.weight(1f)
                )

                // Share button
                ActionButton(
                    icon = Icons.Default.Share,
                    label = stringResource(R.string.share),
                    tone = AccentTeal,
                    enabled = selectedCount > 0,
                    onClick = onShare,
                    modifier = Modifier.weight(1f)
                )

                // Delete button
                ActionButton(
                    icon = Icons.Default.Delete,
                    label = stringResource(R.string.delete),
                    tone = AccentRed,
                    enabled = selectedCount > 0,
                    onClick = onDelete,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    tone: AccentTone,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Disabled state follows the Material 3 defaults (onSurface 12% fill, 38% content).
    val onSurface = MaterialTheme.colorScheme.onSurface
    val containerColor = if (enabled) tone.container else onSurface.copy(alpha = 0.12f)
    val iconColor = if (enabled) tone.color else onSurface.copy(alpha = 0.38f)
    val labelColor = if (enabled) onSurface else onSurface.copy(alpha = 0.38f)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        color = containerColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = iconColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun AnimatedSelectionActionBar(
    visible: Boolean,
    selectedCount: Int,
    totalCount: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onMerge: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        SelectionActionBar(
            selectedCount = selectedCount,
            totalCount = totalCount,
            onClose = onClose,
            onSelectAll = onSelectAll,
            onMerge = onMerge,
            onShare = onShare,
            onDelete = onDelete
        )
    }
}
