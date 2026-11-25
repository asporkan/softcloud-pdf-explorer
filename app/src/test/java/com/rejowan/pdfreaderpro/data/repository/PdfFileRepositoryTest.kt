package com.rejowan.pdfreaderpro.data.repository

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import app.cash.turbine.test
import com.rejowan.pdfreaderpro.domain.model.PdfFile
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Modifier

@OptIn(ExperimentalCoroutinesApi::class)
class PdfFileRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var repository: PdfFileRepositoryImpl

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val mockUri: Uri = mockk(relaxed = true)
    private val mockCollectionUri: Uri = mockk(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        context = mockk(relaxed = true)
        contentResolver = mockk(relaxed = true)

        every { context.contentResolver } returns contentResolver

        // Mock MediaStore static methods
        mockkStatic(MediaStore.Files::class)
        every { MediaStore.Files.getContentUri(any<String>()) } returns mockCollectionUri

        // Mock Uri static methods
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockUri
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        unmockkStatic(MediaStore.Files::class)
        unmockkStatic(Uri::class)
    }

    private fun createRepository(): PdfFileRepositoryImpl {
        return PdfFileRepositoryImpl(context = context)
    }

    // region getAllPdfFiles Tests
    @Test
    fun `getAllPdfFiles returns empty list initially`() = runTest {
        // Mock contentResolver.query to return null (no PDFs found)
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns null

        repository = createRepository()

        repository.getAllPdfFiles().test {
            val files = awaitItem()
            assertTrue(files.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getAllPdfFiles returns flow`() = runTest {
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns null

        repository = createRepository()

        assertNotNull(repository.getAllPdfFiles())
    }
    // endregion

    // region getPdfsByFolder Tests
    @Test
    fun `getPdfsByFolder returns empty list for non-existent folder`() = runTest {
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns null

        repository = createRepository()

        repository.getPdfsByFolder("/nonexistent/folder").test {
            val files = awaitItem()
            assertTrue(files.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getPdfsByFolder returns flow`() = runTest {
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns null

        repository = createRepository()

        assertNotNull(repository.getPdfsByFolder("/some/folder"))
    }
    // endregion

    // region searchPdfs Tests
    @Test
    fun `searchPdfs returns empty list for blank query`() = runTest {
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns null

        repository = createRepository()

        repository.searchPdfs("").test {
            val files = awaitItem()
            assertTrue(files.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `searchPdfs returns empty list for whitespace query`() = runTest {
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns null

        repository = createRepository()

        repository.searchPdfs("   ").test {
            val files = awaitItem()
            assertTrue(files.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `searchPdfs returns flow`() = runTest {
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns null

        repository = createRepository()

        assertNotNull(repository.searchPdfs("test"))
    }
    // endregion

    // region getPdfFolders Tests
    @Test
    fun `getPdfFolders returns empty list initially`() = runTest {
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns null

        repository = createRepository()

        repository.getPdfFolders().test {
            val folders = awaitItem()
            assertTrue(folders.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getPdfFolders returns flow`() = runTest {
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns null

        repository = createRepository()

        assertNotNull(repository.getPdfFolders())
    }
    // endregion

    // region getPdfFile Tests
    @Test
    fun `getPdfFile returns null for non-existent path`() = runTest {
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns null

        repository = createRepository()
        advanceUntilIdle()

        val result = repository.getPdfFile("/nonexistent/file.pdf")
        assertNull(result)
    }

    @Test
    fun `getPdfFile returns null when no files loaded`() = runTest {
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns null

        repository = createRepository()
        advanceUntilIdle()

        val result = repository.getPdfFile("/storage/docs/test.pdf")
        assertNull(result)
    }
    // endregion

    // region renamePdf Tests
    @Test
    fun `renamePdf with invalid path returns failure`() = runTest {
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns null

        repository = createRepository()

        val result = repository.renamePdf("/nonexistent/path/test.pdf", "newname")

        assertTrue(result.isFailure)
    }

    @Test
    fun `renamePdf renames existing file and returns the new path`() = runTest {
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns null
        val original = tempFolder.newFile("old.pdf")

        repository = createRepository()

        val result = repository.renamePdf(original.absolutePath, "new")

        assertTrue(result.isSuccess)
        assertEquals(File(original.parentFile, "new.pdf").absolutePath, result.getOrNull())
        assertFalse(original.exists())
        assertTrue(File(original.parentFile, "new.pdf").exists())
    }
    // endregion

    // region deletePdf Tests
    @Test
    fun `deletePdf deletes an existing file`() = runTest {
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns null
        every { contentResolver.delete(any(), any(), any()) } returns 0
        val file = tempFolder.newFile("to_delete.pdf")

        repository = createRepository()

        val result = repository.deletePdf(file.absolutePath)

        assertTrue(result.isSuccess)
        assertFalse(file.exists())
    }

    @Test
    fun `deletePdf treats a missing file as success and cleans the stale MediaStore row`() = runTest {
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns null
        every { contentResolver.delete(any(), any(), any()) } returns 1
        val missing = File(tempFolder.root, "already_gone.pdf")

        repository = createRepository()

        val result = repository.deletePdf(missing.absolutePath)

        assertTrue(result.isSuccess)
        verify { contentResolver.delete(any(), any(), arrayOf(missing.absolutePath)) }
    }

    @Test
    fun `deletePdf refreshes the list after deleting`() = runTest {
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns null
        every { contentResolver.delete(any(), any(), any()) } returns 0
        val file = tempFolder.newFile("refresh_me.pdf")

        repository = createRepository()
        repository.deletePdf(file.absolutePath)

        verify(atLeast = 1) { contentResolver.query(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `deletePdf returns failure when file and MediaStore both refuse`() = runTest {
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns null
        every { contentResolver.delete(any(), any(), any()) } throws SecurityException("not owner")
        // A non-empty directory cannot be removed by File.delete().
        val stubborn = tempFolder.newFolder("stubborn.pdf")
        File(stubborn, "child.txt").createNewFile()

        repository = createRepository()

        val result = repository.deletePdf(stubborn.absolutePath)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
        assertTrue(stubborn.exists())
    }

    @Test
    fun `deletePdfs reports one result per path in order`() = runTest {
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns null
        every { contentResolver.delete(any(), any(), any()) } returns 0
        val first = tempFolder.newFile("first.pdf")
        val second = tempFolder.newFile("second.pdf")
        val missing = File(tempFolder.root, "missing.pdf")

        repository = createRepository()

        val results = repository.deletePdfs(
            listOf(first.absolutePath, missing.absolutePath, second.absolutePath)
        )

        assertEquals(3, results.size)
        assertTrue(results.all { it.isSuccess })
        assertFalse(first.exists())
        assertFalse(second.exists())
    }
    // endregion

    // region refreshPdfs Tests
    @Test
    fun `refreshPdfs does not throw exception`() = runTest {
        // Mock contentResolver.query to return null (simulating no access or error)
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns null

        repository = createRepository()
        advanceUntilIdle()

        // Should not throw
        repository.refreshPdfs()
        advanceUntilIdle()

        // Files should still be empty
        repository.getAllPdfFiles().test {
            val files = awaitItem()
            assertTrue(files.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refreshPdfs handles empty cursor`() = runTest {
        val cursor = mockk<Cursor>(relaxed = true)
        every { cursor.moveToNext() } returns false
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns cursor

        repository = createRepository()
        repository.refreshPdfs()
        advanceUntilIdle()

        repository.getAllPdfFiles().test {
            val files = awaitItem()
            assertTrue(files.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }
    // endregion
}
