package com.rejowan.pdfreaderpro.util

import android.net.Uri
import com.rejowan.pdfreaderpro.domain.model.PdfFile
import java.io.File

/**
 * Builds a [PdfFile] straight from the file system for paths that are not (yet) in MediaStore,
 * e.g. PDFs opened from other apps. Returns null when the file no longer exists.
 * Dates are in milliseconds, matching PdfFileRepositoryImpl.
 */
fun pdfFileFromDisk(path: String): PdfFile? {
    val file = File(path)
    if (!file.isFile) return null
    return PdfFile(
        id = path.hashCode().toLong(),
        name = file.name,
        path = file.absolutePath,
        uri = Uri.fromFile(file),
        size = file.length(),
        dateModified = file.lastModified(),
        dateAdded = file.lastModified(),
        parentFolder = file.parent.orEmpty()
    )
}
