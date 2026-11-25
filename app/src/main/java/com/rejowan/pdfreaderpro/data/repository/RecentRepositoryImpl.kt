package com.rejowan.pdfreaderpro.data.repository

import com.rejowan.pdfreaderpro.data.local.database.dao.RecentDao
import com.rejowan.pdfreaderpro.data.local.database.entity.RecentEntity
import com.rejowan.pdfreaderpro.domain.model.RecentFile
import com.rejowan.pdfreaderpro.domain.repository.RecentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

class RecentRepositoryImpl(
    private val recentDao: RecentDao,
    /** False while the app cannot see other apps' files; then absence proves nothing. */
    private val canVerifyFiles: () -> Boolean = { true }
) : RecentRepository {

    override fun getRecentFiles(): Flow<List<RecentFile>> {
        return recentDao.getAllRecent().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addOrUpdateRecent(
        path: String,
        name: String,
        size: Long,
        totalPages: Int,
        currentPage: Int
    ) {
        val entity = RecentEntity(
            name = name,
            path = path,
            size = size,
            lastOpened = System.currentTimeMillis(),
            totalPages = totalPages,
            lastPage = currentPage
        )
        recentDao.upsert(entity)
    }

    override suspend fun updateLastPage(path: String, page: Int) {
        recentDao.updateLastPage(path, page)
    }

    override suspend fun getLastPage(path: String): Int? {
        return recentDao.getByPath(path)?.lastPage
    }

    override suspend fun removeRecent(path: String) {
        recentDao.deleteByPath(path)
    }

    override suspend fun clearAllRecent() {
        recentDao.clearAll()
    }

    override suspend fun getRecentCount(): Int {
        return recentDao.getCount()
    }

    override suspend fun updatePath(oldPath: String, newPath: String, newName: String) {
        recentDao.updatePath(oldPath, newPath, newName)
    }

    override suspend fun cleanupMissingFiles() {
        // Without storage access File.exists() is false for files that exist; never wipe history on that basis.
        if (!canVerifyFiles()) {
            Timber.d("Skipping recent cleanup: storage access unavailable")
            return
        }
        withContext(Dispatchers.IO) {
            val recentFiles = recentDao.getAllRecentSync()
            Timber.d("Checking ${recentFiles.size} recent files for missing files")
            recentFiles.forEach { entity ->
                val file = File(entity.path)
                if (!file.exists()) {
                    Timber.d("Removing missing recent: ${entity.path}")
                    recentDao.deleteByPath(entity.path)
                }
            }
        }
    }

    private fun RecentEntity.toDomain(): RecentFile {
        return RecentFile(
            id = id,
            name = name,
            path = path,
            size = size,
            lastOpened = lastOpened,
            totalPages = totalPages,
            lastPage = lastPage
        )
    }
}
