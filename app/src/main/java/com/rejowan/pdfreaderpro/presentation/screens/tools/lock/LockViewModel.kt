package com.rejowan.pdfreaderpro.presentation.screens.tools.lock

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

data class SourceFile(
    val uri: Uri,
    val path: String,
    val name: String,
    val size: Long,
    val pageCount: Int = 0,
    val thumbnail: Bitmap? = null
)

data class LockState(
    val sourceFile: SourceFile? = null,
    val userPassword: String = "",
    val ownerPassword: String = "",
    val allowPrinting: Boolean = false,
    val allowCopying: Boolean = false,
    val allowModifying: Boolean = false,
    val allowAnnotations: Boolean = false,
    val outputFileName: String = "",
    val overwriteOriginal: Boolean = false,
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val progress: Float = 0f,
    val error: String? = null,
    val result: LockResult? = null
)

data class LockResult(
    val outputPath: String,
    val displayName: String,
    val pageCount: Int,
    val fileSize: Long
)

class LockViewModel(
    private val pdfToolsRepository: PdfToolsRepository,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(LockState())
    val state: StateFlow<LockState> = _state.asStateFlow()

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
                val thumbnail = generateThumbnail(path)

                _state.update {
                    it.copy(
                        sourceFile = SourceFile(
                            uri = uri,
                            path = path,
                            name = getFileNameFromUri(uri) ?: file.name,
                            size = file.length(),
                            pageCount = pageCount,
                            thumbnail = thumbnail
                        ),
                        isLoading = false,
                        error = null,
                        result = null
                    )
                }

                // Generate default output name
                val baseName = file.nameWithoutExtension
                _state.update { it.copy(outputFileName = "${baseName}_locked") }
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

    private suspend fun generateThumbnail(pdfPath: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val file = File(pdfPath)
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            val page = renderer.openPage(0)

            val thumbnailSize = 200
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
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate thumbnail")
            null
        }
    }

    fun setUserPassword(password: String) {
        _state.update { it.copy(userPassword = password) }
    }

    fun setOwnerPassword(password: String) {
        _state.update { it.copy(ownerPassword = password) }
    }

    fun setAllowPrinting(allow: Boolean) {
        _state.update { it.copy(allowPrinting = allow) }
    }

    fun setAllowCopying(allow: Boolean) {
        _state.update { it.copy(allowCopying = allow) }
    }

    fun setAllowModifying(allow: Boolean) {
        _state.update { it.copy(allowModifying = allow) }
    }

    fun setAllowAnnotations(allow: Boolean) {
        _state.update { it.copy(allowAnnotations = allow) }
    }

    fun setOutputFileName(name: String) {
        _state.update { it.copy(outputFileName = name) }
    }

    fun setOverwriteOriginal(overwrite: Boolean) {
        _state.update { it.copy(overwriteOriginal = overwrite) }
    }

    fun lock() {
        val currentState = _state.value
        if (!validateLock(currentState)) return
        if (!currentState.overwriteOriginal) return

        lockOverwrite()
    }

    fun lockToUri(destinationUri: Uri) {
        val currentState = _state.value
        if (!validateLock(currentState, requireFileName = false)) return
        val sourceFile = currentState.sourceFile ?: return

        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, progress = 0f, error = null) }

            val tempFile = File(context.cacheDir, "lock_out_${System.currentTimeMillis()}.pdf")
            val result = runLock(sourceFile.path, tempFile.absolutePath, currentState)

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
                        "lock_result_${System.currentTimeMillis()}.pdf"
                    )
                    tempFile.copyTo(localCopy, overwrite = true)
                    tempFile.delete()

                    finishLockSuccess(
                        localPath = localCopy.absolutePath,
                        displayName = getFileNameFromUri(destinationUri)
                            ?: currentState.outputFileName.ifBlank { sourceFile.name }
                    )
                },
                onFailure = { error ->
                    tempFile.delete()
                    Timber.e(error, "Lock failed")
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            error = error.message ?: context.getString(R.string.tool_error_lock_failed)
                        )
                    }
                }
            )
        }
    }

    private fun lockOverwrite() {
        val currentState = _state.value
        val sourceFile = currentState.sourceFile ?: return

        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, progress = 0f, error = null) }

            val tempFile = File(context.cacheDir, "lock_overwrite_${System.currentTimeMillis()}.pdf")
            val result = runLock(sourceFile.path, tempFile.absolutePath, currentState)

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

                    finishLockSuccess(
                        localPath = sourceFile.path,
                        displayName = sourceFile.name
                    )
                },
                onFailure = { error ->
                    tempFile.delete()
                    Timber.e(error, "Lock failed")
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            error = error.message ?: context.getString(R.string.tool_error_lock_failed)
                        )
                    }
                }
            )
        }
    }

    private suspend fun runLock(
        inputPath: String,
        outputPath: String,
        currentState: LockState
    ): Result<Unit> {
        return pdfToolsRepository.lockPdf(
            inputPath = inputPath,
            outputPath = outputPath,
            userPassword = currentState.userPassword,
            ownerPassword = currentState.ownerPassword,
            permissions = PdfToolsRepository.PdfPermissions(
                allowPrinting = currentState.allowPrinting,
                allowCopying = currentState.allowCopying,
                allowModifying = currentState.allowModifying,
                allowAnnotations = currentState.allowAnnotations
            ),
            onProgress = { progress -> _state.update { it.copy(progress = progress) } }
        )
    }

    private fun validateLock(
        currentState: LockState,
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
        if (currentState.ownerPassword.isBlank()) {
            _state.update { it.copy(error = context.getString(R.string.tool_error_owner_password_required)) }
            return false
        }
        if (currentState.ownerPassword.length < 4) {
            _state.update { it.copy(error = context.getString(R.string.tool_error_owner_password_min)) }
            return false
        }
        if (currentState.userPassword.isNotEmpty() && currentState.userPassword.length < 4) {
            _state.update { it.copy(error = context.getString(R.string.tool_error_user_password_min)) }
            return false
        }
        return true
    }

    private suspend fun finishLockSuccess(localPath: String, displayName: String) {
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
                result = LockResult(
                    outputPath = localPath,
                    displayName = name,
                    pageCount = pageCount,
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
        _state.update { LockState() }
    }

    private fun copyUriToCache(uri: Uri): String? {
        return try {
            if (uri.scheme == "file") {
                return uri.path
            }

            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val fileName = getFileNameFromUri(uri) ?: "temp_${System.currentTimeMillis()}.pdf"
            val cacheFile = File(context.cacheDir, "lock_temp/$fileName")
            cacheFile.parentFile?.mkdirs()

            cacheFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }

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
