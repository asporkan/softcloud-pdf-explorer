package com.rejowan.pdfreaderpro.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rejowan.pdfreaderpro.R

/**
 * Single list/grid switch. The icon shows the view you get after tapping (a grid icon while the
 * list is shown, a list icon while the grid is shown), and the label names that action, e.g.
 * "Switch to grid view". The interactive area is 48dp (see [CompactTouchTarget]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewModeToggleButton(
    isGridView: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = stringResource(
        if (isGridView) R.string.view_mode_switch_to_list else R.string.view_mode_switch_to_grid
    )
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
        modifier = modifier
    ) {
        CompactTouchTarget(
            onClick = onToggle,
            visualSize = 32.dp,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            Icon(
                imageVector = if (isGridView) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
