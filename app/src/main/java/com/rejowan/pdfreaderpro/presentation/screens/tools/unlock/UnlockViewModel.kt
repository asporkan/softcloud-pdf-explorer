package com.rejowan.pdfreaderpro.presentation.screens.tools.unlock

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.pdfreaderpro.R
import com.rejowan.pdfreaderpro.domain.repository.PdfToolsRepository
import com.rejowan.pdfreaderpro.presentation.screens.tools.common.ToolOutputWriter
import com.rejowan.pdfreaderpro.util.FileOperations
import com.rejowan.pdfreaderpro.util.SafOutputHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

data class SourceFile(
    val uri: Uri,
    val path: String,
    val name: String,
    val size: Long,
    val isPasswordProtected: Boolean = false
)

data class UnlockState(
    val sourceFile: SourceFile? = null,
    val password: String = "",
    val outputFileName: String = "",
    val overwriteOriginal: Boolean = false,
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val progress: Float = 0f,
    val error: String? = null,
    val readyToSave: Boolean = false,
    val result: UnlockResult? = null
)

data class UnlockResult(
    val outputPath: String,
    val displayName: String,
    val pageCount: Int,
    val fileSize: Long,
    val locationLabel: String = ""
)

class UnlockViewModel(
    private val pdfToolsRepository: PdfToolsRepository,
    private val context: Context
) : ViewModel() {

    private var pendingTemp: File? = null
    private val _state = MutableStateFlow(UnlockState())
    val state: StateFlow<UnlockState> = _state.asStateFlow()

    fun setSourceFile(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val path = copyUriToCache(uri)
                if (path != null) {
                    val file = File(path)
                    val fileName = getFileNameFromUri(uri) ?: file.name

                    // Check if the file is password protected
                    val isProtected = pdfToolsRepository.isPasswordProtected(path).getOrDefault(false)

                    _state.update {
                        it.copy(
                            sourceFile = SourceFile(
                                uri = uri,
                                path = path,
                                name = fileName,
                                size = file.length(),
                                isPasswordProtected = isProtected
                            ),
                            isLoading = false,
                            error = if (!isProtected) {
                                context.getString(R.string.pdf_not_protected)
                            } else {
                                null
                            },
                            result = null
                        )
                    }

                    // Generate default output name
                    val baseName = file.nameWithoutExtension
                    _state.update { it.copy(outputFileName = "${baseName}_unlocked") }
                } else {
                    _state.update { it.copy(isLoading = false, error = context.getString(R.string.tool_error_load_pdf)) }
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

    fun setPassword(password: String) {
        _state.update { it.copy(password = password) }
    }

    fun setOutputFileName(name: String) {
        _state.update { it.copy(outputFileName = name) }
    }

    fun setOverwriteOriginal(overwrite: Boolean) {
        _state.update { it.copy(overwriteOriginal = overwrite) }
    }

    /**
     * Overwrite mode writes back to the source document. Save As mode first unlocks into a cache
     * file, so a wrong password fails here, before any target file exists; on success
     * [UnlockState.readyToSave] asks the screen for a destination, then [commitPendingTo] saves it.
     */
    fun unlock() {
        val currentState = _state.value
        if (!validateUnlock(currentState)) return
        if (currentState.overwriteOriginal) {
            unlockOverwrite()
        } else {
            unlockToPending()
        }
    }

    private fun unlockToPending() {
        val currentState = _state.value
        val sourceFile = currentState.sourceFile ?: return

        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, progress = 0f, error = null, readyToSave = false) }
            discardPending()

            val tempFile = File(context.cacheDir, "unlock_out_${System.currentTimeMillis()}.pdf")
            val result = pdfToolsRepository.unlockPdf(
                inputPath = sourceFile.path,
                outputPath = tempFile.absolutePath,
                password = currentState.password,
                onProgress = { progress -> _state.update { it.copy(progress = progress) } }
            )

            result.fold(
                onSuccess = {
                    pendingTemp = tempFile
                    _state.update { it.copy(isProcessing = false, readyToSave = true) }
                },
                onFailure = { error ->
                    tempFile.delete()
                    Timber.e(error, "Unlock failed")
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            error = error.message ?: context.getString(R.string.tool_error_unlock_failed)
                        )
                    }
                }
            )
        }
    }

    /** The screen has started asking for a destination; the unlocked cache file stays pending. */
    fun markSaveRequested() {
        _state.update { it.copy(readyToSave = false) }
    }

    /** Writes the already-unlocked cache file to the destination the user chose. */
    fun commitPendingTo(destinationUri: Uri) {
        val tempFile = pendingTemp
        if (tempFile == null || !tempFile.exists()) {
            SafOutputHelper.deleteDocumentQuietly(context, destinationUri)
            _state.update { it.copy(error = context.getString(R.string.tool_error_unlock_failed)) }
            return
        }
        pendingTemp = null

        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, error = null) }
            val committed = withContext(Dispatchers.IO) {
                ToolOutputWriter.commit(context, tempFile, destinationUri)
            }
            if (committed == null) {
                _state.update {
                    it.copy(
                        isProcessing = false,
                        error = context.getString(R.string.tool_error_replace_original)
                    )
                }
                return@launch
            }
            finishUnlockSuccess(
                localPath = committed.localCopy.absolutePath,
                displayName = committed.displayName,
                locationLabel = committed.locationLabel
            )
        }
    }

    private fun discardPending() {
        pendingTemp?.delete()
        pendingTemp = null
    }

    override fun onCleared() {
        discardPending()
        super.onCleared()
    }

    private fun unlockOverwrite() {
        val currentState = _state.value
        val sourceFile = currentState.sourceFile ?: return

        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, progress = 0f, error = null) }

            val tempFile = File(context.cacheDir, "unlock_overwrite_${System.currentTimeMillis()}.pdf")
            val result = pdfToolsRepository.unlockPdf(
                inputPath = sourceFile.path,
                outputPath = tempFile.absolutePath,
                password = currentState.password,
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

                    finishUnlockSuccess(
                        localPath = sourceFile.path,
                        displayName = sourceFile.name
                    )
                },
                onFailure = { error ->
                    tempFile.delete()
                    Timber.e(error, "Unlock failed")
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            error = error.message ?: context.getString(R.string.tool_error_unlock_failed)
                        )
                    }
                }
            )
        }
    }

    private fun validateUnlock(
        currentState: UnlockState,
        requireFileName: Boolean = true
    ): Boolean {
        val sourceFile = currentState.sourceFile
        if (sourceFile == null) {
            _state.update { it.copy(error = context.getString(R.string.tool_error_select_pdf_first)) }
            return false
        }
        if (!sourceFile.isPasswordProtected) {
            _state.update { it.copy(error = context.getString(R.string.pdf_not_protected)) }
            return false
        }
        if (currentState.password.isBlank()) {
            _state.update { it.copy(error = context.getString(R.string.tool_error_enter_password)) }
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

    private suspend fun finishUnlockSuccess(localPath: String, displayName: String, locationLabel: String = "") {
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
                result = UnlockResult(
                    outputPath = localPath,
                    displayName = name,
                    pageCount = pageCount,
                    fileSize = outputFile.length(),
                    locationLabel = locationLabel
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
        discardPending()
        _state.update { UnlockState() }
    }

    private fun copyUriToCache(uri: Uri): String? {
        return try {
            if (uri.scheme == "file") {
                return uri.path
            }

            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val fileName = getFileNameFromUri(uri) ?: "temp_${System.currentTimeMillis()}.pdf"
            val cacheFile = File(context.cacheDir, "unlock_temp/$fileName")
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
