package com.rejowan.pdfreaderpro.domain.repository

import com.rejowan.pdfreaderpro.domain.model.PdfFile
import com.rejowan.pdfreaderpro.domain.model.PdfFolder
import kotlinx.coroutines.flow.Flow

interface PdfFileRepository {
    fun getAllPdfFiles(): Flow<List<PdfFile>>
    fun getPdfsByFolder(folderPath: String): Flow<List<PdfFile>>
    fun searchPdfs(query: String): Flow<List<PdfFile>>
    fun getPdfFolders(): Flow<List<PdfFolder>>
    suspend fun getPdfFile(path: String): PdfFile?

    /** Renames the file at [path] (".pdf" is appended if missing). Success carries the new absolute path. */
    suspend fun renamePdf(path: String, newName: String): Result<String>

    /**
     * Deletes the PDF at [path] on the IO dispatcher: File API first, MediaStore as backup. A file that is already
     * gone counts as success (its stale MediaStore row is cleaned). Refreshes the shared list before returning.
     */
    suspend fun deletePdf(path: String): Result<Unit>

    /** Batch variant of [deletePdf]: one result per path (same order), a single list refresh at the end. */
    suspend fun deletePdfs(paths: List<String>): List<Result<Unit>>
    suspend fun refreshPdfs()
}
