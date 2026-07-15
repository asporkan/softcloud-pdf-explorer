package com.rejowan.pdfreaderpro.presentation.screens.tools

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rejowan.pdfreaderpro.R
import com.rejowan.pdfreaderpro.presentation.navigation.navigateToCompressTool
import com.rejowan.pdfreaderpro.presentation.navigation.navigateToMergeTool
import com.rejowan.pdfreaderpro.presentation.navigation.navigateToLockTool
import com.rejowan.pdfreaderpro.presentation.navigation.navigateToReorderTool
import com.rejowan.pdfreaderpro.presentation.navigation.navigateToUnlockTool
import com.rejowan.pdfreaderpro.presentation.navigation.navigateToWatermarkTool
import com.rejowan.pdfreaderpro.presentation.navigation.navigateToPageNumbersTool
import com.rejowan.pdfreaderpro.presentation.navigation.navigateToRemovePagesTool
import com.rejowan.pdfreaderpro.presentation.navigation.navigateToRotateTool
import com.rejowan.pdfreaderpro.presentation.navigation.navigateToSplitTool
import com.rejowan.pdfreaderpro.presentation.navigation.navigateToImageToPdfTool
import com.rejowan.pdfreaderpro.presentation.navigation.navigateToPdfToImageTool
import kotlinx.coroutines.delay

// Accent colors (consistent with app design system)
private val AccentPurple = Color(0xFF9575CD)
private val AccentBlue = Color(0xFF64B5F6)
private val AccentTeal = Color(0xFF4DB6AC)
private val AccentAmber = Color(0xFFFFB74D)
private val AccentGreen = Color(0xFF81C784)

enum class ToolCategory(
    @param:StringRes val titleRes: Int,
    val accentColor: Color
) {
    ORGANIZE(R.string.tool_category_organize, AccentPurple),
    EDIT(R.string.tool_category_edit, AccentBlue),
    SECURITY(R.string.tool_category_security, AccentAmber),
    CONVERT(R.string.tool_category_convert, AccentTeal)
}

data class PdfTool(
    val id: String,
    @param:StringRes val nameRes: Int,
    @param:StringRes val descriptionRes: Int,
    val icon: ImageVector,
    val category: ToolCategory,
    val isEnabled: Boolean = false
)

private val pdfTools = listOf(
    PdfTool(
        id = "merge",
        nameRes = R.string.merge_pdfs,
        descriptionRes = R.string.tools_hub_merge_desc,
        icon = Icons.AutoMirrored.Filled.CallMerge,
        category = ToolCategory.ORGANIZE,
        isEnabled = true
    ),
    PdfTool(
        id = "split",
        nameRes = R.string.split_pdf,
        descriptionRes = R.string.tools_hub_split_desc,
        icon = Icons.AutoMirrored.Filled.CallSplit,
        category = ToolCategory.ORGANIZE,
        isEnabled = true
    ),
    PdfTool(
        id = "compress",
        nameRes = R.string.compress_pdf,
        descriptionRes = R.string.tools_hub_compress_desc,
        icon = Icons.Default.Compress,
        category = ToolCategory.ORGANIZE,
        isEnabled = true
    ),
    PdfTool(
        id = "rotate",
        nameRes = R.string.tool_rotate_pages,
        descriptionRes = R.string.tools_hub_rotate_desc,
        icon = Icons.AutoMirrored.Filled.RotateRight,
        category = ToolCategory.ORGANIZE,
        isEnabled = true
    ),
    PdfTool(
        id = "reorder",
        nameRes = R.string.tool_reorder_pages,
        descriptionRes = R.string.tools_hub_reorder_desc,
        icon = Icons.Default.Reorder,
        category = ToolCategory.ORGANIZE,
        isEnabled = true
    ),
    PdfTool(
        id = "remove_pages",
        nameRes = R.string.remove_pages,
        descriptionRes = R.string.tools_hub_remove_pages_desc,
        icon = Icons.Default.DeleteSweep,
        category = ToolCategory.EDIT,
        isEnabled = true
    ),
    PdfTool(
        id = "watermark",
        nameRes = R.string.add_watermark,
        descriptionRes = R.string.tools_hub_watermark_desc,
        icon = Icons.Default.WaterDrop,
        category = ToolCategory.EDIT,
        isEnabled = true
    ),
    PdfTool(
        id = "page_numbers",
        nameRes = R.string.add_page_numbers,
        descriptionRes = R.string.tools_hub_page_numbers_desc,
        icon = Icons.Default.FormatListNumbered,
        category = ToolCategory.EDIT,
        isEnabled = true
    ),
    PdfTool(
        id = "lock_pdf",
        nameRes = R.string.lock_pdf,
        descriptionRes = R.string.tools_hub_lock_desc,
        icon = Icons.Default.Lock,
        category = ToolCategory.SECURITY,
        isEnabled = true
    ),
    PdfTool(
        id = "unlock_pdf",
        nameRes = R.string.unlock_pdf,
        descriptionRes = R.string.tools_hub_unlock_desc,
        icon = Icons.Default.LockOpen,
        category = ToolCategory.SECURITY,
        isEnabled = true
    ),
    PdfTool(
        id = "img_to_pdf",
        nameRes = R.string.tool_image_to_pdf,
        descriptionRes = R.string.tools_hub_image_to_pdf_desc,
        icon = Icons.Default.Image,
        category = ToolCategory.CONVERT,
        isEnabled = true
    ),
    PdfTool(
        id = "pdf_to_img",
        nameRes = R.string.pdf_to_images,
        descriptionRes = R.string.tools_hub_pdf_to_images_desc,
        icon = Icons.Default.Photo,
        category = ToolCategory.CONVERT,
        isEnabled = true
    ),
)

@Composable
fun ToolsScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 80.dp)
    ) {
        var animationIndex = 0

        ToolCategory.entries.forEach { category ->
            val toolsInCategory = pdfTools.filter { it.category == category }

            if (toolsInCategory.isNotEmpty()) {
                // Section label
                SectionLabel(
                    text = stringResource(category.titleRes),
                    delay = animationIndex * 50
                )
                animationIndex++

                Spacer(modifier = Modifier.height(8.dp))

                // Tool items
                toolsInCategory.forEach { tool ->
                    ToolItem(
                        tool = tool,
                        accentColor = category.accentColor,
                        animationDelay = animationIndex * 50,
                        onClick = {
                            when (tool.id) {
                                "merge" -> navController.navigateToMergeTool()
                                "split" -> navController.navigateToSplitTool("")
                                "compress" -> navController.navigateToCompressTool("")
                                "rotate" -> navController.navigateToRotateTool("")
                                "reorder" -> navController.navigateToReorderTool("")
                                "lock_pdf" -> navController.navigateToLockTool("")
                                "unlock_pdf" -> navController.navigateToUnlockTool("")
                                "remove_pages" -> navController.navigateToRemovePagesTool("")
                                "watermark" -> navController.navigateToWatermarkTool("")
                                "page_numbers" -> navController.navigateToPageNumbersTool("")
                                "img_to_pdf" -> navController.navigateToImageToPdfTool()
                                "pdf_to_img" -> navController.navigateToPdfToImageTool("")
                            }
                        }
                    )
                    animationIndex++

                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(
    text: String,
    delay: Int,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delay.toLong())
        isVisible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(200),
        label = "section alpha"
    )

    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing * 1.5f
        ),
        color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
        modifier = modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun ToolItem(
    tool: PdfTool,
    accentColor: Color,
    animationDelay: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        isVisible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.95f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "tool scale"
    )

    val effectiveAccentColor = if (tool.isEnabled) accentColor else accentColor.copy(alpha = 0.4f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (tool.isEnabled) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = accentColor),
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = effectiveAccentColor.copy(alpha = 0.12f)
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = stringResource(R.string.cd_decorative),
                    modifier = Modifier
                        .padding(10.dp)
                        .size(24.dp),
                    tint = effectiveAccentColor
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(tool.nameRes),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = if (tool.isEnabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        }
                    )

                    // Coming Soon badge
                    if (!tool.isEnabled) {
                        ComingSoonBadge()
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = stringResource(tool.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (tool.isEnabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    }
                )
            }

            // Arrow for enabled tools
            if (tool.isEnabled) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.cd_decorative),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun ComingSoonBadge() {
    Box(
        modifier = Modifier
            .background(
                color = AccentAmber.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = stringResource(R.string.coming_soon),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = AccentAmber
        )
    }
}
