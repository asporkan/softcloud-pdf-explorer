package com.rejowan.pdfreaderpro.presentation.screens.tools.pdftoimage

import timber.log.Timber
import java.io.File

/**
 * File naming for the PDF to Images tool. The repository writes `<source>_page<N>.<ext>`; the
 * user-visible name is `<prefix>_<N>.<ext>`, where the prefix defaults to the source file name.
 */
internal object ImageOutputNaming {

    private val INVALID_CHARS = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")
    private val PAGE_SUFFIX = Regex("_page(\\d+)\\.(png|jpg)$", RegexOption.IGNORE_CASE)
    private const val MAX_PREFIX_LENGTH = 100
    private const val FALLBACK_PREFIX = "image"

    /** Removes characters that are not allowed in file names. Safe to call on every keystroke. */
    fun stripInvalid(raw: String): String = raw.replace(INVALID_CHARS, "")

    /** Final, ready-to-use prefix: invalid characters removed, trimmed, no leading/trailing dots. */
    fun sanitize(raw: String): String =
        stripInvalid(raw).trim().trim('.').take(MAX_PREFIX_LENGTH).trim().trim('.')

    /** Source file name without its extension, cleaned for use as a prefix. */
    fun defaultPrefix(sourceName: String): String = sanitize(File(sourceName).nameWithoutExtension)

    /** The prefix actually used for export: what the user typed, else the source name, else "image". */
    fun resolvePrefix(typed: String, sourceName: String): String =
        sanitize(typed).ifEmpty { defaultPrefix(sourceName) }.ifEmpty { FALLBACK_PREFIX }

    fun fileName(prefix: String, pageNumber: Int, extension: String): String =
        "${prefix}_$pageNumber.$extension"

    fun pageNumberOf(fileName: String): Int? =
        PAGE_SUFFIX.find(fileName)?.groupValues?.get(1)?.toIntOrNull()

    /**
     * Renames repository output (`<source>_page<N>.<ext>`) to `<prefix>_<N>.<ext>` and returns the
     * resulting paths in the same order. A file that cannot be renamed keeps its original path.
     */
    fun applyPrefix(paths: List<String>, prefix: String): List<String> = paths.map { path ->
        val file = File(path)
        val page = pageNumberOf(file.name) ?: return@map path
        val extension = file.extension.lowercase()
        val target = File(file.parentFile, fileName(prefix, page, extension))
        when {
            target.absolutePath == file.absolutePath -> path
            target.exists() -> {
                Timber.w("Prefix rename skipped, target exists: %s", target.name)
                path
            }
            file.renameTo(target) -> target.absolutePath
            else -> {
                Timber.w("Prefix rename failed for %s", file.name)
                path
            }
        }
    }
}
