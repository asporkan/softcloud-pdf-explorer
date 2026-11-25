package com.rejowan.pdfreaderpro.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rejowan.pdfreaderpro.data.local.PasswordStorage
import com.rejowan.pdfreaderpro.data.local.database.dao.BookmarkDao
import com.rejowan.pdfreaderpro.data.local.database.entity.BookmarkEntity
import com.rejowan.pdfreaderpro.domain.model.AppPreferences
import com.rejowan.pdfreaderpro.domain.model.PdfFile
import com.rejowan.pdfreaderpro.domain.repository.FavoriteRepository
import com.rejowan.pdfreaderpro.domain.repository.PdfFileRepository
import com.rejowan.pdfreaderpro.domain.repository.PreferencesRepository
import com.rejowan.pdfreaderpro.domain.repository.RecentRepository
import com.rejowan.pdfreaderpro.R
import com.rejowan.pdfreaderpro.util.DownloadsWriter
import com.rejowan.pdfreaderpro.presentation.screens.reader.ReaderAction
import com.rejowan.pdfreaderpro.presentation.screens.reader.ReaderEvent
import com.rejowan.pdfreaderpro.presentation.screens.reader.ReaderError
import com.rejowan.pdfreaderpro.presentation.screens.reader.components.AttachmentItem
import com.rejowan.pdfreaderpro.presentation.screens.reader.ReaderViewModel
import com.rejowan.pdfreaderpro.presentation.screens.reader.ReadingTheme
import com.rejowan.pdfreaderpro.presentation.screens.reader.ScrollMode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var recentRepository: RecentRepository
    private lateinit var favoriteRepository: FavoriteRepository
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var bookmarkDao: BookmarkDao
    private lateinit var pdfFileRepository: PdfFileRepository
    private lateinit var applicationContext: Context
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var passwordStorage: PasswordStorage
    private lateinit var downloadsWriter: DownloadsWriter
    private var pdfExists = true
    private lateinit var viewModel: ReaderViewModel

    private val testPdfPath = "/storage/test.pdf"
    private val mockUri: Uri = mockk(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        recentRepository = mockk(relaxed = true)
        favoriteRepository = mockk(relaxed = true)
        preferencesRepository = mockk(relaxed = true)
        bookmarkDao = mockk(relaxed = true)
        pdfFileRepository = mockk(relaxed = true)
        applicationContext = mockk(relaxed = true)
        passwordStorage = mockk(relaxed = true)
        downloadsWriter = mockk(relaxed = true)
        pdfExists = true

        savedStateHandle = SavedStateHandle(mapOf("path" to testPdfPath, "initialPage" to 0))

        // Mock Uri.fromFile static method
        mockkStatic(Uri::class)
        every { Uri.fromFile(any()) } returns mockUri

        // Default mocks
        every { preferencesRepository.preferences } returns flowOf(AppPreferences())
        every { bookmarkDao.getBookmarksForPdf(any()) } returns flowOf(emptyList())
        coEvery { favoriteRepository.isFavorite(any()) } returns false
        coEvery { recentRepository.getLastPage(any()) } returns null
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        unmockkStatic(Uri::class)
    }

    private fun createViewModel(): ReaderViewModel {
        return ReaderViewModel(
            recentRepository = recentRepository,
            favoriteRepository = favoriteRepository,
            preferencesRepository = preferencesRepository,
            bookmarkDao = bookmarkDao,
            pdfFileRepository = pdfFileRepository,
            applicationContext = applicationContext,
            savedStateHandle = savedStateHandle,
            passwordStorage = passwordStorage,
            downloadsWriter = downloadsWriter,
            fileExists = { pdfExists }
        )
    }

    // region Test Data
    private fun createBookmark(
        id: Long = 1L,
        pdfPath: String = testPdfPath,
        pageNumber: Int = 0,
        title: String = "Bookmark"
    ) = BookmarkEntity(
        id = id,
        pdfPath = pdfPath,
        pageNumber = pageNumber,
        title = title
    )
    // endregion

    // region Initial State Tests
    @Test
    fun `initial state has correct document path`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(testPdfPath, viewModel.pdfPath)
        assertEquals(testPdfPath, viewModel.state.value.documentPath)
    }

    @Test
    fun `initial state extracts document title from path`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals("test", viewModel.state.value.documentTitle)
    }

    @Test
    fun `initial state loads preferences`() = runTest {
        val customPrefs = AppPreferences(
            readerBrightness = 0.8f,
            readerTheme = com.rejowan.pdfreaderpro.domain.model.ReadingTheme.DARK
        )
        every { preferencesRepository.preferences } returns flowOf(customPrefs)

        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(0.8f, viewModel.state.value.brightness)
        assertEquals(ReadingTheme.DARK, viewModel.state.value.readingTheme)
    }

    @Test
    fun `initial state loads favorite status`() = runTest {
        coEvery { favoriteRepository.isFavorite(testPdfPath) } returns true

        viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isFavorite)
    }

    @Test
    fun `initial state is loading`() = runTest {
        viewModel = createViewModel()

        assertTrue(viewModel.state.value.isLoading)
    }
    // endregion

    // region Bookmark Tests
    @Test
    fun `bookmarks flow updates state`() = runTest {
        val bookmarks = listOf(
            createBookmark(pageNumber = 0),
            createBookmark(pageNumber = 5)
        )
        every { bookmarkDao.getBookmarksForPdf(testPdfPath) } returns flowOf(bookmarks)

        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(2, viewModel.state.value.bookmarks.size)
    }

    @Test
    fun `current page bookmark status is updated`() = runTest {
        val bookmarks = listOf(createBookmark(pageNumber = 0))
        every { bookmarkDao.getBookmarksForPdf(testPdfPath) } returns flowOf(bookmarks)

        viewModel = createViewModel()
        advanceUntilIdle()

        // Current page is 0, which is bookmarked
        assertTrue(viewModel.state.value.isCurrentPageBookmarked)
    }

    @Test
    fun `toggle page bookmark adds bookmark when not bookmarked`() = runTest {
        every { bookmarkDao.getBookmarksForPdf(testPdfPath) } returns flowOf(emptyList())
        val bookmarkSlot = slot<BookmarkEntity>()
        coEvery { bookmarkDao.insert(capture(bookmarkSlot)) } returns 1L

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(ReaderAction.TogglePageBookmark)
        advanceUntilIdle()

        coVerify { bookmarkDao.insert(any()) }
        assertEquals(testPdfPath, bookmarkSlot.captured.pdfPath)
        assertEquals(0, bookmarkSlot.captured.pageNumber)
    }

    @Test
    fun `toggle page bookmark removes bookmark when bookmarked`() = runTest {
        val bookmark = createBookmark(pageNumber = 0)
        every { bookmarkDao.getBookmarksForPdf(testPdfPath) } returns flowOf(listOf(bookmark))

        viewModel = createViewModel()
        advanceUntilIdle()

        // State should show page is bookmarked
        assertTrue(viewModel.state.value.isCurrentPageBookmarked)

        viewModel.onAction(ReaderAction.TogglePageBookmark)
        advanceUntilIdle()

        coVerify { bookmarkDao.deleteByPage(testPdfPath, 0) }
    }

    @Test
    fun `delete bookmark calls dao delete`() = runTest {
        val bookmark = createBookmark()
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(ReaderAction.DeleteBookmark(bookmark))
        advanceUntilIdle()

        coVerify { bookmarkDao.delete(bookmark) }
    }
    // endregion

    // region Navigation Tests
    @Test
    fun `go to page updates current page state`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(ReaderAction.GoToPage(5))
        advanceUntilIdle()

        assertEquals(5, viewModel.state.value.currentPage)
    }
    // endregion

    // region Toolbar Tests
    @Test
    fun `toggle toolbar flips visibility`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val initialVisibility = viewModel.state.value.isToolbarVisible

        viewModel.onAction(ReaderAction.ToggleToolbar)
        advanceUntilIdle()

        assertEquals(!initialVisibility, viewModel.state.value.isToolbarVisible)
    }

    @Test
    fun `toggle toolbar exits full screen mode`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        // Enter full screen
        viewModel.onAction(ReaderAction.ToggleFullScreen)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isFullScreen)

        // Toggle toolbar should exit full screen and show toolbar
        viewModel.onAction(ReaderAction.ToggleToolbar)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isFullScreen)
        assertTrue(viewModel.state.value.isToolbarVisible)
    }
    // endregion

    // region Display Settings Tests
    @Test
    fun `set brightness updates state and persists`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(ReaderAction.SetBrightness(0.7f))
        advanceUntilIdle()

        assertEquals(0.7f, viewModel.state.value.brightness)
        coVerify { preferencesRepository.setReaderBrightness(0.7f) }
    }

    @Test
    fun `set scroll mode updates state and persists`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(ReaderAction.SetScrollMode(ScrollMode.HORIZONTAL))
        advanceUntilIdle()

        assertEquals(ScrollMode.HORIZONTAL, viewModel.state.value.scrollMode)
        coVerify { preferencesRepository.setReaderScrollMode(any()) }
    }

    @Test
    fun `set reading theme updates state and persists`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(ReaderAction.SetReadingTheme(ReadingTheme.SEPIA))
        advanceUntilIdle()

        assertEquals(ReadingTheme.SEPIA, viewModel.state.value.readingTheme)
        coVerify { preferencesRepository.setReaderTheme(any()) }
    }

    @Test
    fun `set keep screen on updates state and persists`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(ReaderAction.SetKeepScreenOn(true))
        advanceUntilIdle()

        assertTrue(viewModel.state.value.keepScreenOn)
        coVerify { preferencesRepository.setReaderKeepScreenOn(true) }
    }
    // endregion

    // region Sheet Visibility Tests
    @Test
    fun `show page jump dialog updates state`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(ReaderAction.ShowPageJumpDialog)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isPageJumpDialogVisible)
    }

    @Test
    fun `hide page jump dialog updates state`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(ReaderAction.ShowPageJumpDialog)
        viewModel.onAction(ReaderAction.HidePageJumpDialog)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isPageJumpDialogVisible)
    }

    @Test
    fun `show bookmarks sheet updates state`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(ReaderAction.ShowBookmarksSheet)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isBookmarksSheetVisible)
    }

    @Test
    fun `show zoom sheet updates state`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(ReaderAction.ShowZoomSheet)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isZoomSheetVisible)
    }

    @Test
    fun `show display sheet updates state`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(ReaderAction.ShowDisplaySheet)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isDisplaySheetVisible)
    }
    // endregion

    // region Favorite Tests
    @Test
    fun `add to favorite calls repository and updates state`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(ReaderAction.AddToFavorite)
        advanceUntilIdle()

        coVerify { favoriteRepository.addFavorite(any()) }
    }

    @Test
    fun `confirm remove favorite calls repository`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(ReaderAction.ConfirmRemoveFavorite)
        advanceUntilIdle()

        coVerify { favoriteRepository.removeFavorite(testPdfPath) }
    }

    @Test
    fun `isFavorite returns repository value`() = runTest {
        coEvery { favoriteRepository.isFavorite(testPdfPath) } returns true

        viewModel = createViewModel()
        advanceUntilIdle()

        val result = viewModel.isFavorite()

        assertTrue(result)
    }
    // endregion

    // region Search Tests
    @Test
    fun `search action updates search query state`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(ReaderAction.Search("test query"))
        advanceUntilIdle()

        assertEquals("test query", viewModel.state.value.searchQuery)
    }

    @Test
    fun `clear search resets search state`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(ReaderAction.Search("test"))
        viewModel.onAction(ReaderAction.ClearSearch)
        advanceUntilIdle()

        assertEquals("", viewModel.state.value.searchQuery)
        assertEquals(0, viewModel.state.value.searchResultCount)
    }

    @Test
    fun `toggle search updates search active state`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(ReaderAction.ToggleSearch)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isSearchActive)
    }

    @Test
    fun `single search match is reported as first of one`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onAction(ReaderAction.Search("rare"))

        // PDF.js reports the current match 1-based
        viewModel.onSearchMatchChanged(current = 1, total = 1)

        assertEquals(1, viewModel.state.value.searchResultCount)
        // State is 0-based, the search bar displays index + 1 => "1/1"
        assertEquals(0, viewModel.state.value.currentSearchIndex)
    }

    @Test
    fun `multiple search matches keep current index zero based`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onAction(ReaderAction.Search("common"))

        viewModel.onSearchMatchChanged(current = 3, total = 5)

        assertEquals(5, viewModel.state.value.searchResultCount)
        assertEquals(2, viewModel.state.value.currentSearchIndex)
    }

    @Test
    fun `search without matches reports zero count`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onAction(ReaderAction.Search("missing"))

        viewModel.onSearchMatchChanged(current = 0, total = 0)

        assertEquals(0, viewModel.state.value.searchResultCount)
        assertEquals(0, viewModel.state.value.currentSearchIndex)
    }

    @Test
    fun `search match updates are ignored when no query is active`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSearchMatchChanged(current = 1, total = 4)

        assertEquals(0, viewModel.state.value.searchResultCount)
        assertEquals(0, viewModel.state.value.currentSearchIndex)
    }
    // endregion

    // region Auto Scroll Tests
    @Test
    fun `start auto scroll updates state`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(ReaderAction.StartAutoScroll(50f))
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isAutoScrollActive)
        assertEquals(50f, viewModel.state.value.autoScrollSpeed)
        assertFalse(viewModel.state.value.isToolbarVisible)
    }

    @Test
    fun `stop auto scroll updates state`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(ReaderAction.StartAutoScroll(50f))
        viewModel.onAction(ReaderAction.StopAutoScroll)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isAutoScrollActive)
    }

    @Test
    fun `toggle auto scroll pause updates state`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(ReaderAction.StartAutoScroll(50f))
        viewModel.onAction(ReaderAction.ToggleAutoScrollPause)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isAutoScrollPaused)
    }

    @Test
    fun `set auto scroll speed updates state`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(ReaderAction.SetAutoScrollSpeed(75f))
        advanceUntilIdle()

        assertEquals(75f, viewModel.state.value.autoScrollSpeed)
    }
    // endregion

    // region Page Rotation Tests
    @Test
    fun `rotate clockwise increases rotation by 90`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val initialRotation = viewModel.state.value.pageRotation

        viewModel.onAction(ReaderAction.RotateClockwise)
        advanceUntilIdle()

        assertEquals((initialRotation + 90) % 360, viewModel.state.value.pageRotation)
    }

    @Test
    fun `rotate counter clockwise decreases rotation by 90`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        // Start at 90 to avoid negative
        viewModel.onAction(ReaderAction.RotateClockwise)
        advanceUntilIdle()
        assertEquals(90, viewModel.state.value.pageRotation)

        viewModel.onAction(ReaderAction.RotateCounterClockwise)
        advanceUntilIdle()

        assertEquals(0, viewModel.state.value.pageRotation)
    }
    // endregion

    // region Zoom Tests
    @Test
    fun `set zoom updates state within bounds`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(ReaderAction.SetZoom(2.0f))
        advanceUntilIdle()

        assertEquals(2.0f, viewModel.state.value.zoom)
    }

    @Test
    fun `set zoom clamps to max zoom`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val maxZoom = viewModel.state.value.maxZoom
        viewModel.onAction(ReaderAction.SetZoom(maxZoom + 10f))
        advanceUntilIdle()

        assertEquals(maxZoom, viewModel.state.value.zoom)
    }

    @Test
    fun `set zoom clamps to min zoom`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val minZoom = viewModel.state.value.minZoom
        viewModel.onAction(ReaderAction.SetZoom(minZoom - 1f))
        advanceUntilIdle()

        assertEquals(minZoom, viewModel.state.value.zoom)
    }
    // endregion

    // region Event Tests
    @Test
    fun `share document sends event`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onAction(ReaderAction.ShareDocument)
            advanceUntilIdle()

            assertEquals(ReaderEvent.ShareDocument, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `close document sends event`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onAction(ReaderAction.CloseDocument)
            advanceUntilIdle()

            assertEquals(ReaderEvent.DocumentClosed, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
    // endregion

    // region Utility Method Tests
    @Test
    fun `getDocumentFileName returns correct filename`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val result = viewModel.getDocumentFileName()

        assertEquals("test.pdf", result)
    }
    // endregion

    @Test
    fun `confirm delete uses repository and cleans up recent and favorites`() = runTest {
        coEvery { pdfFileRepository.deletePdf(testPdfPath) } returns Result.success(Unit)
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(ReaderAction.ConfirmDelete)
        advanceUntilIdle()

        coVerify { pdfFileRepository.deletePdf(testPdfPath) }
        coVerify { recentRepository.removeRecent(testPdfPath) }
        coVerify { favoriteRepository.removeFavorite(testPdfPath) }
    }

    @Test
    fun `failed delete keeps recent and favorites`() = runTest {
        coEvery { pdfFileRepository.deletePdf(testPdfPath) } returns Result.failure(SecurityException("denied"))
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(ReaderAction.ConfirmDelete)
        advanceUntilIdle()

        coVerify(exactly = 0) { recentRepository.removeRecent(testPdfPath) }
        coVerify(exactly = 0) { favoriteRepository.removeFavorite(testPdfPath) }
    }

    // region Attachment save (B28)
    private val attachment = AttachmentItem(title = "a.txt", id = "att1", dest = null)

    @Test
    fun `downloaded attachment is saved through the writer and reports the stored name`() = runTest {
        coEvery { downloadsWriter.save(any(), "report.txt", "text/plain") } returns Result.success("report (1).txt")
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onAction(ReaderAction.DownloadAttachment(attachment))

        viewModel.events.test {
            viewModel.onAttachmentBytes(byteArrayOf(1, 2, 3), "report.txt", "text/plain")
            advanceUntilIdle()

            val event = awaitItem() as ReaderEvent.ShowMessage
            assertEquals(R.string.reader_msg_saved_to_downloads, event.messageRes)
            // The real (MediaStore-assigned) name is shown, not the requested one
            assertEquals(listOf<Any>("report (1).txt"), event.args)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 1) {
            downloadsWriter.save(match { it.contentEquals(byteArrayOf(1, 2, 3)) }, "report.txt", "text/plain")
        }
    }

    @Test
    fun `download without a pending action still saves`() = runTest {
        coEvery { downloadsWriter.save(any(), any(), any()) } returns Result.success("x.bin")
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAttachmentBytes(byteArrayOf(9), "x.bin", null)
        advanceUntilIdle()

        coVerify(exactly = 1) { downloadsWriter.save(any(), "x.bin", null) }
    }

    @Test
    fun `attachment without a name gets a generated name`() = runTest {
        val nameSlot = slot<String?>()
        coEvery { downloadsWriter.save(any(), captureNullable(nameSlot), any()) } returns Result.success("attachment")
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onAction(ReaderAction.DownloadAttachment(attachment))

        viewModel.onAttachmentBytes(byteArrayOf(1), null, null)
        advanceUntilIdle()

        assertTrue(nameSlot.captured!!.startsWith("attachment_"))
    }

    @Test
    fun `failed attachment save sends an error event instead of a success message`() = runTest {
        coEvery { downloadsWriter.save(any(), any(), any()) } returns Result.failure(java.io.IOException("disk full"))
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onAction(ReaderAction.DownloadAttachment(attachment))

        viewModel.events.test {
            viewModel.onAttachmentBytes(byteArrayOf(1), "a.txt", "text/plain")
            advanceUntilIdle()

            val event = awaitItem() as ReaderEvent.Error
            assertEquals(R.string.reader_err_save_attachment, event.messageRes)
            assertEquals(listOf<Any>("disk full"), event.args)
            cancelAndIgnoreRemainingEvents()
        }
    }
    // endregion

    // region Retry (B29)
    @Test
    fun `load failure shows the error screen state`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onLoadFailed(java.io.IOException("boom"))

        assertEquals(ReaderError(R.string.error_loading_pdf), viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `retry clears the error and shows loading again`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onLoadFailed(java.io.IOException("boom"))
        assertNotNull(viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoading)

        viewModel.onAction(ReaderAction.Retry)
        advanceUntilIdle()

        assertNull(viewModel.state.value.error)
        assertTrue(viewModel.state.value.isLoading)
    }

    @Test
    fun `retry with a missing file keeps the error screen with a not found message`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onLoadFailed(java.io.IOException("boom"))
        pdfExists = false

        viewModel.onAction(ReaderAction.Retry)
        advanceUntilIdle()

        assertEquals(ReaderError(R.string.error_file_not_found), viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `retry resets password flags`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onLoadFailed(java.io.IOException("boom"))

        viewModel.onAction(ReaderAction.Retry)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isPasswordRequired)
        assertFalse(state.isPasswordError)
        assertFalse(state.passwordSubmitted)
    }
    // endregion
}
