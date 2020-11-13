package org.bibletranslationtools.fetcher

import java.io.File
import java.io.FileNotFoundException
import org.bibletranslationtools.fetcher.data.Chapter
import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.impl.repository.AvailabilityCacheRepo
import org.bibletranslationtools.fetcher.impl.repository.BookCatalogImpl
import org.bibletranslationtools.fetcher.impl.repository.BookRepositoryImpl
import org.bibletranslationtools.fetcher.impl.repository.ContentAvailabilityCacheBuilder
import org.bibletranslationtools.fetcher.impl.repository.StorageAccessImpl
import org.bibletranslationtools.fetcher.repository.ChapterCatalog
import org.bibletranslationtools.fetcher.repository.DirectoryProvider
import org.bibletranslationtools.fetcher.repository.LanguageCatalog
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class ContentAvailabilityCacheTest {
    private val languageCode = "en"
    private val titus = "tit"
    private val chapterNumber = 1
    private val rcFileName = "en_ulb.zip"

    @Test
    fun testCacheContent() {
        val tempDir = createTempDir("testDir")
        getTestRCFile().copyTo(tempDir.resolve(rcFileName))
        val chapterPath = tempDir.resolve(
            "en/ulb/tit/$chapterNumber/CONTENTS/mp3/hi/chapter"
        ).apply { mkdirs() }
        chapterPath.resolve("en_ulb_nt_tit_c$chapterNumber.mp3").createNewFile()

        val mockLanguageCatalog = mock(LanguageCatalog::class.java)
        val mockChapterCatalog = mock(ChapterCatalog::class.java)
        val mockDirectoryProvider = mock(DirectoryProvider::class.java)
        val mockStorageAccess: StorageAccess = StorageAccessImpl(mockDirectoryProvider)
        val bookRepository = BookRepositoryImpl(BookCatalogImpl())

        `when`(mockDirectoryProvider.getRCRepositoriesDir()).thenReturn(tempDir)
        `when`(mockDirectoryProvider.getContentRoot()).thenReturn(tempDir)
        `when`(mockLanguageCatalog.getAll()).thenReturn(
            listOf(Language("en", "", "", true))
        )
        `when`(
            mockChapterCatalog.getAll(anyString(), anyString())
        ).thenReturn(
            listOf(Chapter(chapterNumber))
        )

        val cacheBuilder = ContentAvailabilityCacheBuilder(
            mockLanguageCatalog,
            mockChapterCatalog,
            bookRepository,
            mockStorageAccess,
            mockDirectoryProvider
        )
        val cache = AvailabilityCacheRepo(cacheBuilder)

        assertTrue(cache.isLanguageAvailable(languageCode))
        assertTrue(cache.isBookAvailable(titus, languageCode, "orature"))
        assertNotNull(cache.getChapterUrl(chapterNumber, titus, languageCode, "mp3"))
        assertNotNull(cache.getChapterUrl(chapterNumber, titus, languageCode, "orature"))
        assertNull(cache.getChapterUrl(chapterNumber, titus, languageCode, "bttr"))

        tempDir.deleteRecursively()
    }

    @Throws(FileNotFoundException::class)
    private fun getTestRCFile(): File {
        val rcFilePath = javaClass.classLoader.getResource(rcFileName)
            ?: throw(FileNotFoundException("Test resource not found: $rcFileName"))
        return File(rcFilePath.file)
    }
}
