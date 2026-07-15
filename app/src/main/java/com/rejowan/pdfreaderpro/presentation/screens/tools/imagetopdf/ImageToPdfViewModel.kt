package com.rejowan.pdfreaderpro.presentation.screens.tools.imagetopdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.pdfreaderpro.R
import com.rejowan.pdfreaderpro.domain.repository.PdfToolsRepository
import com.rejowan.pdfreaderpro.util.FileOperations
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

data class ImageItem(
    val id: String,
    val uri: Uri,
    val path: String,
    val name: String,
    val size: Long,
    val thumbnail: Bitmap? = null
)

data class ImageToPdfState(
    val images: List<ImageItem> = emptyList(),
    val outputFileName: String = "",
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val progress: Float = 0f,
    val error: String? = null,
    val result: ImageToPdfResult? = null
)

data class ImageToPdfResult(
    val outputPath: String,
    val displayName: String,
    val pageCount: Int,
    val fileSize: Long
)

class ImageToPdfViewModel(
    private val pdfToolsRepository: PdfToolsRepository,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ImageToPdfState())
    val state: StateFlow<ImageToPdfState> = _state.asStateFlow()

    init {
        generateDefaultFileName()
    }

    private fun generateDefaultFileName() {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        _state.update { it.copy(outputFileName = "images_$timestamp") }
    }

    fun addImages(uris: List<Uri>) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val newImages = uris.mapNotNull { uri ->
                processImage(uri)
            }

            _state.update { current ->
                current.copy(
                    images = current.images + newImages,
                    isLoading = false
                )
            }
        }
    }

    private suspend fun processImage(uri: Uri): ImageItem? = withContext(Dispatchers.IO) {
        try {
            val path = copyImageToCache(uri) ?: return@withContext null
            val file = File(path)
            val thumbnail = generateThumbnail(path)

            ImageItem(
                id = "${System.currentTimeMillis()}_${uri.hashCode()}",
                uri = uri,
                path = path,
                name = getFileNameFromUri(uri) ?: file.name,
                size = file.length(),
                thumbnail = thumbnail
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to process image")
            null
        }
    }

    private fun generateThumbnail(path: String): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, options)

            val targetSize = 300
            val scale = maxOf(
                options.outWidth / targetSize,
                options.outHeight / targetSize
            ).coerceAtLeast(1)

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
            }
            BitmapFactory.decodeFile(path, decodeOptions)
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate thumbnail")
            null
        }
    }

    fun removeImage(id: String) {
        _state.update { current ->
            current.copy(images = current.images.filter { it.id != id })
        }
    }

    fun moveImage(fromIndex: Int, toIndex: Int) {
        _state.update { current ->
            val mutableList = current.images.toMutableList()
            val item = mutableList.removeAt(fromIndex)
            mutableList.add(toIndex, item)
            current.copy(images = mutableList)
        }
    }

    fun setOutputFileName(name: String) {
        _state.update { it.copy(outputFileName = name) }
    }

    /**
     * Validates inputs. Save As destination is chosen by the screen via CreateDocument
     * then [convertToPdfToUri].
     */
    fun convertToPdf(): Boolean {
        val currentState = _state.value

        if (currentState.images.isEmpty()) {
            _state.update { it.copy(error = context.getString(R.string.tool_error_add_at_least_one_image)) }
            return false
        }

        if (currentState.outputFileName.isBlank()) {
            _state.update { it.copy(error = context.getString(R.string.tool_error_enter_output_name)) }
            return false
        }

        return true
    }

    fun convertToPdfToUri(destinationUri: Uri) {
        val currentState = _state.value
        if (!convertToPdf()) return

        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, progress = 0f, error = null) }

            val tempFile = File(context.cacheDir, "imagetopdf_out_${System.currentTimeMillis()}.pdf")
            val imagePaths = currentState.images.map { it.path }

            val result = pdfToolsRepository.imagesToPdf(
                imagePaths = imagePaths,
                outputPath = tempFile.absolutePath,
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
                        "imagetopdf_result_${System.currentTimeMillis()}.pdf"
                    )
                    tempFile.copyTo(localCopy, overwrite = true)
                    tempFile.delete()

                    val displayName = FileOperations.getFileNameFromUri(context, destinationUri)
                        ?: currentState.outputFileName.ifBlank { "document" }
                    val name = when {
                        displayName.endsWith(".pdf", ignoreCase = true) -> displayName
                        displayName.isNotBlank() -> "$displayName.pdf"
                        else -> localCopy.name
                    }

                    _state.update {
                        it.copy(
                            isProcessing = false,
                            progress = 1f,
                            result = ImageToPdfResult(
                                outputPath = localCopy.absolutePath,
                                displayName = name,
                                pageCount = currentState.images.size,
                                fileSize = localCopy.length()
                            )
                        )
                    }
                },
                onFailure = { error ->
                    tempFile.delete()
                    Timber.e(error, "Image to PDF conversion failed")
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            error = error.message
                                ?: context.getString(R.string.tool_error_convert_images_failed)
                        )
                    }
                }
            )
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun reset() {
        _state.update { ImageToPdfState() }
        generateDefaultFileName()
    }

    private suspend fun copyImageToCache(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            if (uri.scheme == "file") {
                return@withContext uri.path
            }

            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            inputStream.use { stream ->
                val fileName = getFileNameFromUri(uri) ?: "image_${System.currentTimeMillis()}.jpg"
                val cacheFile = File(context.cacheDir, "imagetopdf_temp/$fileName")
                cacheFile.parentFile?.mkdirs()

                cacheFile.outputStream().use { output ->
                    stream.copyTo(output)
                }

                cacheFile.absolutePath
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to copy image to cache")
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
