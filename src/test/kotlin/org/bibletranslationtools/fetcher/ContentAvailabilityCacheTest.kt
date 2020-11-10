package org.bibletranslationtools.fetcher

import java.io.File
import java.io.FileNotFoundException
import org.bibletranslationtools.fetcher.data.Chapter
import org.bibletranslationtools.fetcher.impl.repository.ContentAvailabilityCache
import org.bibletranslationtools.fetcher.impl.repository.StorageAccessImpl
import org.bibletranslationtools.fetcher.repository.ChapterCatalog
import org.bibletranslationtools.fetcher.repository.DirectoryProvider
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.bibletranslationtools.fetcher.usecase.DependencyResolver
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class ContentAvailabilityCacheTest {
    private val languageCode = "en"
    private val titusSlug = "tit"
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

        val mockDirectoryProvider = mock(DirectoryProvider::class.java)
        val mockStorageAccess: StorageAccess = StorageAccessImpl(mockDirectoryProvider)
        val mockChapterCatalog = mock(ChapterCatalog::class.java)

        `when`(mockDirectoryProvider.getRCRepositoriesDir()).thenReturn(tempDir)
        `when`(mockDirectoryProvider.getContentRoot()).thenReturn(tempDir)
        `when`(
            mockChapterCatalog.getAll(anyString(), anyString())
        ).thenReturn(
            listOf(Chapter(chapterNumber))
        )

        val cache = ContentAvailabilityCache(
            mockChapterCatalog,
            DependencyResolver.bookRepository,
            mockStorageAccess,
            mockDirectoryProvider
        )

        assertTrue(cache.isChapterAvailable(chapterNumber, titusSlug, languageCode, "mp3"))
        assertTrue(cache.isChapterAvailable(chapterNumber, titusSlug, languageCode, "orature"))
        assertFalse(cache.isChapterAvailable(chapterNumber, titusSlug, languageCode, "bttr"))
        println(tempDir.deleteRecursively())
    }

    @Throws(FileNotFoundException::class)
    private fun getTestRCFile(): File {
        val rcFilePath = javaClass.classLoader.getResource(rcFileName)
            ?: throw(FileNotFoundException("Test resource not found: $rcFileName"))
        return File(rcFilePath.file)
    }
}
