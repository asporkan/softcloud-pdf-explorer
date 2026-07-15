package com.rejowan.pdfreaderpro.presentation.screens.tools.reorder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.pdfreaderpro.R
import com.rejowan.pdfreaderpro.domain.repository.PdfToolsRepository
import com.rejowan.pdfreaderpro.util.FileOperations
import com.rejowan.pdfreaderpro.util.passwordProtectedBlockMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

data class PageItem(
    val originalIndex: Int,  // Original page index (0-based)
    val pageNumber: Int,     // Display number (1-based)
    val thumbnail: Bitmap?
)

data class SourceFile(
    val uri: Uri,
    val path: String,
    val name: String,
    val size: Long,
    val pageCount: Int = 0
)

data class ReorderState(
    val sourceFile: SourceFile? = null,
    val pages: List<PageItem> = emptyList(),
    val outputFileName: String = "",
    val overwriteOriginal: Boolean = false,
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val progress: Float = 0f,
    val error: String? = null,
    val result: ReorderResult? = null,
    val hasChanges: Boolean = false
)

data class ReorderResult(
    val outputPath: String,
    val displayName: String,
    val pageCount: Int,
    val fileSize: Long
)

class ReorderViewModel(
    private val pdfToolsRepository: PdfToolsRepository,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ReorderState())
    val state: StateFlow<ReorderState> = _state.asStateFlow()

    private var originalOrder: List<Int> = emptyList()

    /** In-flight thumbnail requests (0-based original indices). */
    private val thumbnailRequests = mutableSetOf<Int>()

    /** LRU of rendered thumbnails to bound memory for large PDFs. */
    private val thumbnailCache = object : LinkedHashMap<Int, Bitmap>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Bitmap>?): Boolean {
            if (size <= MAX_CACHED_THUMBNAILS) return false
            val index = eldest?.key ?: return false
            val bitmap = eldest.value
            _state.update { current ->
                current.copy(
                    pages = current.pages.map { page ->
                        if (page.originalIndex == index) page.copy(thumbnail = null) else page
                    }
                )
            }
            if (!bitmap.isRecycled) bitmap.recycle()
            return true
        }
    }

    fun setSourceFile(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val path = withContext(Dispatchers.IO) { copyUriToCache(uri) }
                if (path == null) {
                    _state.update {
                        it.copy(isLoading = false, error = context.getString(R.string.tool_error_load_pdf))
                    }
                    return@launch
                }

                pdfToolsRepository.passwordProtectedBlockMessage(context, path)?.let { msg ->
                    File(path).delete()
                    _state.update {
                        it.copy(isLoading = false, sourceFile = null, error = msg)
                    }
                    return@launch
                }

                val loaded = withContext(Dispatchers.IO) {
                    val file = File(path)
                    val (pageCount, pages) = loadPagePlaceholders(path)
                    Triple(path, file, pageCount to pages)
                }
                val (loadedPath, file, pageData) = loaded
                val (pageCount, pages) = pageData

                    synchronized(thumbnailCache) { thumbnailCache.clear() }
                    thumbnailRequests.clear()
                    originalOrder = pages.map { it.originalIndex }

                    _state.update {
                        it.copy(
                            sourceFile = SourceFile(
                                uri = uri,
                                path = loadedPath,
                                name = getFileNameFromUri(uri) ?: file.name,
                                size = file.length(),
                                pageCount = pageCount
                            ),
                            pages = pages,
                            isLoading = false,
                            error = null,
                            result = null,
                            hasChanges = false
                        )
                    }

                    val baseName = file.nameWithoutExtension
                    _state.update { it.copy(outputFileName = "${baseName}_reordered") }

                    // Prefetch first screenful so the grid is not empty
                    pages.take(PREFETCH_THUMBNAILS).forEach { ensureThumbnail(it.originalIndex) }
            } catch (e: Exception) {
                Timber.e(e, "Failed to set source file")
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = context.getString(
                            R.string.tool_error_load_pdf_detail,
                            e.message ?: ""
                        )
                    )
                }
            }
        }
    }

    /**
     * Builds placeholders for every page. Thumbnails are loaded on demand —
     * the previous hard cap of 100 was intentional memory protection, not lazy
     * loading, and truncated both the UI and the save order for large PDFs.
     */
    private fun loadPagePlaceholders(pdfPath: String): Pair<Int, List<PageItem>> {
        ParcelFileDescriptor.open(File(pdfPath), ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                val pageCount = renderer.pageCount
                val pages = List(pageCount) { i ->
                    PageItem(originalIndex = i, pageNumber = i + 1, thumbnail = null)
                }
                return pageCount to pages
            }
        }
    }

    fun ensureThumbnail(originalIndex: Int) {
        val existing = _state.value.pages.find { it.originalIndex == originalIndex }
        if (existing == null || existing.thumbnail != null) return
        if (!thumbnailRequests.add(originalIndex)) return

        val path = _state.value.sourceFile?.path ?: run {
            thumbnailRequests.remove(originalIndex)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bitmap = renderThumbnail(path, originalIndex) ?: return@launch
                synchronized(thumbnailCache) {
                    thumbnailCache[originalIndex] = bitmap
                }
                _state.update { current ->
                    current.copy(
                        pages = current.pages.map { page ->
                            if (page.originalIndex == originalIndex) {
                                page.copy(thumbnail = bitmap)
                            } else page
                        }
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to render thumbnail for page $originalIndex")
            } finally {
                thumbnailRequests.remove(originalIndex)
            }
        }
    }

    private fun renderThumbnail(pdfPath: String, pageIndex: Int): Bitmap? {
        ParcelFileDescriptor.open(File(pdfPath), ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                if (pageIndex !in 0 until renderer.pageCount) return null
                renderer.openPage(pageIndex).use { page ->
                    val thumbnailSize = 300
                    val aspectRatio = page.width.toFloat() / page.height.toFloat()
                    val width: Int
                    val height: Int
                    if (aspectRatio > 1) {
                        width = thumbnailSize
                        height = (thumbnailSize / aspectRatio).toInt().coerceAtLeast(1)
                    } else {
                        height = thumbnailSize
                        width = (thumbnailSize * aspectRatio).toInt().coerceAtLeast(1)
                    }
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    return bitmap
                }
            }
        }
    }

    fun movePage(fromIndex: Int, toIndex: Int) {
        _state.update { current ->
            val pages = current.pages.toMutableList()
            val item = pages.removeAt(fromIndex)
            pages.add(toIndex, item)

            // Check if order has changed from original
            val currentOrder = pages.map { it.originalIndex }
            val hasChanges = currentOrder != originalOrder

            current.copy(pages = pages, hasChanges = hasChanges)
        }
    }

    companion object {
        private const val MAX_CACHED_THUMBNAILS = 48
        private const val PREFETCH_THUMBNAILS = 24
    }

    fun setOutputFileName(name: String) {
        _state.update { it.copy(outputFileName = name) }
    }

    fun setOverwriteOriginal(overwrite: Boolean) {
        _state.update { it.copy(overwriteOriginal = overwrite) }
    }

    /**
     * Entry from Save: overwrite writes back to the selected document URI;
     * Save As is handled by the screen via CreateDocument then [reorderToUri].
     */
    fun reorder() {
        val currentState = _state.value
        if (!validateReorder(currentState)) return

        if (!currentState.overwriteOriginal) {
            // Screen must launch CreateDocument; nothing to do here.
            return
        }

        reorderOverwrite()
    }

    fun reorderToUri(destinationUri: Uri) {
        val currentState = _state.value
        if (!validateReorder(currentState, requireFileName = false)) return

        val sourceFile = currentState.sourceFile ?: return
        val newOrder = currentState.pages.map { it.originalIndex + 1 }

        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, progress = 0f, error = null) }

            val tempFile = File(context.cacheDir, "reorder_out_${System.currentTimeMillis()}.pdf")
            val result = pdfToolsRepository.reorderPages(
                inputPath = sourceFile.path,
                outputPath = tempFile.absolutePath,
                newOrder = newOrder,
                onProgress = { progress -> _state.update { it.copy(progress = progress) } }
            )

            result.fold(
                onSuccess = {
                    if (!FileOperations.writeFileToUri(context, tempFile, destinationUri)) {
                        tempFile.delete()
                        _state.update {
                            it.copy(
                                isProcessing = false,
                                error = context.getString(R.string.tool_error_replace_original)
                            )
                        }
                        return@launch
                    }

                    // Local copy for in-app open/share (destination URI is the real output)
                    val localCopy = File(
                        context.cacheDir,
                        "reorder_result_${System.currentTimeMillis()}.pdf"
                    )
                    tempFile.copyTo(localCopy, overwrite = true)
                    tempFile.delete()

                    finishReorderSuccess(
                        localPath = localCopy.absolutePath,
                        displayName = getFileNameFromUri(destinationUri)
                            ?: currentState.outputFileName.ifBlank { sourceFile.name }
                    )
                },
                onFailure = { error ->
                    tempFile.delete()
                    Timber.e(error, "Reorder failed")
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            error = error.message
                                ?: context.getString(R.string.tool_error_reorder_failed)
                        )
                    }
                }
            )
        }
    }

    private fun reorderOverwrite() {
        val currentState = _state.value
        val sourceFile = currentState.sourceFile ?: return
        val newOrder = currentState.pages.map { it.originalIndex + 1 }

        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, progress = 0f, error = null) }

            // Must write to a temp file first: iText cannot read+write the same path,
            // and content:// originals are not filesystem paths.
            val tempFile = File(context.cacheDir, "reorder_overwrite_${System.currentTimeMillis()}.pdf")
            val result = pdfToolsRepository.reorderPages(
                inputPath = sourceFile.path,
                outputPath = tempFile.absolutePath,
                newOrder = newOrder,
                onProgress = { progress -> _state.update { it.copy(progress = progress) } }
            )

            result.fold(
                onSuccess = {
                    if (!FileOperations.writeFileToUri(context, tempFile, sourceFile.uri)) {
                        tempFile.delete()
                        _state.update {
                            it.copy(
                                isProcessing = false,
                                error = context.getString(R.string.tool_error_replace_original)
                            )
                        }
                        return@launch
                    }

                    // Keep working cache in sync with the overwritten original for Reader
                    try {
                        tempFile.copyTo(File(sourceFile.path), overwrite = true)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to refresh cache after overwrite")
                    }
                    tempFile.delete()

                    finishReorderSuccess(
                        localPath = sourceFile.path,
                        displayName = sourceFile.name
                    )
                },
                onFailure = { error ->
                    tempFile.delete()
                    Timber.e(error, "Reorder failed")
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            error = error.message
                                ?: context.getString(R.string.tool_error_reorder_failed)
                        )
                    }
                }
            )
        }
    }

    private fun validateReorder(
        currentState: ReorderState,
        requireFileName: Boolean = true
    ): Boolean {
        if (currentState.sourceFile == null) {
            _state.update { it.copy(error = context.getString(R.string.tool_error_select_pdf_first)) }
            return false
        }
        if (requireFileName &&
            !currentState.overwriteOriginal &&
            currentState.outputFileName.isBlank()
        ) {
            _state.update { it.copy(error = context.getString(R.string.tool_error_enter_output_name)) }
            return false
        }
        if (!currentState.hasChanges) {
            _state.update { it.copy(error = context.getString(R.string.tool_error_no_reorder_changes)) }
            return false
        }
        return true
    }

    private suspend fun finishReorderSuccess(localPath: String, displayName: String) {
        val outputFile = File(localPath)
        val pageCount = pdfToolsRepository.getPageCount(localPath).getOrDefault(0)
        val name = when {
            displayName.endsWith(".pdf", ignoreCase = true) -> displayName
            displayName.isNotBlank() -> "$displayName.pdf"
            else -> outputFile.name
        }
        _state.update {
            it.copy(
                isProcessing = false,
                progress = 1f,
                result = ReorderResult(
                    outputPath = localPath,
                    displayName = name,
                    pageCount = pageCount,
                    fileSize = outputFile.length()
                )
            )
        }
    }

    fun resetOrder() {
        _state.update { current ->
            val sortedPages = current.pages.sortedBy { it.originalIndex }
            current.copy(pages = sortedPages, hasChanges = false)
        }
    }

    fun clearResult() {
        _state.update { it.copy(result = null) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun reset() {
        synchronized(thumbnailCache) {
            thumbnailCache.values.forEach { if (!it.isRecycled) it.recycle() }
            thumbnailCache.clear()
        }
        thumbnailRequests.clear()
        originalOrder = emptyList()
        _state.update { ReorderState() }
    }

    private fun copyUriToCache(uri: Uri): String? {
        return try {
            if (uri.scheme == "file") {
                return uri.path
            }

            val fileName = getFileNameFromUri(uri) ?: "temp_${System.currentTimeMillis()}.pdf"
            val cacheFile = File(context.cacheDir, "reorder_temp/$fileName")
            cacheFile.parentFile?.mkdirs()

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                cacheFile.outputStream().use { output ->
                    inputStream.copyTo(output)
                }
            } ?: return null

            cacheFile.absolutePath
        } catch (e: Exception) {
            Timber.e(e, "Failed to copy URI to cache")
            null
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    cursor.getString(nameIndex)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
