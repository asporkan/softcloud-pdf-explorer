package com.rejowan.pdfreaderpro.presentation.screens.tools.removepages

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

data class PageInfo(
    val pageNumber: Int,
    val thumbnail: Bitmap?,
    val isSelected: Boolean = false  // Selected means marked for removal
)

data class SourceFile(
    val uri: Uri,
    val path: String,
    val name: String,
    val size: Long,
    val pageCount: Int = 0,
    val pages: List<PageInfo> = emptyList()
)

data class RemovePagesState(
    val sourceFile: SourceFile? = null,
    val outputFileName: String = "",
    val overwriteOriginal: Boolean = false,
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val progress: Float = 0f,
    val error: String? = null,
    val result: RemovePagesResult? = null
)

data class RemovePagesResult(
    val outputPath: String,
    val displayName: String,
    val originalPageCount: Int,
    val newPageCount: Int,
    val removedPages: Int,
    val fileSize: Long
)

class RemovePagesViewModel(
    private val pdfToolsRepository: PdfToolsRepository,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(RemovePagesState())
    val state: StateFlow<RemovePagesState> = _state.asStateFlow()

    private val thumbnailRequests = mutableSetOf<Int>()

    private val thumbnailCache = object : LinkedHashMap<Int, Bitmap>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Bitmap>?): Boolean {
            if (size <= MAX_CACHED_THUMBNAILS) return false
            val index = eldest?.key ?: return false
            val bitmap = eldest.value
            _state.update { current ->
                current.copy(
                    sourceFile = current.sourceFile?.copy(
                        pages = current.sourceFile.pages.map { page ->
                            if (page.pageNumber == index + 1) page.copy(thumbnail = null) else page
                        }
                    )
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

                    _state.update {
                        it.copy(
                            sourceFile = SourceFile(
                                uri = uri,
                                path = loadedPath,
                                name = getFileNameFromUri(uri) ?: file.name,
                                size = file.length(),
                                pageCount = pageCount,
                                pages = pages
                            ),
                            isLoading = false,
                            error = null,
                            result = null
                        )
                    }

                    val baseName = file.nameWithoutExtension
                    _state.update { it.copy(outputFileName = "${baseName}_modified") }

                    pages.take(PREFETCH_THUMBNAILS).forEach { ensureThumbnail(it.pageNumber - 1) }
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
     * Placeholders for every page; thumbnails load on demand.
     * Previous hard cap of 100 truncated UI for large PDFs (not lazy loading).
     */
    private fun loadPagePlaceholders(pdfPath: String): Pair<Int, List<PageInfo>> {
        ParcelFileDescriptor.open(File(pdfPath), ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                val pageCount = renderer.pageCount
                val pages = List(pageCount) { i ->
                    PageInfo(pageNumber = i + 1, thumbnail = null, isSelected = false)
                }
                return pageCount to pages
            }
        }
    }

    fun ensureThumbnail(pageIndex: Int) {
        val existing = _state.value.sourceFile?.pages?.find { it.pageNumber == pageIndex + 1 }
        if (existing == null || existing.thumbnail != null) return
        if (!thumbnailRequests.add(pageIndex)) return

        val path = _state.value.sourceFile?.path ?: run {
            thumbnailRequests.remove(pageIndex)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bitmap = renderThumbnail(path, pageIndex) ?: return@launch
                synchronized(thumbnailCache) {
                    thumbnailCache[pageIndex] = bitmap
                }
                _state.update { current ->
                    current.copy(
                        sourceFile = current.sourceFile?.copy(
                            pages = current.sourceFile.pages.map { page ->
                                if (page.pageNumber == pageIndex + 1) {
                                    page.copy(thumbnail = bitmap)
                                } else page
                            }
                        )
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to render thumbnail for page $pageIndex")
            } finally {
                thumbnailRequests.remove(pageIndex)
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

    fun togglePageSelection(pageNumber: Int) {
        _state.update { current ->
            val updatedPages = current.sourceFile?.pages?.map { page ->
                if (page.pageNumber == pageNumber) {
                    page.copy(isSelected = !page.isSelected)
                } else page
            } ?: emptyList()

            current.copy(
                sourceFile = current.sourceFile?.copy(pages = updatedPages)
            )
        }
    }

    fun selectAllPages() {
        _state.update { current ->
            val updatedPages = current.sourceFile?.pages?.map { page ->
                page.copy(isSelected = true)
            } ?: emptyList()

            current.copy(
                sourceFile = current.sourceFile?.copy(pages = updatedPages)
            )
        }
    }

    fun deselectAllPages() {
        _state.update { current ->
            val updatedPages = current.sourceFile?.pages?.map { page ->
                page.copy(isSelected = false)
            } ?: emptyList()

            current.copy(
                sourceFile = current.sourceFile?.copy(pages = updatedPages)
            )
        }
    }

    fun selectOddPages() {
        _state.update { current ->
            val updatedPages = current.sourceFile?.pages?.map { page ->
                page.copy(isSelected = page.pageNumber % 2 == 1)
            } ?: emptyList()

            current.copy(
                sourceFile = current.sourceFile?.copy(pages = updatedPages)
            )
        }
    }

    fun selectEvenPages() {
        _state.update { current ->
            val updatedPages = current.sourceFile?.pages?.map { page ->
                page.copy(isSelected = page.pageNumber % 2 == 0)
            } ?: emptyList()

            current.copy(
                sourceFile = current.sourceFile?.copy(pages = updatedPages)
            )
        }
    }

    fun selectRange(start: Int, end: Int) {
        _state.update { current ->
            val updatedPages = current.sourceFile?.pages?.map { page ->
                page.copy(isSelected = page.pageNumber in start..end)
            } ?: emptyList()

            current.copy(
                sourceFile = current.sourceFile?.copy(pages = updatedPages)
            )
        }
    }

    fun selectBeforePage(page: Int) {
        _state.update { current ->
            val updatedPages = current.sourceFile?.pages?.map { p ->
                p.copy(isSelected = p.pageNumber < page)
            } ?: emptyList()

            current.copy(
                sourceFile = current.sourceFile?.copy(pages = updatedPages)
            )
        }
    }

    fun selectAfterPage(page: Int) {
        _state.update { current ->
            val updatedPages = current.sourceFile?.pages?.map { p ->
                p.copy(isSelected = p.pageNumber > page)
            } ?: emptyList()

            current.copy(
                sourceFile = current.sourceFile?.copy(pages = updatedPages)
            )
        }
    }

    fun selectFirstN(n: Int) {
        _state.update { current ->
            val updatedPages = current.sourceFile?.pages?.map { page ->
                page.copy(isSelected = page.pageNumber <= n)
            } ?: emptyList()

            current.copy(
                sourceFile = current.sourceFile?.copy(pages = updatedPages)
            )
        }
    }

    fun selectLastN(n: Int) {
        _state.update { current ->
            val totalPages = current.sourceFile?.pageCount ?: 0
            val updatedPages = current.sourceFile?.pages?.map { page ->
                page.copy(isSelected = page.pageNumber > (totalPages - n))
            } ?: emptyList()

            current.copy(
                sourceFile = current.sourceFile?.copy(pages = updatedPages)
            )
        }
    }

    fun setOutputFileName(name: String) {
        _state.update { it.copy(outputFileName = name) }
    }

    fun setOverwriteOriginal(overwrite: Boolean) {
        _state.update { it.copy(overwriteOriginal = overwrite) }
    }

    fun removePages() {
        val currentState = _state.value
        if (!validateRemove(currentState)) return
        if (!currentState.overwriteOriginal) return // Screen launches CreateDocument

        removePagesOverwrite()
    }

    fun removePagesToUri(destinationUri: Uri) {
        val currentState = _state.value
        if (!validateRemove(currentState, requireFileName = false)) return
        val sourceFile = currentState.sourceFile ?: return
        val pagesToRemove = sourceFile.pages.filter { it.isSelected }.map { it.pageNumber }

        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, progress = 0f, error = null) }
            val tempFile = File(context.cacheDir, "removepages_out_${System.currentTimeMillis()}.pdf")
            val result = pdfToolsRepository.removePages(
                inputPath = sourceFile.path,
                outputPath = tempFile.absolutePath,
                pagesToRemove = pagesToRemove,
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
                    val localCopy = File(
                        context.cacheDir,
                        "removepages_result_${System.currentTimeMillis()}.pdf"
                    )
                    tempFile.copyTo(localCopy, overwrite = true)
                    tempFile.delete()
                    finishRemoveSuccess(
                        localPath = localCopy.absolutePath,
                        displayName = getFileNameFromUri(destinationUri)
                            ?: currentState.outputFileName.ifBlank { sourceFile.name },
                        originalPageCount = sourceFile.pageCount,
                        removedCount = pagesToRemove.size
                    )
                },
                onFailure = { error ->
                    tempFile.delete()
                    Timber.e(error, "Remove pages failed")
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            error = error.message
                                ?: context.getString(R.string.tool_error_remove_pages_failed)
                        )
                    }
                }
            )
        }
    }

    private fun removePagesOverwrite() {
        val currentState = _state.value
        val sourceFile = currentState.sourceFile ?: return
        val pagesToRemove = sourceFile.pages.filter { it.isSelected }.map { it.pageNumber }

        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, progress = 0f, error = null) }
            val tempFile = File(context.cacheDir, "removepages_overwrite_${System.currentTimeMillis()}.pdf")
            val result = pdfToolsRepository.removePages(
                inputPath = sourceFile.path,
                outputPath = tempFile.absolutePath,
                pagesToRemove = pagesToRemove,
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
                    try {
                        tempFile.copyTo(File(sourceFile.path), overwrite = true)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to refresh cache after overwrite")
                    }
                    tempFile.delete()
                    finishRemoveSuccess(
                        localPath = sourceFile.path,
                        displayName = sourceFile.name,
                        originalPageCount = sourceFile.pageCount,
                        removedCount = pagesToRemove.size
                    )
                },
                onFailure = { error ->
                    tempFile.delete()
                    Timber.e(error, "Remove pages failed")
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            error = error.message
                                ?: context.getString(R.string.tool_error_remove_pages_failed)
                        )
                    }
                }
            )
        }
    }

    private fun validateRemove(
        currentState: RemovePagesState,
        requireFileName: Boolean = true
    ): Boolean {
        val sourceFile = currentState.sourceFile
        if (sourceFile == null) {
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
        val pagesToRemove = sourceFile.pages.filter { it.isSelected }.map { it.pageNumber }
        if (pagesToRemove.isEmpty()) {
            _state.update {
                it.copy(error = context.getString(R.string.tool_error_select_pages_remove))
            }
            return false
        }
        if (pagesToRemove.size >= sourceFile.pageCount) {
            _state.update {
                it.copy(error = context.getString(R.string.tool_error_cannot_remove_all))
            }
            return false
        }
        return true
    }

    private suspend fun finishRemoveSuccess(
        localPath: String,
        displayName: String,
        originalPageCount: Int,
        removedCount: Int
    ) {
        val outputFile = File(localPath)
        val newPageCount = pdfToolsRepository.getPageCount(localPath).getOrDefault(0)
        val name = when {
            displayName.endsWith(".pdf", ignoreCase = true) -> displayName
            displayName.isNotBlank() -> "$displayName.pdf"
            else -> outputFile.name
        }
        _state.update {
            it.copy(
                isProcessing = false,
                progress = 1f,
                result = RemovePagesResult(
                    outputPath = localPath,
                    displayName = name,
                    originalPageCount = originalPageCount,
                    newPageCount = newPageCount,
                    removedPages = removedCount,
                    fileSize = outputFile.length()
                )
            )
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
        _state.update { RemovePagesState() }
    }

    companion object {
        private const val MAX_CACHED_THUMBNAILS = 48
        private const val PREFETCH_THUMBNAILS = 24
    }

    private fun copyUriToCache(uri: Uri): String? {
        return try {
            if (uri.scheme == "file") {
                return uri.path
            }

            val fileName = getFileNameFromUri(uri) ?: "temp_${System.currentTimeMillis()}.pdf"
            val cacheFile = File(context.cacheDir, "removepages_temp/$fileName")
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
