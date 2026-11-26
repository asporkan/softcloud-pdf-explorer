package com.rejowan.pdfreaderpro.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rejowan.pdfreaderpro.presentation.theme.AccentTone
import com.rejowan.pdfreaderpro.presentation.theme.accents
import androidx.compose.material3.minimumInteractiveComponentSize

/**
 * Home tab strip with the list/grid switch on the right.
 *
 * Tab widths are proportional to the measured width of their labels (not equal), so the longest
 * label ("Son Açılanlar") gets the room it needs and none is cut at 360dp. Only when the labels
 * cannot fit at all (very large font scale) do they shrink together and ellipsize.
 */
@Composable
fun CompactTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    isGridView: Boolean,
    onViewModeToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val softPurple = MaterialTheme.accents.purple
    val textMeasurer = rememberTextMeasurer()
    val boldStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
    val innerPaddingPx = with(LocalDensity.current) { (TabTextPadding * 2).toPx() }
    val tabWeights = remember(tabs, boldStyle, innerPaddingPx) {
        tabs.map { title ->
            val textWidth = textMeasurer.measure(
                text = title,
                style = boldStyle,
                maxLines = 1,
                softWrap = false
            ).size.width
            (textWidth + innerPaddingPx).coerceAtLeast(1f)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, title ->
                CompactTab(
                    title = title,
                    isSelected = index == selectedIndex,
                    accentColor = softPurple,
                    onClick = { onTabSelected(index) },
                    modifier = Modifier.weight(tabWeights[index])
                )
            }
        }

        // One button switches list <-> grid; its 48dp touch target is the only fixed width.
        ViewModeToggleButton(
            isGridView = isGridView,
            onToggle = onViewModeToggle
        )
    }
}

private val TabTextPadding = 4.dp

@Composable
private fun CompactTab(
    title: String,
    isSelected: Boolean,
    accentColor: AccentTone,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) accentColor.container else Color.Transparent,
        label = "tab background"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) accentColor.onContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "tab text"
    )

    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = backgroundColor
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = textColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TabTextPadding, vertical = 8.dp)
        )
    }
}
