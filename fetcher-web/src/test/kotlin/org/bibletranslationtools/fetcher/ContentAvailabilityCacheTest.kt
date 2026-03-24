package org.bibletranslationtools.fetcher

import org.bibletranslationtools.fetcher.config.EnvironmentConfig
import org.bibletranslationtools.fetcher.data.Chapter
import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.impl.repository.*
import org.bibletranslationtools.fetcher.repository.ChapterCatalog
import org.bibletranslationtools.fetcher.repository.DirectoryProvider
import org.bibletranslationtools.fetcher.repository.LanguageCatalog
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.junit.Assert.*
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.wycliffeassociates.rcmediadownloader.data.MediaType
import java.io.File
import kotlin.io.path.createTempDirectory

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
        val tempDir = createTempDirectory("testDir").toFile()

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

        val mockConfig = mock(EnvironmentConfig::class.java)
        `when`(mockConfig.CONTENT_ROOT_DIR).thenReturn(tempDir.path)
        `when`(mockConfig.CDN_BASE_URL).thenReturn("unused")
        `when`(mockConfig.CDN_BASE_RC_URL).thenReturn("unused")
        `when`(mockConfig.CACHE_REFRESH_MINUTES).thenReturn("60")
        `when`(mockConfig.ORATURE_REPO_DIR).thenReturn("unused")
        `when`(mockConfig.RC_OUTPUT_DIR).thenReturn("unused")
        `when`(mockConfig.LANG_NAMES_URL).thenReturn("unused")

        val cacheBuilder = ContentAvailabilityCacheBuilder(
            mockConfig,
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

//        withEnvironmentVariable("CONTENT_ROOT", tempDir.path)
//            .and("CDN_BASE_URL", "unused")
//            .and("CDN_BASE_RC_URL", "unused")
//            .and("CACHE_REFRESH_MINUTES", "unused")
//            .and("ORATURE_REPO_DIR", "unused")
//            .and("RC_TEMP_DIR", "unused")
//            .and("LANG_NAMES_URL", "unused")
//            .execute {
//
//            }
        tempDir.deleteRecursively()
    }

    private fun CreateResourcesForBuildingCache(tempDir: File) {
        var chapterPath: File

        if (RequestResourceContainerImpl.mediaTypes.contains(MediaType.WAV)) {
            chapterPath = tempDir.resolve(
                "en/ulb/2pe/$chapterNumber/CONTENTS/wav/chapter"
            ).apply { mkdirs() }
            chapterPath.resolve("en_ulb_2pe_c$chapterNumber.wav").createNewFile()
        }

        if (RequestResourceContainerImpl.mediaTypes.contains(MediaType.MP3)) {
            chapterPath = tempDir.resolve(
                "en/ulb/2pe/$chapterNumber/CONTENTS/mp3/hi/chapter"
            ).apply { mkdirs() }
            chapterPath.resolve("en_ulb_2pe_c$chapterNumber.mp3").createNewFile()
        }
    }
}
