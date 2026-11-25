package com.rejowan.pdfreaderpro.presentation.screens.tools.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.content.edit
import androidx.documentfile.provider.DocumentFile
import com.rejowan.pdfreaderpro.R
import com.rejowan.pdfreaderpro.util.SafOutputHelper
import timber.log.Timber

/**
 * Remembers the folder PDF tool outputs are saved to (persistable SAF tree grant).
 *
 * Public API for other screens (e.g. Settings): [getFolder], [setFolder], [clear],
 * [askEveryTime], [folderLabel].
 */
class OutputFolderStore(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** When true the user is asked for a destination on every save; the folder is only a starting point. */
    var askEveryTime: Boolean
        get() = prefs.getBoolean(KEY_ASK, false)
        set(value) = prefs.edit { putBoolean(KEY_ASK, value) }

    /**
     * Remembered folder tree URI, or null when none is stored. A folder that was deleted or whose
     * permission was revoked is cleared silently and reported as null.
     */
    fun getFolder(): Uri? {
        val uri = prefs.getString(KEY_TREE, null)?.let { Uri.parse(it) } ?: return null
        if (isUsable(uri)) return uri
        clear()
        return null
    }

    /** Stores [treeUri] (an OpenDocumentTree result) and keeps a persistable read/write grant on it. */
    fun setFolder(treeUri: Uri) {
        val previous = prefs.getString(KEY_TREE, null)?.let { Uri.parse(it) }
        try {
            appContext.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (e: Exception) {
            Timber.w(e, "Persistable tree permission not granted for $treeUri")
        }
        if (previous != null && previous != treeUri) releaseQuietly(previous)
        prefs.edit { putString(KEY_TREE, treeUri.toString()) }
    }

    /** Forgets the remembered folder (and releases its grant). */
    fun clear() {
        prefs.getString(KEY_TREE, null)?.let { releaseQuietly(Uri.parse(it)) }
        prefs.edit { remove(KEY_TREE) }
    }

    /** Readable folder name (e.g. `Download/Belgeler`), or null when no usable folder is stored. */
    fun folderLabel(): String? {
        val folder = getFolder() ?: return null
        return SafOutputHelper.readableFolder(folder, appContext.getString(R.string.tools_output_root))
    }

    /** Remembers the folder of a document saved through CreateDocument (starting point only, no grant). */
    fun rememberSavedDocument(documentUri: Uri) {
        val parent = SafOutputHelper.parentFolderDocumentUri(documentUri) ?: return
        prefs.edit { putString(KEY_HINT, parent.toString()) }
    }

    /** Folder document URI to pass as EXTRA_INITIAL_URI, or null. */
    fun initialUri(): Uri? {
        prefs.getString(KEY_TREE, null)?.let { tree ->
            val uri = Uri.parse(tree)
            return try {
                DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
            } catch (e: Exception) {
                null
            }
        }
        return prefs.getString(KEY_HINT, null)?.let { Uri.parse(it) }
    }

    private fun isUsable(uri: Uri): Boolean {
        val granted = appContext.contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission && it.isWritePermission
        }
        if (!granted) return false
        return try {
            val doc = DocumentFile.fromTreeUri(appContext, uri)
            doc != null && doc.exists() && doc.isDirectory && doc.canWrite()
        } catch (e: Exception) {
            false
        }
    }

    private fun releaseQuietly(uri: Uri) {
        try {
            appContext.contentResolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (e: Exception) {
            Timber.w(e, "Release persistable permission failed for $uri")
        }
    }

    private companion object {
        const val PREFS = "tool_output_folder"
        const val KEY_TREE = "tree_uri"
        const val KEY_HINT = "hint_uri"
        const val KEY_ASK = "ask_every_time"
    }
}
