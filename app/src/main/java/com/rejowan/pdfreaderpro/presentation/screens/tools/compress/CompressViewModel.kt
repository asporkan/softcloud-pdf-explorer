package com.rejowan.pdfreaderpro.presentation.screens.tools.compress

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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Compression quality levels with their corresponding quality values.
 */
enum class CompressionLevel(
    @StringRes val labelRes: Int,
    @StringRes val descriptionRes: Int,
    val quality: Float
) {
    LOW(R.string.compression_low, R.string.tools_compress_low_desc, 0.8f),
    MEDIUM(R.string.compression_medium, R.string.tools_compress_medium_desc, 0.5f),
    HIGH(R.string.compression_high, R.string.tools_compress_high_desc, 0.2f)
}

data class SourceFile(
    val uri: Uri,
    val path: String,
    val name: String,
    val size: Long,
    val pageCount: Int = 0,
    val thumbnail: Bitmap? = null
)

data class CompressState(
    val sourceFile: SourceFile? = null,
    val compressionLevel: CompressionLevel = CompressionLevel.MEDIUM,
    val outputFileName: String = "",
    val overwriteOriginal: Boolean = false,
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val progress: Float = 0f,
    val error: String? = null,
    val result: CompressResult? = null
)

data class CompressResult(
    val outputPath: String,
    val originalSize: Long,
    val compressedSize: Long,
    val pageCount: Int
) {
    val reductionPercentage: Float
        get() = if (originalSize > 0) {
            ((originalSize - compressedSize).toFloat() / originalSize) * 100f
        } else 0f

    val savedBytes: Long
        get() = originalSize - compressedSize
}

class CompressViewModel(
    private val pdfToolsRepository: PdfToolsRepository,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(CompressState())
    val state: StateFlow<CompressState> = _state.asStateFlow()

    private fun generateDefaultFileName() {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        _state.update { it.copy(outputFileName = "compressed_$timestamp") }
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
                    val fileSize = file.length()
                    val pageCount = pdfToolsRepository.getPageCount(path).getOrDefault(0)
                    val thumbnail = generateThumbnail(path)

                    LoadedSource(
                        uri = uri,
                        path = path,
                        name = getFileNameFromUri(uri) ?: file.name,
                        fileSize = fileSize,
                        pageCount = pageCount,
                        thumbnail = thumbnail,
                        baseName = file.nameWithoutExtension
                    )
                }

                _state.update {
                    it.copy(
                        sourceFile = SourceFile(
                            uri = loaded.uri,
                            path = loaded.path,
                            name = loaded.name,
                            size = loaded.fileSize,
                            pageCount = loaded.pageCount,
                            thumbnail = loaded.thumbnail
                        ),
                        isLoading = false,
                        error = null,
                        result = null,
                        outputFileName = "${loaded.baseName}_compressed"
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
        val fileSize: Long,
        val pageCount: Int,
        val thumbnail: Bitmap?,
        val baseName: String
    )

    private suspend fun generateThumbnail(pdfPath: String): Bitmap? {
        try {
            val file = File(pdfPath)
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use { renderer ->
                    if (renderer.pageCount > 0) {
                        renderer.openPage(0).use { page ->
                            val thumbnailSize = 120
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
                            return bitmap
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate thumbnail")
        }
        return null
    }

    fun setCompressionLevel(level: CompressionLevel) {
        _state.update { it.copy(compressionLevel = level) }
    }

    fun setOutputFileName(name: String) {
        _state.update { it.copy(outputFileName = name) }
    }

    fun setOverwriteOriginal(overwrite: Boolean) {
        _state.update { it.copy(overwriteOriginal = overwrite) }
    }

    /**
     * Entry from Save: overwrite writes back to the selected document URI;
     * Save As is handled by the screen via CreateDocument then [compressToUri].
     */
    fun compress() {
        val currentState = _state.value
        if (!validateCompress(currentState)) return

        if (!currentState.overwriteOriginal) {
            return
        }

        compressOverwrite()
    }

    fun compressToUri(destinationUri: Uri) {
        val currentState = _state.value
        if (!validateCompress(currentState, requireFileName = false)) return

        val sourceFile = currentState.sourceFile ?: return

        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, progress = 0f, error = null) }

            val tempFile = File(context.cacheDir, "compress_out_${System.currentTimeMillis()}.pdf")
            val result = pdfToolsRepository.compressPdf(
                inputPath = sourceFile.path,
                outputPath = tempFile.absolutePath,
                quality = currentState.compressionLevel.quality,
                onProgress = { progress ->
                    _state.update { it.copy(progress = progress) }
                }
            )

            result.fold(
                onSuccess = { newSize ->
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
                        "compress_result_${System.currentTimeMillis()}.pdf"
                    )
                    tempFile.copyTo(localCopy, overwrite = true)
                    tempFile.delete()

                    finishCompressSuccess(
                        localPath = localCopy.absolutePath,
                        originalSize = sourceFile.size,
                        compressedSize = newSize
                    )
                },
                onFailure = { error ->
                    tempFile.delete()
                    Timber.e(error, "Compression failed")
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            error = error.message
                                ?: context.getString(R.string.tool_error_compress_failed)
                        )
                    }
                }
            )
        }
    }

    private fun compressOverwrite() {
        val currentState = _state.value
        val sourceFile = currentState.sourceFile ?: return

        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, progress = 0f, error = null) }

            val tempFile = File(context.cacheDir, "compress_overwrite_${System.currentTimeMillis()}.pdf")
            val result = pdfToolsRepository.compressPdf(
                inputPath = sourceFile.path,
                outputPath = tempFile.absolutePath,
                quality = currentState.compressionLevel.quality,
                onProgress = { progress ->
                    _state.update { it.copy(progress = progress) }
                }
            )

            result.fold(
                onSuccess = { newSize ->
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

                    finishCompressSuccess(
                        localPath = sourceFile.path,
                        originalSize = sourceFile.size,
                        compressedSize = newSize
                    )
                },
                onFailure = { error ->
                    tempFile.delete()
                    Timber.e(error, "Compression failed")
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            error = error.message
                                ?: context.getString(R.string.tool_error_compress_failed)
                        )
                    }
                }
            )
        }
    }

    private fun validateCompress(
        currentState: CompressState,
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
        return true
    }

    private suspend fun finishCompressSuccess(
        localPath: String,
        originalSize: Long,
        compressedSize: Long
    ) {
        val pageCount = pdfToolsRepository.getPageCount(localPath).getOrDefault(0)
        _state.update {
            it.copy(
                isProcessing = false,
                progress = 1f,
                result = CompressResult(
                    outputPath = localPath,
                    originalSize = originalSize,
                    compressedSize = compressedSize,
                    pageCount = pageCount
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
        _state.update { CompressState() }
        generateDefaultFileName()
    }

    private fun copyUriToCache(uri: Uri): String? {
        return try {
            if (uri.scheme == "file") {
                return uri.path
            }

            val fileName = getFileNameFromUri(uri) ?: "temp_${System.currentTimeMillis()}.pdf"
            val cacheFile = File(context.cacheDir, "compress_temp/$fileName")
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
