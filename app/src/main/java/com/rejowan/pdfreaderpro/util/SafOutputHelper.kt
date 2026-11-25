package com.rejowan.pdfreaderpro.util

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import timber.log.Timber
import java.io.File

/**
 * SAF helpers used by the PDF tools: output creation, cleanup of orphan targets,
 * readable names and best-effort media scanning. Does not touch [FileOperations].
 */
object SafOutputHelper {

    private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
    private val INVALID_NAME_CHARS = Regex("[\\\\/:*?\"<>|]")

    /** DISPLAY_NAME of a content/file URI, or null. */
    fun displayName(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") return uri.lastPathSegment
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (e: Exception) {
            Timber.w(e, "displayName failed for $uri")
            null
        }
    }

    /** Deletes a document created for an operation that then failed. Never throws. */
    fun deleteDocumentQuietly(context: Context, uri: Uri?) {
        if (uri == null) return
        try {
            when (uri.scheme) {
                "content" -> DocumentsContract.deleteDocument(context.contentResolver, uri)
                "file" -> uri.path?.let { File(it).delete() }
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to delete orphan output $uri")
        }
    }

    /** Creates an empty child of [mimeType] inside a persisted folder tree. Name conflicts get a provider-made suffix. */
    fun createInFolder(context: Context, treeUri: Uri, mimeType: String, fileName: String): Uri? {
        return try {
            val tree = DocumentFile.fromTreeUri(context, treeUri)
            if (tree == null || !tree.canWrite()) return null
            val base = fileName.substringBeforeLast('.', fileName).ifBlank { "document" }
            tree.createFile(mimeType, base)?.uri
        } catch (e: Exception) {
            Timber.e(e, "createInFolder failed for $fileName")
            null
        }
    }

    /**
     * Copies [files] into new children of the folder tree. Returns the created URIs, or null on
     * failure (already created children are removed so no orphans remain).
     */
    fun writeFilesToFolder(
        context: Context,
        treeUri: Uri,
        files: List<File>,
        mimeType: String
    ): List<Uri>? {
        if (files.isEmpty()) return null
        val created = mutableListOf<Uri>()
        for (file in files) {
            val dest = if (file.exists()) createInFolder(context, treeUri, mimeType, file.name) else null
            if (dest == null || !FileOperations.writeFileToUri(context, file, dest)) {
                deleteDocumentQuietly(context, dest)
                created.forEach { deleteDocumentQuietly(context, it) }
                return null
            }
            created += dest
        }
        return created
    }

    /**
     * Copy of [tempFile] in the app cache whose file name is the real saved name, so the reader
     * and result cards show the name the user sees in their file manager.
     */
    fun namedLocalCopy(context: Context, tempFile: File, displayName: String): File {
        val safeName = displayName.replace(INVALID_NAME_CHARS, "_").ifBlank { tempFile.name }
        val dir = File(context.cacheDir, "tool_results/${System.currentTimeMillis()}")
        dir.mkdirs()
        val target = File(dir, safeName)
        tempFile.copyTo(target, overwrite = true)
        return target
    }

    /** Document id such as `primary:Download/Sub/a.pdf` for external-storage URIs, else null. */
    private fun externalDocumentId(uri: Uri): String? {
        return try {
            if (uri.authority != EXTERNAL_STORAGE_AUTHORITY) return null
            val segments = uri.pathSegments
            if ("document" in segments) DocumentsContract.getDocumentId(uri)
            else if ("tree" in segments) DocumentsContract.getTreeDocumentId(uri)
            else null
        } catch (e: Exception) {
            null
        }
    }

    /** Document URI of the folder that contains [documentUri] (external storage only). */
    fun parentFolderDocumentUri(documentUri: Uri): Uri? {
        val id = externalDocumentId(documentUri) ?: return null
        val slash = id.lastIndexOf('/')
        if (slash < 0) return null
        return DocumentsContract.buildDocumentUri(EXTERNAL_STORAGE_AUTHORITY, id.substring(0, slash))
    }

    /** Human readable folder path for a tree or document URI, e.g. `Download/Belgeler`. */
    fun readableFolder(uri: Uri, rootLabel: String): String {
        val id = externalDocumentId(uri)
            ?: return uri.lastPathSegment?.substringAfterLast(':')?.ifBlank { null } ?: rootLabel
        val path = id.substringAfter(':', "")
        return path.ifBlank { rootLabel }
    }

    /** Readable folder that holds the saved [documentUri] (falls back to [rootLabel]). */
    fun readableParentOf(documentUri: Uri, rootLabel: String): String {
        val parent = parentFolderDocumentUri(documentUri) ?: return rootLabel
        return readableFolder(parent, rootLabel)
    }

    private fun externalFilePath(uri: Uri): String? {
        val id = externalDocumentId(uri) ?: return null
        if (!id.startsWith("primary:")) return null
        return File(Environment.getExternalStorageDirectory(), id.substringAfter(':')).absolutePath
    }

    /** Best-effort MediaStore scan so the saved output shows up in lists. Skips URIs without a file path. */
    fun scanSaved(context: Context, uris: List<Uri>, mimeType: String) {
        val paths = uris.mapNotNull { externalFilePath(it) }
        if (paths.isEmpty()) return
        try {
            MediaScannerConnection.scanFile(
                context.applicationContext,
                paths.toTypedArray(),
                Array(paths.size) { mimeType },
                null
            )
        } catch (e: Exception) {
            Timber.w(e, "Media scan failed")
        }
    }
}
