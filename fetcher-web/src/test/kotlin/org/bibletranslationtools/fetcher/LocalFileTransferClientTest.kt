package org.bibletranslationtools.fetcher

import org.bibletranslationtools.fetcher.config.EnvironmentConfig
import org.bibletranslationtools.fetcher.io.LocalFileTransferClient
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.wycliffeassociates.rcmediadownloader.io.IDownloadClient
import kotlin.io.path.createTempDirectory

class LocalFileTransferClientTest {
    @Test
    fun testLocalFileDownload() {
        val mockCDN = "https://test_domain.org"
        val relativePath = "en/ulb/tit/1/CONTENTS/mp3/hi/chapter"
        val fileName = "en_ulb_tit_c1.txt"
        val url = "$mockCDN/$relativePath/$fileName"

        val outputDir = createTempDirectory("fetcher_test").toFile()
        val mockContentDir = createTempDirectory("fetcher_test").toFile()
        val srcFile = mockContentDir.resolve(relativePath)
            .apply { mkdirs() }
            .resolve(fileName)
        srcFile.writeText("test content")

        val mockConfig = mock(EnvironmentConfig::class.java)
        `when`(mockConfig.CONTENT_ROOT_DIR).thenReturn(mockContentDir.path)
        `when`(mockConfig.CDN_BASE_URL).thenReturn("unused")
        `when`(mockConfig.CDN_BASE_RC_URL).thenReturn(mockCDN)
        `when`(mockConfig.CACHE_REFRESH_MINUTES).thenReturn("60")
        `when`(mockConfig.ORATURE_REPO_DIR).thenReturn("unused")
        `when`(mockConfig.RC_OUTPUT_DIR).thenReturn("unused")
        `when`(mockConfig.LANG_NAMES_URL).thenReturn("unused")

        val downloadClient: IDownloadClient = LocalFileTransferClient(mockConfig)
        val file = downloadClient.downloadFromUrl(url, outputDir)
        assertNotNull("Transfer unsuccessful from $url", file)
        assertTrue(file!!.exists())
        assertEquals("test content", file.readText())

        mockContentDir.deleteRecursively()
        outputDir.deleteRecursively()
    }
}
