package com.rejowan.pdfreaderpro.util

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException

/**
 * Writes a byte array into the public Downloads folder.
 *
 * - API 29+: `MediaStore.Downloads` (works under scoped storage without any storage permission).
 *   `IS_PENDING` hides the row until the bytes are fully written; name collisions get a
 *   MediaStore-made suffix, so the stored name is read back and returned.
 * - API 24-28: plain `File` API (WRITE_EXTERNAL_STORAGE is held there); collisions get a " (n)" suffix.
 *
 * Never throws: failures come back as [Result.failure] so the caller can tell the user.
 */
class DownloadsWriter(private val context: Context) {

    /** Saves [bytes] as [fileName] and returns the name actually stored (may differ on collision). */
    suspend fun save(bytes: ByteArray, fileName: String?, mimeType: String?): Result<String> =
        withContext(Dispatchers.IO) {
            val safeName = sanitizeFileName(fileName)
            val resolvedMime = resolveMimeType(safeName, mimeType)
            try {
                val stored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveWithMediaStore(bytes, safeName, resolvedMime)
                } else {
                    saveLegacy(bytes, safeName, resolvedMime)
                }
                Result.success(stored)
            } catch (e: Exception) {
                Timber.w(e, "Saving $safeName to Downloads failed")
                Result.failure(e)
            }
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveWithMediaStore(bytes: ByteArray, name: String, mimeType: String): String {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("MediaStore insert returned null for $name")
        try {
            resolver.openOutputStream(uri, "w")?.use { it.write(bytes) }
                ?: throw IOException("Could not open output stream for $name")

            val done = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)

            return resolver.query(uri, arrayOf(MediaStore.Downloads.DISPLAY_NAME), null, null, null)
                ?.use { if (it.moveToFirst()) it.getString(0) else null }
                ?.takeIf { it.isNotBlank() }
                ?: name
        } catch (e: Exception) {
            // Do not leave a pending, empty row behind.
            runCatching { resolver.delete(uri, null, null) }
            throw e
        }
    }

    @Suppress("DEPRECATION")
    private fun saveLegacy(bytes: ByteArray, name: String, mimeType: String): String {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!dir.exists() && !dir.mkdirs()) throw IOException("Downloads folder is not available")

        val finalName = uniqueName(name) { File(dir, it).exists() }
        val target = File(dir, finalName)
        try {
            target.writeBytes(bytes)
        } catch (e: Exception) {
            target.delete()
            throw e
        }
        MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf(mimeType), null)
        return finalName
    }

    companion object {
        private const val FALLBACK_BASE = "attachment"
        private val FORBIDDEN_CHARS = Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]")

        /**
         * Reduces an untrusted (PDF-supplied) name to a plain file name: no directory parts, no path
         * traversal, no control or reserved characters. Falls back to "attachment" when nothing is left.
         */
        fun sanitizeFileName(raw: String?): String {
            val leaf = raw.orEmpty().substringAfterLast('/').substringAfterLast('\\')
            val cleaned = leaf.replace(FORBIDDEN_CHARS, "_").trim().trimStart('.').trim()
            return cleaned.ifBlank { FALLBACK_BASE }
        }

        /** MIME type from [declared] if usable, else from the extension of [fileName], else octet-stream. */
        fun resolveMimeType(fileName: String, declared: String?): String {
            declared?.takeIf { it.contains('/') && it.isNotBlank() }?.let { return it }
            val ext = fileName.substringAfterLast('.', "").lowercase()
            val guessed = if (ext.isEmpty()) null else MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            return guessed ?: "application/octet-stream"
        }

        /** "name.ext", then "name (1).ext", "name (2).ext"... until [exists] is false. */
        fun uniqueName(name: String, exists: (String) -> Boolean): String {
            if (!exists(name)) return name
            val base = name.substringBeforeLast('.', name)
            val ext = name.substringAfterLast('.', "").let { if (it.isEmpty()) "" else ".$it" }
            var counter = 1
            while (true) {
                val candidate = "$base ($counter)$ext"
                if (!exists(candidate)) return candidate
                counter++
            }
        }
    }
}
