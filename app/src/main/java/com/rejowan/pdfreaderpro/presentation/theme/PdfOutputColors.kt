package com.rejowan.pdfreaderpro.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Colors the user can burn into a generated PDF (page numbers, text watermark).
 *
 * These are document content, not UI chrome, so they intentionally do not follow the
 * app theme. Keeping them here (instead of inline in the tool screens) keeps all literal
 * colors under `presentation/theme/`.
 */
object PdfOutputColors {
    val PageNumberPresets: List<Color> = listOf(
        Color(0xFF000000), // Black
        Color(0xFF424242), // Dark gray
        Color(0xFF757575), // Gray
        Color(0xFFEF5350), // Red
        Color(0xFF42A5F5), // Blue
        Color(0xFF66BB6A), // Green
        Color(0xFFAB47BC), // Purple
    )

    val WatermarkPresets: List<Color> = listOf(
        Color(0xFF808080), // Gray
        Color(0xFFEF5350), // Red
        Color(0xFF42A5F5), // Blue
        Color(0xFF66BB6A), // Green
        Color(0xFFFFCA28), // Amber
        Color(0xFFAB47BC), // Purple
        Color(0xFF000000), // Black
    )

    val WatermarkDefaultText: Color = Color(0xFF808080)

    /** Black or white, whichever has the higher WCAG contrast on [background] (check marks on swatches). */
    fun contentOn(background: Color): Color =
        if (background.luminance() > 0.179f) Color.Black else Color.White
}
