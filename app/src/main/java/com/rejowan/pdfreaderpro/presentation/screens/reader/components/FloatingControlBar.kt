package com.rejowan.pdfreaderpro.presentation.screens.reader.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.ViewDay
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rejowan.pdfreaderpro.R
import com.rejowan.pdfreaderpro.presentation.theme.AccentTone
import com.rejowan.pdfreaderpro.presentation.theme.accents
import androidx.compose.material3.minimumInteractiveComponentSize

// Theme-aware accents: icon = tone.color on tone.container (contrast holds in Light/Dark/Black)
private val AccentPurple: AccentTone @Composable get() = MaterialTheme.accents.purple
private val AccentBlue: AccentTone @Composable get() = MaterialTheme.accents.blue
private val AccentTeal: AccentTone @Composable get() = MaterialTheme.accents.teal
private val AccentAmber: AccentTone @Composable get() = MaterialTheme.accents.amber

@Composable
fun FloatingControlBar(
    isBookmarked: Boolean,
    onTocClick: () -> Unit,
    onViewClick: () -> Unit,
    onZoomClick: () -> Unit,
    onDisplayClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f)

    Surface(
        modifier = modifier
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(32.dp),
                ambientColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.25f),
                spotColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)
            ),
        shape = RoundedCornerShape(32.dp),
        color = surfaceColor,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Table of Contents
            ControlBarButton(
                icon = Icons.AutoMirrored.Rounded.MenuBook,
                contentDescription = stringResource(R.string.cd_table_of_contents),
                onClick = onTocClick,
                tone = AccentPurple
            )

            // View Mode
            ControlBarButton(
                icon = Icons.Rounded.ViewDay,
                contentDescription = stringResource(R.string.view_options),
                onClick = onViewClick,
                tone = AccentBlue
            )

            // Zoom
            ControlBarButton(
                icon = Icons.Rounded.ZoomIn,
                contentDescription = stringResource(R.string.zoom),
                onClick = onZoomClick,
                tone = AccentTeal
            )

            // Display Settings (Theme, Brightness)
            ControlBarButton(
                icon = Icons.Rounded.Palette,
                contentDescription = stringResource(R.string.display_settings),
                onClick = onDisplayClick,
                tone = AccentAmber
            )

            // Bookmark
            BookmarkButton(
                isBookmarked = isBookmarked,
                onClick = onBookmarkClick
            )

            // More Options
            ControlBarButton(
                icon = Icons.Rounded.MoreHoriz,
                contentDescription = stringResource(R.string.more_options),
                onClick = onMoreClick,
                tone = MaterialTheme.accents.gray
            )
        }
    }
}

@Composable
private fun ControlBarButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tone: AccentTone,
    modifier: Modifier = Modifier
) {
    val tint = tone.color
    val backgroundColor = tone.container

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(46.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = tint, bounded = true),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun BookmarkButton(
    isBookmarked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val red = MaterialTheme.accents.red
    val tint = if (isBookmarked) red.color else MaterialTheme.colorScheme.onSurfaceVariant
    val backgroundColor = if (isBookmarked) red.container else Color.Transparent

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(46.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = red.color, bounded = true),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
            contentDescription = if (isBookmarked) {
                stringResource(R.string.cd_remove_bookmark)
            } else {
                stringResource(R.string.cd_add_bookmark)
            },
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}
