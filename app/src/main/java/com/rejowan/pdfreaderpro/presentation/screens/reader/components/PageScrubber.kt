package com.rejowan.pdfreaderpro.presentation.screens.reader.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * The scrubber floats over the PDF page, whose colour is set by the reading theme
 * (Light / Sepia / Dark / Black) and can be anything. Every part of it therefore sits on a
 * translucent `inverseSurface` chip with `inverseOnSurface` content (>= 4.5:1) and a thin
 * outline so the chip itself stays visible on both light and dark pages.
 */
private const val ChipAlpha = 0.9f

@Composable
private fun chipColor(): Color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = ChipAlpha)

/** "1" / total page count label on its own chip. */
@Composable
private fun PageCountChip(text: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.inverseOnSurface,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = modifier
            .clip(shape)
            .background(chipColor())
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), shape)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

/**
 * Page scrubber that adapts to scroll direction.
 * - Vertical mode: vertical track on right edge
 * - Horizontal mode: horizontal track above bottom bar
 */
@Composable
fun PageScrubber(
    currentPage: Int,
    totalPages: Int,
    isVisible: Boolean,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isHorizontal: Boolean = false,
    bottomClearance: Dp = 80.dp
) {
    if (isHorizontal) {
        HorizontalPageScrubber(
            currentPage = currentPage,
            totalPages = totalPages,
            isVisible = isVisible,
            onPageChange = onPageChange,
            modifier = modifier
        )
    } else {
        VerticalPageScrubber(
            currentPage = currentPage,
            totalPages = totalPages,
            isVisible = isVisible,
            onPageChange = onPageChange,
            modifier = modifier,
            bottomClearance = bottomClearance
        )
    }
}

@Composable
private fun VerticalPageScrubber(
    currentPage: Int,
    totalPages: Int,
    isVisible: Boolean,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    bottomClearance: Dp = 80.dp
) {
    val density = LocalDensity.current

    val progress = if (totalPages > 1) currentPage.toFloat() / (totalPages - 1) else 0f

    var trackHeightPx by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(progress) }
    var showJumpToPageSheet by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(
        targetValue = if (isDragging) dragProgress else progress,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "scrubber progress"
    )

    val displayPage = if (isDragging) {
        (dragProgress * (totalPages - 1)).roundToInt() + 1
    } else {
        currentPage + 1
    }

    val trackColor = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.3f)
    val progressColor = MaterialTheme.colorScheme.inversePrimary
    val railColor = chipColor()
    val railBorder = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)

    AnimatedVisibility(
        visible = isVisible && totalPages > 1,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ) + fadeIn(),
        exit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ) + fadeOut(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(end = 6.dp, top = 86.dp, bottom = bottomClearance),
            horizontalAlignment = Alignment.End
        ) {
            // Track area with droplet
            Box(
                modifier = Modifier
                    .weight(1f)
                    .width(64.dp)
                    .onSizeChanged { trackHeightPx = it.height.toFloat() }
                    .pointerInput(totalPages) {
                        detectTapGestures { offset ->
                            val newProgress = (offset.y / trackHeightPx).coerceIn(0f, 1f)
                            val newPage = (newProgress * (totalPages - 1)).roundToInt()
                            onPageChange(newPage)
                        }
                    }
                    .pointerInput(totalPages) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                isDragging = true
                                val newProgress = (offset.y / trackHeightPx).coerceIn(0f, 1f)
                                dragProgress = newProgress
                                val newPage = (newProgress * (totalPages - 1)).roundToInt()
                                onPageChange(newPage)
                            },
                            onDragEnd = {
                                isDragging = false
                            },
                            onDragCancel = { isDragging = false },
                            onDrag = { change, _ ->
                                change.consume()
                                val newProgress = (change.position.y / trackHeightPx).coerceIn(0f, 1f)
                                val oldPage = (dragProgress * (totalPages - 1)).roundToInt()
                                val newPage = (newProgress * (totalPages - 1)).roundToInt()
                                dragProgress = newProgress
                                if (newPage != oldPage) {
                                    onPageChange(newPage)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.TopEnd
            ) {
                // Droplet thumb height
                val thumbHeight = 36.dp
                val thumbHeightPx = with(density) { thumbHeight.toPx() }
                val trackPadding = thumbHeight / 2

                // Rail chip behind the track so it reads on any page colour
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(vertical = trackPadding - 2.dp)
                        .width(8.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(railColor)
                        .border(1.dp, railBorder, RoundedCornerShape(4.dp))
                )

                // Track background
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(vertical = trackPadding, horizontal = 2.dp)
                        .width(4.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(trackColor)
                )

                // Progress fill
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(vertical = trackPadding, horizontal = 2.dp)
                        .width(4.dp)
                        .fillMaxHeight(animatedProgress.coerceIn(0.001f, 1f))
                        .clip(RoundedCornerShape(2.dp))
                        .background(progressColor)
                )

                // Droplet thumb
                Box(
                    modifier = Modifier
                        .offset {
                            val yOffset = (animatedProgress * (trackHeightPx - thumbHeightPx)).roundToInt()
                            IntOffset(0, yOffset)
                        }
                        .align(Alignment.TopEnd)
                        .padding(end = 4.dp)
                ) {
                    VerticalDropletThumb(
                        pageNumber = displayPage,
                        isDragging = isDragging,
                        onClick = { showJumpToPageSheet = true }
                    )
                }
            }

            // Total pages at bottom
            PageCountChip(text = "$totalPages", modifier = Modifier.padding(top = 4.dp))
        }
    }

    // Jump to Page Bottom Sheet
    if (showJumpToPageSheet) {
        PageJumpSheet(
            currentPage = currentPage + 1,
            totalPages = totalPages,
            onDismiss = { showJumpToPageSheet = false },
            onPageSelected = { page ->
                onPageChange(page - 1)
                showJumpToPageSheet = false
            }
        )
    }
}

@Composable
private fun HorizontalPageScrubber(
    currentPage: Int,
    totalPages: Int,
    isVisible: Boolean,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    val progress = if (totalPages > 1) currentPage.toFloat() / (totalPages - 1) else 0f

    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(progress) }
    var showJumpToPageSheet by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(
        targetValue = if (isDragging) dragProgress else progress,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "scrubber progress"
    )

    val displayPage = if (isDragging) {
        (dragProgress * (totalPages - 1)).roundToInt() + 1
    } else {
        currentPage + 1
    }

    val trackColor = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.3f)
    val progressColor = MaterialTheme.colorScheme.inversePrimary
    val railColor = chipColor()
    val railBorder = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)

    // Thumb dimensions - must match HorizontalDropletThumb size
    val thumbWidth = 36.dp
    val thumbWidthPx = with(density) { thumbWidth.toPx() }
    val trackPadding = thumbWidth / 2  // So tip reaches both ends

    AnimatedVisibility(
        visible = isVisible && totalPages > 1,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ) + fadeOut(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Page 1 indicator
            PageCountChip(text = "1", modifier = Modifier.padding(end = 4.dp))

            // Track area with droplet
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .onSizeChanged { trackWidthPx = it.width.toFloat() }
                    .pointerInput(totalPages) {
                        detectTapGestures { offset ->
                            val newProgress = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                            val newPage = (newProgress * (totalPages - 1)).roundToInt()
                            onPageChange(newPage)
                        }
                    }
                    .pointerInput(totalPages) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                isDragging = true
                                val newProgress = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                                dragProgress = newProgress
                                val newPage = (newProgress * (totalPages - 1)).roundToInt()
                                onPageChange(newPage)
                            },
                            onDragEnd = { isDragging = false },
                            onDragCancel = { isDragging = false },
                            onDrag = { change, _ ->
                                change.consume()
                                val newProgress = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                                val oldPage = (dragProgress * (totalPages - 1)).roundToInt()
                                val newPage = (newProgress * (totalPages - 1)).roundToInt()
                                dragProgress = newProgress
                                if (newPage != oldPage) {
                                    onPageChange(newPage)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.BottomStart
            ) {
                // Rail chip behind the track so it reads on any page colour
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = trackPadding - 2.dp)
                        .height(8.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(railColor)
                        .border(1.dp, railBorder, RoundedCornerShape(4.dp))
                )

                // Track background - shorter so thumb tip aligns at edges
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = trackPadding, vertical = 2.dp)
                        .height(4.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(2.dp))
                        .background(trackColor)
                )

                // Progress fill - same padding, uses fillMaxWidth like vertical uses fillMaxHeight
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = trackPadding, vertical = 2.dp)
                        .height(4.dp)
                        .fillMaxWidth(animatedProgress.coerceIn(0.001f, 1f))
                        .clip(RoundedCornerShape(2.dp))
                        .background(progressColor)
                )

                // Droplet thumb (pointing down)
                Box(
                    modifier = Modifier
                        .offset {
                            val xOffset = (animatedProgress * (trackWidthPx - thumbWidthPx)).roundToInt()
                            IntOffset(xOffset, 0)
                        }
                        .align(Alignment.BottomStart)
                        .padding(bottom = 4.dp)
                ) {
                    HorizontalDropletThumb(
                        pageNumber = displayPage,
                        isDragging = isDragging,
                        onClick = { showJumpToPageSheet = true }
                    )
                }
            }

            // Total pages indicator
            PageCountChip(text = "$totalPages", modifier = Modifier.padding(start = 4.dp))
        }
    }

    // Jump to Page Bottom Sheet
    if (showJumpToPageSheet) {
        PageJumpSheet(
            currentPage = currentPage + 1,
            totalPages = totalPages,
            onDismiss = { showJumpToPageSheet = false },
            onPageSelected = { page ->
                onPageChange(page - 1)
                showJumpToPageSheet = false
            }
        )
    }
}

// Droplet shape pointing RIGHT (for vertical scrubber)
private val VerticalDropletShape = GenericShape { size, _ ->
    val width = size.width
    val height = size.height
    val bulbRadius = height / 2

    moveTo(width, height / 2)

    quadraticTo(
        x1 = width * 0.5f,
        y1 = 0f,
        x2 = bulbRadius,
        y2 = 0f
    )

    arcTo(
        rect = androidx.compose.ui.geometry.Rect(
            left = 0f,
            top = 0f,
            right = bulbRadius * 2,
            bottom = height
        ),
        startAngleDegrees = -90f,
        sweepAngleDegrees = -180f,
        forceMoveTo = false
    )

    quadraticTo(
        x1 = width * 0.5f,
        y1 = height,
        x2 = width,
        y2 = height / 2
    )

    close()
}

// Droplet shape pointing DOWN (for horizontal scrubber)
private val HorizontalDropletShape = GenericShape { size, _ ->
    val width = size.width
    val height = size.height
    val bulbRadius = width / 2

    // Start at the pointed tip (bottom center)
    moveTo(width / 2, height)

    // Curve from tip to right side of bulb
    quadraticTo(
        x1 = width,
        y1 = height * 0.5f,
        x2 = width,
        y2 = bulbRadius
    )

    // Arc for the top bulb (right to left)
    arcTo(
        rect = androidx.compose.ui.geometry.Rect(
            left = 0f,
            top = 0f,
            right = width,
            bottom = bulbRadius * 2
        ),
        startAngleDegrees = 0f,
        sweepAngleDegrees = -180f,
        forceMoveTo = false
    )

    // Curve from left side of bulb back to tip
    quadraticTo(
        x1 = 0f,
        y1 = height * 0.5f,
        x2 = width / 2,
        y2 = height
    )

    close()
}

@Composable
private fun VerticalDropletThumb(
    pageNumber: Int,
    isDragging: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val bgColor = if (isDragging) primary else primary.copy(alpha = 0.95f)

    Surface(
        modifier = modifier
            .width(56.dp)
            .height(36.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = !isDragging,
                onClick = onClick
            ),
        shape = VerticalDropletShape,
        color = bgColor,
        shadowElevation = if (isDragging) 8.dp else 4.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(end = 8.dp, start = 2.dp)
        ) {
            Text(
                text = "$pageNumber",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HorizontalDropletThumb(
    pageNumber: Int,
    isDragging: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val bgColor = if (isDragging) primary else primary.copy(alpha = 0.95f)

    Surface(
        modifier = modifier
            .width(36.dp)
            .height(48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = !isDragging,
                onClick = onClick
            ),
        shape = HorizontalDropletShape,
        color = bgColor,
        shadowElevation = if (isDragging) 8.dp else 4.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(bottom = 10.dp, top = 2.dp)
        ) {
            Text(
                text = "$pageNumber",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
