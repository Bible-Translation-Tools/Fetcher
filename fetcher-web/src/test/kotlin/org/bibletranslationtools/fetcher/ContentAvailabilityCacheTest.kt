package org.bibletranslationtools.fetcher

import com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable
import java.io.File
import java.io.FileNotFoundException
import org.bibletranslationtools.fetcher.config.EnvironmentConfig
import org.bibletranslationtools.fetcher.data.Chapter
import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.impl.repository.AvailabilityCacheAccessor
import org.bibletranslationtools.fetcher.impl.repository.BookCatalogImpl
import org.bibletranslationtools.fetcher.impl.repository.BookRepositoryImpl
import org.bibletranslationtools.fetcher.impl.repository.ContentAvailabilityCacheBuilder
import org.bibletranslationtools.fetcher.impl.repository.ProductCatalogImpl
import org.bibletranslationtools.fetcher.impl.repository.StorageAccessImpl
import org.bibletranslationtools.fetcher.repository.ChapterCatalog
import org.bibletranslationtools.fetcher.repository.DirectoryProvider
import org.bibletranslationtools.fetcher.repository.LanguageCatalog
import org.bibletranslationtools.fetcher.repository.ResourceContainerRepository
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.bibletranslationtools.fetcher.usecase.RequestResourceContainer
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.wycliffeassociates.rcmediadownloader.data.MediaType

class ContentAvailabilityCacheTest {
    private val languageCode = "en"
    private val `2peter` = "2pe"
    private val chapterNumber = 1

    /**
     *  Note: the urls in media.yaml will need to be up-to-date,
     *  otherwise this test may fail
     */
    @Test
    fun testCacheContent() {
        val tempDir = createTempDir("testDir")

        CreateResourcesForBuildingCache(tempDir)

        val mockLanguageCatalog = mock(LanguageCatalog::class.java)
        val mockChapterCatalog = mock(ChapterCatalog::class.java)
        val mockDirectoryProvider = mock(DirectoryProvider::class.java)
        val mockStorageAccess: StorageAccess = StorageAccessImpl(mockDirectoryProvider)

        `when`(mockDirectoryProvider.getContentRoot()).thenReturn(tempDir)
        `when`(mockLanguageCatalog.getAll()).thenReturn(
            listOf(Language("en", "", "", true))
        )
        `when`(
            mockChapterCatalog.getAll(anyString(), anyString())
        ).thenReturn(
            listOf(Chapter(chapterNumber))
        )

        withEnvironmentVariable("CONTENT_ROOT", tempDir.path)
            .and("CDN_BASE_URL", "unused")
            .and("CDN_BASE_RC_URL", "unused")
            .and("CACHE_REFRESH_MINUTES", "unused")
            .and("ORATURE_REPO_DIR", "unused")
            .and("RC_TEMP_DIR", "unused")
            .execute {
                val cacheBuilder = ContentAvailabilityCacheBuilder(
                    EnvironmentConfig(),
                    mockLanguageCatalog,
                    ProductCatalogImpl(),
                    mockChapterCatalog,
                    BookRepositoryImpl(BookCatalogImpl()),
                    mockStorageAccess
                )
                val cache = AvailabilityCacheAccessor(cacheBuilder)
                assertTrue(cache.isLanguageAvailable(languageCode))
                assertTrue(cache.isBookAvailable(`2peter`, languageCode, "orature"))
                assertNotNull(cache.getChapterUrl(chapterNumber, `2peter`, languageCode, "mp3"))
                assertNotNull(cache.getChapterUrl(chapterNumber, `2peter`, languageCode, "orature"))
                assertNull(cache.getChapterUrl(chapterNumber, `2peter`, languageCode, "bttr"))
            }
        tempDir.deleteRecursively()
    }

    private fun CreateResourcesForBuildingCache(tempDir: File) {
        var chapterPath: File

        if (RequestResourceContainer.mediaTypes.contains(MediaType.WAV)) {
            chapterPath = tempDir.resolve(
                "en/ulb/2pe/$chapterNumber/CONTENTS/wav/chapter"
            ).apply { mkdirs() }
            chapterPath.resolve("en_ulb_2pe_c$chapterNumber.wav").createNewFile()
        }

        if (RequestResourceContainer.mediaTypes.contains(MediaType.MP3)) {
            chapterPath = tempDir.resolve(
                "en/ulb/2pe/$chapterNumber/CONTENTS/mp3/hi/chapter"
            ).apply { mkdirs() }
            chapterPath.resolve("en_ulb_2pe_c$chapterNumber.mp3").createNewFile()
        }
    }

    private fun CreateResourcesForBuildingCache(tempDir: File) {
        var chapterPath: File

        if (RequestResourceContainer.mediaTypes.contains(MediaType.WAV)) {
            chapterPath = tempDir.resolve(
                "en/ulb/2pe/$chapterNumber/CONTENTS/wav/chapter"
            ).apply { mkdirs() }
            chapterPath.resolve("en_ulb_2pe_c$chapterNumber.wav").createNewFile()
        }

        if (RequestResourceContainer.mediaTypes.contains(MediaType.MP3)) {
            chapterPath = tempDir.resolve(
                "en/ulb/2pe/$chapterNumber/CONTENTS/mp3/hi/chapter"
            ).apply { mkdirs() }
            chapterPath.resolve("en_ulb_2pe_c$chapterNumber.mp3").createNewFile()
        }
    }
}
