package com.rejowan.pdfreaderpro.presentation.screens.tools.pdftoimage

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

enum class ImageFormat(
    val extension: String,
    @StringRes val labelRes: Int
) {
    PNG("png", R.string.format_png),
    JPG("jpg", R.string.format_jpg)
}

enum class PageSelection {
    ALL,
    CUSTOM
}

data class SourceFile(
    val uri: Uri,
    val path: String,
    val name: String,
    val size: Long,
    val pageCount: Int = 0,
    val previewBitmap: Bitmap? = null
)

data class PdfToImageState(
    val sourceFile: SourceFile? = null,
    val imageFormat: ImageFormat = ImageFormat.PNG,
    val pageSelection: PageSelection = PageSelection.ALL,
    val customPages: String = "",
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val progress: Float = 0f,
    val error: String? = null,
    val result: PdfToImageResult? = null
)

data class PdfToImageResult(
    val outputDir: String,
    val imagePaths: List<String>,
    val imageCount: Int,
    val format: String
)

class PdfToImageViewModel(
    private val pdfToolsRepository: PdfToolsRepository,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(PdfToImageState())
    val state: StateFlow<PdfToImageState> = _state.asStateFlow()

    fun setSourceFile(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val path = copyUriToCache(uri)
                if (path == null) {
                    _state.update { it.copy(isLoading = false, error = context.getString(R.string.tool_error_load_pdf)) }
                    return@launch
                }

                pdfToolsRepository.passwordProtectedBlockMessage(context, path)?.let { msg ->
                    File(path).delete()
                    _state.update {
                        it.copy(isLoading = false, sourceFile = null, error = msg)
                    }
                    return@launch
                }

                val file = File(path)
                val pageCount = pdfToolsRepository.getPageCount(path).getOrDefault(0)
                val preview = generatePreview(path)

                _state.update {
                    it.copy(
                        sourceFile = SourceFile(
                            uri = uri,
                            path = path,
                            name = getFileNameFromUri(uri) ?: file.name,
                            size = file.length(),
                            pageCount = pageCount,
                            previewBitmap = preview
                        ),
                        isLoading = false,
                        error = null,
                        result = null
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to set source file")
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = context.getString(R.string.tool_error_load_pdf_detail, e.message ?: "")
                    )
                }
            }
        }
    }

    private suspend fun generatePreview(pdfPath: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val file = File(pdfPath)
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)

            val page = renderer.openPage(0)

            val scale = 2
            val bitmap = Bitmap.createBitmap(
                page.width * scale,
                page.height * scale,
                Bitmap.Config.ARGB_8888
            )
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            renderer.close()
            fd.close()

            bitmap
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate preview")
            null
        }
    }

    fun setImageFormat(format: ImageFormat) {
        _state.update { it.copy(imageFormat = format) }
    }

    fun setPageSelection(selection: PageSelection) {
        _state.update { it.copy(pageSelection = selection) }
    }

    fun setCustomPages(pages: String) {
        _state.update { it.copy(customPages = pages) }
    }

    /**
     * Validates export inputs. Destination folder is chosen by the screen via OpenDocumentTree
     * then [exportImagesToTree].
     */
    fun exportImages(): Boolean {
        val currentState = _state.value
        if (currentState.sourceFile == null) {
            _state.update { it.copy(error = context.getString(R.string.tool_error_select_pdf_first)) }
            return false
        }
        return true
    }

    fun exportImagesToTree(treeUri: Uri) {
        val currentState = _state.value
        val sourceFile = currentState.sourceFile ?: return
        if (!exportImages()) return

        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, progress = 0f, error = null) }

            val tempDir = File(context.cacheDir, "pdftoimage_out_${System.currentTimeMillis()}").apply { mkdirs() }

            val pages = calculatePages(
                currentState.pageSelection,
                currentState.customPages,
                sourceFile.pageCount
            )

            val mimeType = if (currentState.imageFormat.extension.equals("jpg", ignoreCase = true)) {
                "image/jpeg"
            } else {
                "image/png"
            }

            val result = pdfToolsRepository.pdfToImages(
                inputPath = sourceFile.path,
                outputDir = tempDir.absolutePath,
                format = currentState.imageFormat.extension,
                pages = pages,
                onProgress = { progress ->
                    _state.update { it.copy(progress = progress) }
                }
            )

            result.fold(
                onSuccess = { tempFiles ->
                    val files = tempFiles.map { File(it) }
                    val written = withContext(Dispatchers.IO) {
                        FileOperations.writeFilesToTree(context, treeUri, files, mimeType)
                    }
                    if (!written) {
                        tempDir.deleteRecursively()
                        _state.update {
                            it.copy(
                                isProcessing = false,
                                error = context.getString(R.string.tool_error_save_to_folder)
                            )
                        }
                        return@launch
                    }

                    val localFiles = copyToResultCache(tempFiles, "pdftoimage")
                    tempDir.deleteRecursively()

                    _state.update {
                        it.copy(
                            isProcessing = false,
                            progress = 1f,
                            result = PdfToImageResult(
                                outputDir = File(localFiles.first()).parent ?: "",
                                imagePaths = localFiles,
                                imageCount = localFiles.size,
                                format = currentState.imageFormat.extension.uppercase()
                            )
                        )
                    }
                },
                onFailure = { error ->
                    tempDir.deleteRecursively()
                    Timber.e(error, "PDF to images export failed")
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            error = error.message
                                ?: context.getString(R.string.tool_error_export_images_failed)
                        )
                    }
                }
            )
        }
    }

    private fun copyToResultCache(tempFiles: List<String>, prefix: String): List<String> {
        val resultDir = File(context.cacheDir, "${prefix}_result_${System.currentTimeMillis()}").apply { mkdirs() }
        return tempFiles.map { path ->
            val src = File(path)
            val dest = File(resultDir, src.name)
            src.copyTo(dest, overwrite = true)
            dest.absolutePath
        }
    }

    private fun calculatePages(
        selection: PageSelection,
        customPages: String,
        totalPages: Int
    ): List<Int>? {
        return when (selection) {
            PageSelection.ALL -> null
            PageSelection.CUSTOM -> parseCustomPages(customPages, totalPages)
        }
    }

    private fun parseCustomPages(input: String, totalPages: Int): List<Int> {
        val pages = mutableSetOf<Int>()
        val parts = input.split(",")

        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.contains("-")) {
                val range = trimmed.split("-")
                if (range.size == 2) {
                    val start = range[0].trim().toIntOrNull() ?: continue
                    val end = range[1].trim().toIntOrNull() ?: continue
                    for (i in start..end) {
                        if (i in 1..totalPages) pages.add(i)
                    }
                }
            } else {
                val page = trimmed.toIntOrNull()
                if (page != null && page in 1..totalPages) {
                    pages.add(page)
                }
            }
        }

        return pages.sorted()
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun reset() {
        _state.update { PdfToImageState() }
    }

    private fun copyUriToCache(uri: Uri): String? {
        return try {
            if (uri.scheme == "file") {
                return uri.path
            }

            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            inputStream.use { stream ->
                val fileName = getFileNameFromUri(uri) ?: "temp_${System.currentTimeMillis()}.pdf"
                val cacheFile = File(context.cacheDir, "pdftoimage_temp/$fileName")
                cacheFile.parentFile?.mkdirs()

                cacheFile.outputStream().use { output ->
                    stream.copyTo(output)
                }

                cacheFile.absolutePath
            }
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
