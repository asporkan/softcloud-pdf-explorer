package com.rejowan.pdfreaderpro.presentation.screens.tools.merge

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
 * Represents page selection mode for a PDF file.
 */
sealed class PageSelection {
    data object All : PageSelection()
    data class Range(val start: Int, val end: Int) : PageSelection()
    data class Custom(val pages: List<Int>) : PageSelection()

    fun toDisplayString(context: Context, totalPages: Int): String = when (this) {
        is All -> context.getString(R.string.page_selection_all, totalPages)
        is Range -> context.getString(R.string.page_selection_range, start, end)
        is Custom -> if (pages.size <= 5) {
            context.getString(R.string.page_selection_custom, pages.joinToString(", "))
        } else {
            context.getString(
                R.string.page_selection_custom_more,
                pages.take(4).joinToString(", "),
                pages.size
            )
        }
    }

    fun toPageList(totalPages: Int): List<Int>? = when (this) {
        is All -> null // null means all pages
        is Range -> (start..end.coerceAtMost(totalPages)).toList()
        is Custom -> pages.filter { it in 1..totalPages }
    }

    fun getSelectedCount(totalPages: Int): Int = when (this) {
        is All -> totalPages
        is Range -> (end.coerceAtMost(totalPages) - start + 1).coerceAtLeast(0)
        is Custom -> pages.count { it in 1..totalPages }
    }
}

data class MergeFile(
    val uri: Uri,
    val path: String,
    val name: String,
    val size: Long,
    val pageCount: Int = 0,
    val thumbnail: Bitmap? = null,
    val pageSelection: PageSelection = PageSelection.All,
    val isPasswordProtected: Boolean = false
)

data class MergeState(
    val selectedFiles: List<MergeFile> = emptyList(),
    val outputFileName: String = "",
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val progress: Float = 0f,
    val error: String? = null,
    val result: MergeResult? = null
)

data class MergeResult(
    val outputPath: String,
    val displayName: String,
    val pageCount: Int,
    val fileSize: Long
)

class MergeViewModel(
    private val pdfToolsRepository: PdfToolsRepository,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(MergeState())
    val state: StateFlow<MergeState> = _state.asStateFlow()

    init {
        generateDefaultFileName()
    }

    private fun generateDefaultFileName() {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        _state.update { it.copy(outputFileName = "merged_$timestamp") }
    }

    fun addFiles(uris: List<Uri>) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val skippedPasswordProtected = mutableListOf<String>()

            val newFiles = uris.mapNotNull { uri ->
                try {
                    val path = getPathFromUri(uri)
                    if (path != null && File(path).exists()) {
                        val file = File(path)
                        // Check if password protected
                        val isProtected = pdfToolsRepository.isPasswordProtected(path).getOrDefault(false)
                        if (isProtected) {
                            skippedPasswordProtected.add(file.name)
                            return@mapNotNull null
                        }
                        val pageCount = pdfToolsRepository.getPageCount(path).getOrDefault(0)
                        val thumbnail = generateThumbnail(path)
                        MergeFile(
                            uri = uri,
                            path = path,
                            name = file.name,
                            size = file.length(),
                            pageCount = pageCount,
                            thumbnail = thumbnail
                        )
                    } else {
                        // Try to copy from content URI
                        val tempPath = copyUriToCache(uri)
                        if (tempPath != null) {
                            val file = File(tempPath)
                            val fileName = getFileNameFromUri(uri) ?: file.name
                            // Check if password protected
                            val isProtected = pdfToolsRepository.isPasswordProtected(tempPath).getOrDefault(false)
                            if (isProtected) {
                                skippedPasswordProtected.add(fileName)
                                // Clean up temp file for password-protected PDF
                                file.delete()
                                return@mapNotNull null
                            }
                            val pageCount = pdfToolsRepository.getPageCount(tempPath).getOrDefault(0)
                            val thumbnail = generateThumbnail(tempPath)
                            MergeFile(
                                uri = uri,
                                path = tempPath,
                                name = fileName,
                                size = file.length(),
                                pageCount = pageCount,
                                thumbnail = thumbnail
                            )
                        } else null
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to add file: $uri")
                    null
                }
            }

            _state.update { current ->
                val existingPaths = current.selectedFiles.map { it.path }.toSet()
                val filteredNew = newFiles.filter { it.path !in existingPaths }
                val updatedFiles = current.selectedFiles + filteredNew
                val errorMessage = skippedPasswordError(skippedPasswordProtected, updatedFiles)
                current.copy(
                    selectedFiles = updatedFiles,
                    isLoading = false,
                    error = errorMessage
                )
            }
        }
    }

    fun addFilesFromPaths(paths: List<String>) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val skippedPasswordProtected = mutableListOf<String>()

            val newFiles = paths.mapNotNull { path ->
                try {
                    val file = File(path)
                    if (!file.exists()) return@mapNotNull null

                    // Check if password protected
                    val isProtected = pdfToolsRepository.isPasswordProtected(path).getOrDefault(false)
                    if (isProtected) {
                        skippedPasswordProtected.add(file.name)
                        return@mapNotNull null
                    }

                    val pageCount = pdfToolsRepository.getPageCount(path).getOrDefault(0)
                    val thumbnail = generateThumbnail(path)
                    MergeFile(
                        uri = Uri.fromFile(file),
                        path = path,
                        name = file.name,
                        size = file.length(),
                        pageCount = pageCount,
                        thumbnail = thumbnail
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Failed to add file from path: $path")
                    null
                }
            }

            _state.update { current ->
                val existingPaths = current.selectedFiles.map { it.path }.toSet()
                val filteredNew = newFiles.filter { it.path !in existingPaths }
                val updatedFiles = current.selectedFiles + filteredNew
                val errorMessage = skippedPasswordError(skippedPasswordProtected, updatedFiles)
                current.copy(
                    selectedFiles = updatedFiles,
                    isLoading = false,
                    error = errorMessage
                )
            }
        }
    }

    private suspend fun generateThumbnail(pdfPath: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val file = File(pdfPath)
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)

            if (renderer.pageCount > 0) {
                val page = renderer.openPage(0)

                // Create a compact thumbnail (48dp equivalent at ~2x density = 96px)
                val thumbnailSize = 96
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
                page.close()
                renderer.close()
                fd.close()
                bitmap
            } else {
                renderer.close()
                fd.close()
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate thumbnail for: $pdfPath")
            null
        }
    }

    fun removeFile(file: MergeFile) {
        _state.update { current ->
            current.copy(selectedFiles = current.selectedFiles.filter { it.path != file.path })
        }
    }

    fun moveFile(fromIndex: Int, toIndex: Int) {
        _state.update { current ->
            val files = current.selectedFiles.toMutableList()
            if (fromIndex in files.indices && toIndex in files.indices) {
                val item = files.removeAt(fromIndex)
                files.add(toIndex, item)
            }
            current.copy(selectedFiles = files)
        }
    }

    fun updatePageSelection(file: MergeFile, selection: PageSelection) {
        _state.update { current ->
            current.copy(
                selectedFiles = current.selectedFiles.map {
                    if (it.path == file.path) it.copy(pageSelection = selection) else it
                }
            )
        }
    }

    fun setOutputFileName(name: String) {
        _state.update { it.copy(outputFileName = name) }
    }

    /**
     * Validates merge inputs. Save As destination is chosen by the screen via CreateDocument
     * then [mergeToUri].
     */
    fun merge(): Boolean {
        return validateMerge()
    }

    fun mergeToUri(destinationUri: Uri) {
        val currentState = _state.value
        if (!validateMerge()) return

        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, progress = 0f, error = null) }

            val tempFile = File(context.cacheDir, "merge_out_${System.currentTimeMillis()}.pdf")

            val selections = currentState.selectedFiles.map { file ->
                PdfToolsRepository.PdfPageSelection(
                    path = file.path,
                    pages = file.pageSelection.toPageList(file.pageCount)
                )
            }

            val result = pdfToolsRepository.mergePdfsWithSelection(
                selections = selections,
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

                    cleanupTempFiles()

                    val localCopy = File(
                        context.cacheDir,
                        "merge_result_${System.currentTimeMillis()}.pdf"
                    )
                    tempFile.copyTo(localCopy, overwrite = true)
                    tempFile.delete()

                    finishMergeSuccess(
                        localPath = localCopy.absolutePath,
                        displayName = FileOperations.getFileNameFromUri(context, destinationUri)
                            ?: currentState.outputFileName.ifBlank { "merged" }
                    )
                },
                onFailure = { error ->
                    tempFile.delete()
                    Timber.e(error, "Merge failed")
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            error = error.message ?: context.getString(R.string.tool_error_merge_failed)
                        )
                    }
                }
            )
        }
    }

    private fun validateMerge(): Boolean {
        val currentState = _state.value
        if (currentState.selectedFiles.size < 2) {
            _state.update { it.copy(error = context.getString(R.string.tool_error_select_min_merge)) }
            return false
        }

        if (currentState.outputFileName.isBlank()) {
            _state.update { it.copy(error = context.getString(R.string.tool_error_enter_output_name_short)) }
            return false
        }

        val emptySelectionFiles = currentState.selectedFiles.filter { file ->
            file.pageSelection.getSelectedCount(file.pageCount) == 0
        }
        if (emptySelectionFiles.isNotEmpty()) {
            val fileNames = emptySelectionFiles.joinToString(", ") { it.name }
            _state.update {
                it.copy(error = context.getString(R.string.tool_error_no_pages_selected_for, fileNames))
            }
            return false
        }

        return true
    }

    private suspend fun finishMergeSuccess(localPath: String, displayName: String) {
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
                result = MergeResult(
                    outputPath = localPath,
                    displayName = name,
                    pageCount = pageCount,
                    fileSize = outputFile.length()
                )
            )
        }
    }

    private fun skippedPasswordError(
        skippedNames: List<String>,
        filesAfterAdd: List<MergeFile>
    ): String? {
        if (skippedNames.isEmpty()) return null
        return if (filesAfterAdd.isEmpty()) {
            context.getString(R.string.tool_error_pdf_password_protected)
        } else {
            context.getString(
                R.string.tool_error_skipped_password,
                skippedNames.joinToString(", ")
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
        cleanupTempFiles()
        _state.update {
            MergeState()
        }
        generateDefaultFileName()
    }

    private fun cleanupTempFiles() {
        try {
            val tempDir = File(context.cacheDir, "merge_temp")
            if (tempDir.exists()) {
                tempDir.deleteRecursively()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to cleanup temp files")
        }
    }

    private fun getPathFromUri(uri: Uri): String? {
        if (uri.scheme == "file") {
            return uri.path
        }

        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { _ ->
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun copyUriToCache(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            inputStream.use { stream ->
                val fileName = getFileNameFromUri(uri) ?: "temp_${System.currentTimeMillis()}.pdf"
                val cacheFile = File(context.cacheDir, "merge_temp/$fileName")
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
