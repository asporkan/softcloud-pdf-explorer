package com.rejowan.pdfreaderpro.presentation.screens.tools.rotate

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.annotation.StringRes
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

/**
 * Rotation angle options.
 */
enum class RotationAngle(val degrees: Int, @StringRes val labelRes: Int) {
    ROTATE_90(90, R.string.rotate_angle_90_right),
    ROTATE_180(180, R.string.rotate_angle_180),
    ROTATE_270(270, R.string.rotate_angle_90_left)
}

/**
 * Page selection mode for rotation.
 */
enum class PageSelectionMode {
    ALL_PAGES,
    SELECTED_PAGES
}

/**
 * Quick selection options for pages.
 */
enum class QuickSelection {
    ALL,
    ODD,
    EVEN,
    FIRST_HALF,
    SECOND_HALF,
    EVERY_2ND,
    EVERY_3RD
}

data class PageInfo(
    val pageNumber: Int,
    val thumbnail: Bitmap?,
    val currentRotation: Int = 0,
    val isSelected: Boolean = false
)

data class SourceFile(
    val uri: Uri,
    val path: String,
    val name: String,
    val size: Long,
    val pageCount: Int = 0,
    val pages: List<PageInfo> = emptyList()
)

data class RotateState(
    val sourceFile: SourceFile? = null,
    val rotationAngle: RotationAngle = RotationAngle.ROTATE_90,
    val selectionMode: PageSelectionMode = PageSelectionMode.ALL_PAGES,
    val outputFileName: String = "",
    val overwriteOriginal: Boolean = false,
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val progress: Float = 0f,
    val error: String? = null,
    val result: RotateResult? = null
)

data class RotateResult(
    val outputPath: String,
    val pageCount: Int,
    val fileSize: Long,
    val rotatedPages: Int
)

class RotateViewModel(
    private val pdfToolsRepository: PdfToolsRepository,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(RotateState())
    val state: StateFlow<RotateState> = _state.asStateFlow()

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
                    val pageCount = pdfToolsRepository.getPageCount(path).getOrDefault(0)
                    val pages = generatePageThumbnails(path, pageCount)
                    val selectedPages = pages.map { it.copy(isSelected = true) }

                    LoadedSource(
                        uri = uri,
                        path = path,
                        name = getFileNameFromUri(uri) ?: file.name,
                        size = file.length(),
                        pageCount = pageCount,
                        pages = selectedPages,
                        baseName = file.nameWithoutExtension
                    )
                }

                _state.update {
                    it.copy(
                        sourceFile = SourceFile(
                            uri = loaded.uri,
                            path = loaded.path,
                            name = loaded.name,
                            size = loaded.size,
                            pageCount = loaded.pageCount,
                            pages = loaded.pages
                        ),
                        selectionMode = PageSelectionMode.ALL_PAGES,
                        isLoading = false,
                        error = null,
                        result = null,
                        outputFileName = "${loaded.baseName}_rotated"
                    )
                }
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

    private data class LoadedSource(
        val uri: Uri,
        val path: String,
        val name: String,
        val size: Long,
        val pageCount: Int,
        val pages: List<PageInfo>,
        val baseName: String
    )

    private fun generatePageThumbnails(
        pdfPath: String,
        pageCount: Int
    ): List<PageInfo> {
        val pages = mutableListOf<PageInfo>()
        try {
            val file = File(pdfPath)
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use { renderer ->
                    for (i in 0 until minOf(pageCount, 50)) {
                        renderer.openPage(i).use { page ->
                            val thumbnailSize = 300
                            val aspectRatio = page.width.toFloat() / page.height.toFloat()
                            val width: Int
                            val height: Int
                            if (aspectRatio > 1) {
                                width = thumbnailSize
                                height = (thumbnailSize / aspectRatio).toInt()
                            } else {
                                height = thumbnailSize
                                width = (thumbnailSize * aspectRatio).toInt()
                            }

                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                            pages.add(
                                PageInfo(
                                    pageNumber = i + 1,
                                    thumbnail = bitmap,
                                    currentRotation = 0,
                                    isSelected = false
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate thumbnails")
        }
        return pages
    }

    fun setRotationAngle(angle: RotationAngle) {
        _state.update { it.copy(rotationAngle = angle) }
    }

    fun setSelectionMode(mode: PageSelectionMode) {
        _state.update { current ->
            val updatedPages = current.sourceFile?.pages?.map { page ->
                page.copy(isSelected = mode == PageSelectionMode.ALL_PAGES)
            } ?: emptyList()

            current.copy(
                selectionMode = mode,
                sourceFile = current.sourceFile?.copy(pages = updatedPages)
            )
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
                sourceFile = current.sourceFile?.copy(pages = updatedPages),
                selectionMode = PageSelectionMode.SELECTED_PAGES
            )
        }
    }

    fun selectAllPages() {
        _state.update { current ->
            val updatedPages = current.sourceFile?.pages?.map { page ->
                page.copy(isSelected = true)
            } ?: emptyList()

            current.copy(
                sourceFile = current.sourceFile?.copy(pages = updatedPages),
                selectionMode = PageSelectionMode.ALL_PAGES
            )
        }
    }

    fun deselectAllPages() {
        _state.update { current ->
            val updatedPages = current.sourceFile?.pages?.map { page ->
                page.copy(isSelected = false)
            } ?: emptyList()

            current.copy(
                sourceFile = current.sourceFile?.copy(pages = updatedPages),
                selectionMode = PageSelectionMode.SELECTED_PAGES
            )
        }
    }

    fun applyQuickSelection(selection: QuickSelection) {
        _state.update { current ->
            val totalPages = current.sourceFile?.pageCount ?: 0
            val updatedPages = current.sourceFile?.pages?.map { page ->
                val isSelected = when (selection) {
                    QuickSelection.ALL -> true
                    QuickSelection.ODD -> page.pageNumber % 2 == 1
                    QuickSelection.EVEN -> page.pageNumber % 2 == 0
                    QuickSelection.FIRST_HALF -> page.pageNumber <= (totalPages + 1) / 2
                    QuickSelection.SECOND_HALF -> page.pageNumber > totalPages / 2
                    QuickSelection.EVERY_2ND -> page.pageNumber % 2 == 0
                    QuickSelection.EVERY_3RD -> page.pageNumber % 3 == 0
                }
                page.copy(isSelected = isSelected)
            } ?: emptyList()

            current.copy(
                sourceFile = current.sourceFile?.copy(pages = updatedPages),
                selectionMode = if (selection == QuickSelection.ALL) {
                    PageSelectionMode.ALL_PAGES
                } else {
                    PageSelectionMode.SELECTED_PAGES
                }
            )
        }
    }

    fun selectPageRange(start: Int, end: Int) {
        _state.update { current ->
            val updatedPages = current.sourceFile?.pages?.map { page ->
                page.copy(isSelected = page.pageNumber in start..end)
            } ?: emptyList()

            current.copy(
                sourceFile = current.sourceFile?.copy(pages = updatedPages),
                selectionMode = PageSelectionMode.SELECTED_PAGES
            )
        }
    }

    fun setOutputFileName(name: String) {
        _state.update { it.copy(outputFileName = name) }
    }

    fun setOverwriteOriginal(overwrite: Boolean) {
        _state.update { it.copy(overwriteOriginal = overwrite) }
    }

    /**
     * Entry from Save: overwrite writes back to the selected document URI;
     * Save As is handled by the screen via CreateDocument then [rotateToUri].
     */
    fun rotate() {
        val currentState = _state.value
        if (!validateRotate(currentState)) return

        if (!currentState.overwriteOriginal) {
            return
        }

        rotateOverwrite()
    }

    fun rotateToUri(destinationUri: Uri) {
        val currentState = _state.value
        if (!validateRotate(currentState, requireFileName = false)) return

        val sourceFile = currentState.sourceFile ?: return
        val pagesToRotate = resolvePagesToRotate(currentState)

        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, progress = 0f, error = null) }

            val tempFile = File(context.cacheDir, "rotate_out_${System.currentTimeMillis()}.pdf")
            val result = pdfToolsRepository.rotatePages(
                inputPath = sourceFile.path,
                outputPath = tempFile.absolutePath,
                rotation = currentState.rotationAngle.degrees,
                pages = pagesToRotate,
                onProgress = { progress ->
                    _state.update { it.copy(progress = progress) }
                }
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
                        "rotate_result_${System.currentTimeMillis()}.pdf"
                    )
                    tempFile.copyTo(localCopy, overwrite = true)
                    tempFile.delete()

                    finishRotateSuccess(
                        localPath = localCopy.absolutePath,
                        rotatedPages = pagesToRotate?.size ?: sourceFile.pageCount
                    )
                },
                onFailure = { error ->
                    tempFile.delete()
                    Timber.e(error, "Rotation failed")
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            error = error.message
                                ?: context.getString(R.string.tool_error_rotation_failed)
                        )
                    }
                }
            )
        }
    }

    private fun rotateOverwrite() {
        val currentState = _state.value
        val sourceFile = currentState.sourceFile ?: return
        val pagesToRotate = resolvePagesToRotate(currentState)

        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, progress = 0f, error = null) }

            val tempFile = File(context.cacheDir, "rotate_overwrite_${System.currentTimeMillis()}.pdf")
            val result = pdfToolsRepository.rotatePages(
                inputPath = sourceFile.path,
                outputPath = tempFile.absolutePath,
                rotation = currentState.rotationAngle.degrees,
                pages = pagesToRotate,
                onProgress = { progress ->
                    _state.update { it.copy(progress = progress) }
                }
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

                    finishRotateSuccess(
                        localPath = sourceFile.path,
                        rotatedPages = pagesToRotate?.size ?: sourceFile.pageCount
                    )
                },
                onFailure = { error ->
                    tempFile.delete()
                    Timber.e(error, "Rotation failed")
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            error = error.message
                                ?: context.getString(R.string.tool_error_rotation_failed)
                        )
                    }
                }
            )
        }
    }

    private fun validateRotate(
        currentState: RotateState,
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
        if (currentState.selectionMode == PageSelectionMode.SELECTED_PAGES) {
            val selected = currentState.sourceFile.pages.filter { it.isSelected }.map { it.pageNumber }
            if (selected.isEmpty()) {
                _state.update {
                    it.copy(error = context.getString(R.string.tool_error_select_pages_rotate))
                }
                return false
            }
        }
        return true
    }

    /** Returns null for all-pages mode; null with error when no pages selected. */
    private fun resolvePagesToRotate(currentState: RotateState): List<Int>? {
        val sourceFile = currentState.sourceFile ?: return null
        return when (currentState.selectionMode) {
            PageSelectionMode.ALL_PAGES -> null
            PageSelectionMode.SELECTED_PAGES -> {
                sourceFile.pages.filter { it.isSelected }.map { it.pageNumber }
            }
        }
    }

    private suspend fun finishRotateSuccess(localPath: String, rotatedPages: Int) {
        val outputFile = File(localPath)
        val pageCount = pdfToolsRepository.getPageCount(localPath).getOrDefault(0)
        _state.update {
            it.copy(
                isProcessing = false,
                progress = 1f,
                result = RotateResult(
                    outputPath = localPath,
                    pageCount = pageCount,
                    fileSize = outputFile.length(),
                    rotatedPages = rotatedPages
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
        _state.update { RotateState() }
    }

    private fun copyUriToCache(uri: Uri): String? {
        return try {
            if (uri.scheme == "file") {
                return uri.path
            }

            val fileName = getFileNameFromUri(uri) ?: "temp_${System.currentTimeMillis()}.pdf"
            val cacheFile = File(context.cacheDir, "rotate_temp/$fileName")
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
