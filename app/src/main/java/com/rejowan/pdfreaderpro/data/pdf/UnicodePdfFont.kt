package com.rejowan.pdfreaderpro.data.pdf

import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy
import timber.log.Timber
import java.io.File

/**
 * Text stamped into PDFs (watermark, page numbers) must support Turkish and other non-Latin-1
 * characters. The default iText font (Helvetica, WinAnsi) silently drops İ, Ş, Ğ, ı, ...
 * so we embed a subset of a Unicode system font instead and only fall back to Helvetica
 * when no usable system font exists.
 */
internal object UnicodePdfFont {

    /** Static (non-variable) TrueType fonts first; variable fonts are a last resort. */
    internal val CANDIDATES = listOf(
        "/system/fonts/RobotoStatic-Regular.ttf",
        "/system/fonts/NotoSans-Regular.ttf",
        "/system/fonts/DroidSans.ttf",
        "/system/fonts/Roboto-Regular.ttf"
    )

    fun create(candidates: List<String> = CANDIDATES): PdfFont {
        for (path in candidates) {
            if (!File(path).isFile) continue
            try {
                return PdfFontFactory.createFont(
                    path,
                    PdfEncodings.IDENTITY_H,
                    EmbeddingStrategy.FORCE_EMBEDDED
                )
            } catch (e: Exception) {
                Timber.w(e, "Unusable font: %s", path)
            }
        }
        return PdfFontFactory.createFont()
    }
}
