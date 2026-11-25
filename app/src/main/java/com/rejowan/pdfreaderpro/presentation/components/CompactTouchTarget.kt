package com.rejowan.pdfreaderpro.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A compact visual (for example a 32dp rounded options chip) whose interactive area is still at
 * least 48x48dp, as Android accessibility guidelines require.
 *
 * The clickable surface is the 48dp outer box; the [visualSize] box only paints the container.
 * Put the icon or text inside [content]; its `contentDescription` becomes the control's label.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompactTouchTarget(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    visualSize: Dp = 32.dp,
    shape: Shape = CircleShape,
    containerColor: Color = Color.Transparent,
    visualModifier: Modifier = Modifier,
    enabled: Boolean = true,
    role: Role? = Role.Button,
    rippleColor: Color = Color.Unspecified,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val indication = if (rippleColor == Color.Unspecified) {
        LocalIndication.current
    } else {
        ripple(color = rippleColor)
    }
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = indication,
                enabled = enabled,
                role = role,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(visualSize)
                .clip(shape)
                .background(containerColor)
                .then(visualModifier),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}
