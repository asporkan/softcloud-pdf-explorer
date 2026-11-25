package com.rejowan.pdfreaderpro.data.repository

import android.app.Activity
import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.rejowan.pdfreaderpro.domain.model.PdfFile
import com.rejowan.pdfreaderpro.domain.model.PdfFolder
import com.rejowan.pdfreaderpro.domain.repository.PdfFileRepository
import com.rejowan.pdfreaderpro.util.FileOperations
import com.rejowan.pdfreaderpro.util.PdfThumbnailManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

class PdfFileRepositoryImpl(
    private val context: Context
) : PdfFileRepository {

    private val _pdfFiles = MutableStateFlow<List<PdfFile>>(emptyList())

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshMutex = Mutex()

    // PdfRenderer is slow; reuse the page count while size and modified time are unchanged.
    private data class CachedPageCount(val size: Long, val dateModified: Long, val pages: Int)

    private val pageCountCache = ConcurrentHashMap<String, CachedPageCount>()

    private var isObserving = false
    private var debounceJob: Job? = null
    private val startedActivities = AtomicInteger(0)

    @Volatile
    private var changedWhileBackground = false

    override fun getAllPdfFiles(): Flow<List<PdfFile>> = _pdfFiles

    override fun getPdfsByFolder(folderPath: String): Flow<List<PdfFile>> {
        return _pdfFiles.map { files ->
            files.filter { it.parentFolder == folderPath }
        }
    }

    override fun searchPdfs(query: String): Flow<List<PdfFile>> {
        return _pdfFiles.map { files ->
            if (query.isBlank()) {
                emptyList()
            } else {
                files.filter {
                    it.name.contains(query, ignoreCase = true)
                }
            }
        }
    }

    override fun getPdfFolders(): Flow<List<PdfFolder>> {
        return _pdfFiles.map { files ->
            files.groupBy { it.parentFolder }
                .map { (path, pdfs) ->
                    PdfFolder(
                        path = path,
                        name = path.substringAfterLast("/").ifEmpty { "Storage" },
                        pdfCount = pdfs.size
                    )
                }
                .sortedBy { it.name.lowercase() }
        }
    }

    override suspend fun getPdfFile(path: String): PdfFile? {
        return _pdfFiles.value.find { it.path == path }
    }

    override suspend fun renamePdf(path: String, newName: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val newPath = FileOperations.renameFile(path, newName)
                    ?: return@withContext Result.failure(IOException("Failed to rename file"))
                // From Android 11 the FUSE layer updates MediaStore on rename; older versions need a scan.
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    scanPaths(arrayOf(path, newPath))
                }
                refreshPdfs()
                Result.success(newPath)
            } catch (e: Exception) {
                Timber.e(e, "Error renaming PDF")
                Result.failure(e)
            }
        }
    }

    override suspend fun deletePdf(path: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            val result = deleteFromStorage(path)
            if (result.isSuccess) removeFromList(setOf(path))
            refreshPdfs()
            result
        }
    }

    override suspend fun deletePdfs(paths: List<String>): List<Result<Unit>> {
        return withContext(Dispatchers.IO) {
            val results = paths.map { deleteFromStorage(it) }
            removeFromList(paths.filterIndexed { i, _ -> results[i].isSuccess }.toSet())
            refreshPdfs()
            results
        }
    }

    /** File API first, then MediaStore. Never throws; failures are returned to the caller. */
    private fun deleteFromStorage(path: String): Result<Unit> {
        val file = File(path)
        return try {
            when {
                !file.exists() -> {
                    // Already gone: only a stale MediaStore row can be left, so clean it up.
                    deleteMediaStoreRows(path)
                    Result.success(Unit)
                }
                file.delete() -> {
                    deleteMediaStoreRows(path)
                    Result.success(Unit)
                }
                else -> {
                    deleteMediaStoreRows(path, rethrowSecurity = true)
                    if (!file.exists()) {
                        Result.success(Unit)
                    } else {
                        Result.failure(IOException("Failed to delete file"))
                    }
                }
            }
        } catch (e: SecurityException) {
            // Includes RecoverableSecurityException (Android 10+): the app has no right to remove this file.
            Timber.e(e, "No permission to delete PDF")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting PDF")
            Result.failure(e)
        }
    }

    /** Removes MediaStore rows whose `_data` is [path]. Returns the number of removed rows. */
    private fun deleteMediaStoreRows(path: String, rethrowSecurity: Boolean = false): Int {
        return try {
            context.contentResolver.delete(
                filesCollection(),
                "${MediaStore.Files.FileColumns.DATA} = ?",
                arrayOf(path)
            )
        } catch (e: SecurityException) {
            if (rethrowSecurity) throw e
            Timber.w(e, "MediaStore row cleanup not permitted for $path")
            0
        } catch (e: Exception) {
            Timber.w(e, "MediaStore row cleanup failed for $path")
            0
        }
    }

    private fun removeFromList(paths: Set<String>) {
        if (paths.isEmpty()) return
        _pdfFiles.update { files -> files.filterNot { it.path in paths } }
    }

    private suspend fun scanPaths(paths: Array<String>) {
        try {
            withTimeoutOrNull(SCAN_TIMEOUT_MS) {
                suspendCancellableCoroutine { cont ->
                    val remaining = AtomicInteger(paths.size)
                    MediaScannerConnection.scanFile(context, paths, null) { _, _ ->
                        if (remaining.decrementAndGet() == 0 && cont.isActive) cont.resume(Unit)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "MediaStore scan failed")
        }
    }

    override suspend fun refreshPdfs() {
        ensureObservingChanges()
        withContext(Dispatchers.IO) {
            refreshMutex.withLock {
                // A failed query keeps the current list instead of blanking the screen.
                queryPdfFiles()?.let { pdfs ->
                    _pdfFiles.value = pdfs
                    Timber.d("Refreshed PDFs: ${pdfs.size} files found")
                }
            }
        }
    }

    /**
     * Listens to MediaStore so files created by tools or other apps show up without a manual refresh.
     * Battery friendly: changes are debounced, and while no activity is started they only set a flag that is
     * handled when the app returns to the foreground.
     */
    private fun ensureObservingChanges() {
        if (isObserving) return
        isObserving = true
        try {
            // Registration happens while the UI is on screen, so assume one started activity.
            startedActivities.set(1)
            (context.applicationContext as? Application)?.registerActivityLifecycleCallbacks(activityCallbacks)
            context.contentResolver.registerContentObserver(
                filesCollection(),
                true,
                object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean) = scheduleRefresh()
                }
            )
        } catch (e: Exception) {
            Timber.w(e, "Could not observe MediaStore changes")
        }
    }

    @Synchronized
    private fun scheduleRefresh() {
        if (startedActivities.get() <= 0) {
            changedWhileBackground = true
            return
        }
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(OBSERVER_DEBOUNCE_MS)
            refreshPdfs()
        }
    }

    private val activityCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityStarted(activity: Activity) {
            if (startedActivities.incrementAndGet() == 1 && changedWhileBackground) {
                changedWhileBackground = false
                scheduleRefresh()
            }
        }

        override fun onActivityStopped(activity: Activity) {
            startedActivities.decrementAndGet()
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }

    private fun filesCollection(): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }
    }

    /** Returns the PDFs found in MediaStore, or null when the query itself failed. */
    private fun queryPdfFiles(): List<PdfFile>? {
        val pdfList = mutableListOf<PdfFile>()

        val collection = filesCollection()

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.DATE_ADDED
        )

        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
        val selectionArgs = arrayOf("application/pdf")
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Unknown.pdf"
                    val path = cursor.getString(pathColumn) ?: continue
                    val size = cursor.getLong(sizeColumn)
                    val dateModified = cursor.getLong(dateModifiedColumn) * 1000
                    val dateAdded = cursor.getLong(dateAddedColumn) * 1000

                    // Skip if file doesn't exist
                    if (!File(path).exists()) continue

                    val uri = ContentUris.withAppendedId(collection, id)
                    val parentFolder = File(path).parent ?: ""
                    val cached = pageCountCache[path]
                    val pageCount = if (cached != null && cached.size == size && cached.dateModified == dateModified) {
                        cached.pages
                    } else {
                        PdfThumbnailManager.getPageCount(path)
                            .also { pageCountCache[path] = CachedPageCount(size, dateModified, it) }
                    }

                    pdfList.add(
                        PdfFile(
                            id = id,
                            name = name,
                            path = path,
                            uri = uri,
                            size = size,
                            dateModified = dateModified,
                            dateAdded = dateAdded,
                            parentFolder = parentFolder,
                            pageCount = pageCount
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error querying PDF files")
            return null
        }

        val livePaths = pdfList.mapTo(HashSet()) { it.path }
        pageCountCache.keys.retainAll(livePaths)
        return pdfList
    }

    private companion object {
        const val OBSERVER_DEBOUNCE_MS = 1_500L
        const val SCAN_TIMEOUT_MS = 3_000L
    }
}
