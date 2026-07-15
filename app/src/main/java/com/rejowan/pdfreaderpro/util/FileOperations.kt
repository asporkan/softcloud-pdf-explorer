package com.rejowan.pdfreaderpro.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

object FileOperations {

    private const val SHARED_PDF_CACHE_DIR = "shared_pdfs"
    private const val MAX_CACHE_AGE_MS = 14 * 24 * 60 * 60 * 1000L // 14 days

    /**
     * Copies a content URI to the app's cache directory.
     * Uses MD5 verification to avoid duplicating identical files.
     * Returns the local file path, or null if copy failed.
     */
    fun copyContentUriToCache(context: Context, uri: Uri): String? {
        return try {
            val fileName = getFileNameFromUri(context, uri) ?: "shared_${System.currentTimeMillis()}.pdf"
            val cacheDir = File(context.cacheDir, SHARED_PDF_CACHE_DIR).apply { mkdirs() }
            val destFile = File(cacheDir, fileName)
            val tempFile = File(cacheDir, "temp_${System.currentTimeMillis()}.pdf")

            // Copy to temp file first
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            val newFileMd5 = calculateMd5(tempFile)

            // Check if existing file with same name has same MD5
            if (destFile.exists()) {
                val existingMd5 = calculateMd5(destFile)
                if (existingMd5 == newFileMd5) {
                    // Same file, reuse existing
                    tempFile.delete()
                    Timber.d("Reusing existing cached file (MD5 match): ${destFile.absolutePath}")
                    return destFile.absolutePath
                }
            }

            // Different file or doesn't exist - overwrite
            tempFile.renameTo(destFile)
            Timber.d("Cached new file: ${destFile.absolutePath}")
            destFile.absolutePath
        } catch (e: Exception) {
            Timber.e(e, "Error copying content URI to cache")
            null
        }
    }

    /**
     * Calculates MD5 hash of a file.
     */
    private fun calculateMd5(file: File): String {
        val md = MessageDigest.getInstance("MD5")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                md.update(buffer, 0, bytesRead)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Gets the display name of a file from a content URI.
     */
    fun getFileNameFromUri(context: Context, uri: Uri): String? {
        return try {
            when (uri.scheme) {
                "content" -> {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (nameIndex >= 0) cursor.getString(nameIndex) else null
                        } else null
                    }
                }
                "file" -> uri.lastPathSegment
                else -> null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting file name from URI")
            null
        }
    }

    /**
     * Cleans up old cached PDFs that are older than MAX_CACHE_AGE_MS.
     */
    fun cleanupOldCachedPdfs(context: Context) {
        try {
            val cacheDir = File(context.cacheDir, SHARED_PDF_CACHE_DIR)
            if (!cacheDir.exists()) return

            val cutoffTime = System.currentTimeMillis() - MAX_CACHE_AGE_MS
            cacheDir.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoffTime) {
                    file.delete()
                    Timber.d("Deleted old cached PDF: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error cleaning up cached PDFs")
        }
    }

    /**
     * Best-effort persistable read/write grant for SAF documents so overwrite can update the original.
     */
    fun takePersistableReadWritePermission(context: Context, uri: Uri) {
        if (uri.scheme != "content") return
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            Timber.w(e, "Persistable URI permission not available; session grants may still allow write")
        } catch (e: Exception) {
            Timber.w(e, "Failed to take persistable URI permission")
        }
    }

    /** Persist read/write access to a folder picked via OpenDocumentTree. */
    fun takePersistableTreePermission(context: Context, treeUri: Uri) {
        if (treeUri.scheme != "content") return
        try {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            Timber.w(e, "Persistable tree permission not available; session grants may still allow write")
        } catch (e: Exception) {
            Timber.w(e, "Failed to take persistable tree permission")
        }
    }

    /** Creates a new document child under a tree URI (OpenDocumentTree result). */
    fun createDocumentInTree(
        context: Context,
        treeUri: Uri,
        mimeType: String,
        displayName: String
    ): Uri? {
        return try {
            // OpenDocumentTree returns a tree URI; createDocument needs the document URI.
            val parentDocumentUri = DocumentsContract.buildDocumentUriUsingTree(
                treeUri,
                DocumentsContract.getTreeDocumentId(treeUri)
            )
            DocumentsContract.createDocument(
                context.contentResolver,
                parentDocumentUri,
                mimeType,
                displayName
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to create document in tree: $displayName")
            null
        }
    }

    /**
     * Writes local files as new documents into a folder chosen via OpenDocumentTree.
     * Never overwrites the source document — always creates new children under the tree.
     */
    fun writeFilesToTree(
        context: Context,
        treeUri: Uri,
        files: List<File>,
        mimeType: String
    ): Boolean {
        if (files.isEmpty()) return false
        val tree = DocumentFile.fromTreeUri(context, treeUri)
        if (tree == null || !tree.canWrite()) {
            Timber.e("Tree URI not writable: $treeUri")
            return false
        }
        for (file in files) {
            if (!file.exists()) {
                Timber.e("Missing output file: ${file.absolutePath}")
                return false
            }
            // Prefer DocumentFile.createFile; fall back to DocumentsContract with correct parent URI.
            val destUri = tree.createFile(mimeType, file.nameWithoutExtension)?.uri
                ?: createDocumentInTree(context, treeUri, mimeType, file.name)
            if (destUri == null) {
                Timber.e("Failed to create child document: ${file.name}")
                return false
            }
            if (!writeFileToUri(context, file, destUri)) return false
        }
        return true
    }

    /** Ensures a user-facing name ends with `.pdf` for CreateDocument suggestions. */
    fun ensurePdfExtension(fileName: String): String {
        val trimmed = fileName.trim().ifBlank { "document" }
        return if (trimmed.endsWith(".pdf", ignoreCase = true)) trimmed else "$trimmed.pdf"
    }

    /**
     * Writes [file] to a SAF/content or file URI (truncating). Used to overwrite originals / Save As.
     */
    fun writeFileToUri(context: Context, file: File, uri: Uri): Boolean {
        return try {
            when (uri.scheme) {
                "file" -> {
                    val target = File(uri.path ?: return false)
                    file.copyTo(target, overwrite = true)
                    true
                }
                "content" -> {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                        file.inputStream().use { input -> input.copyTo(output) }
                    } ?: return false
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to write file to URI: $uri")
            false
        }
    }

    /**
     * Checks if a URI is a content URI that needs to be copied.
     */
    fun isContentUri(uri: Uri): Boolean {
        return uri.scheme == "content"
    }

    /**
     * Resolves a URI to a file path. For content URIs, copies to cache first.
     * Returns the file path or null if resolution failed.
     */
    fun resolveUriToPath(context: Context, uri: Uri): String? {
        return when (uri.scheme) {
            "file" -> uri.path
            "content" -> copyContentUriToCache(context, uri)
            else -> null
        }
    }

    fun sharePdf(context: Context, filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Timber.e("File not found: $filePath")
                return
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(
                Intent.createChooser(shareIntent, "Share PDF")
            )
        } catch (e: Exception) {
            Timber.e(e, "Error sharing file")
        }
    }

    fun shareMultiplePdfs(context: Context, filePaths: List<String>) {
        try {
            val uris = ArrayList<Uri>()
            filePaths.forEach { filePath ->
                val file = File(filePath)
                if (file.exists()) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        file
                    )
                    uris.add(uri)
                }
            }

            if (uris.isEmpty()) {
                Timber.e("No valid files to share")
                return
            }

            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "application/pdf"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(
                Intent.createChooser(shareIntent, "Share ${uris.size} PDFs")
            )
        } catch (e: Exception) {
            Timber.e(e, "Error sharing multiple files")
        }
    }

    /**
     * Renames a file and returns the new file path, or null if rename failed.
     */
    fun renameFile(filePath: String, newName: String): String? {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                Timber.e("File not found: $filePath")
                return null
            }

            val newFileName = if (newName.endsWith(".pdf", ignoreCase = true)) {
                newName
            } else {
                "$newName.pdf"
            }

            val newFile = File(file.parentFile, newFileName)

            if (newFile.exists()) {
                Timber.e("File with name $newFileName already exists")
                return null
            }

            if (file.renameTo(newFile)) {
                newFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error renaming file")
            null
        }
    }

    fun deleteFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                Timber.e("File not found: $filePath")
                return false
            }
            file.delete()
        } catch (e: Exception) {
            Timber.e(e, "Error deleting file")
            false
        }
    }

    fun getFileSize(filePath: String): Long {
        return try {
            File(filePath).length()
        } catch (e: Exception) {
            0L
        }
    }

    fun fileExists(filePath: String): Boolean {
        return try {
            File(filePath).exists()
        } catch (e: Exception) {
            false
        }
    }
}
