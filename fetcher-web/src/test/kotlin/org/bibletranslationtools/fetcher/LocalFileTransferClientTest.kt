package org.bibletranslationtools.fetcher

import com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable
import org.bibletranslationtools.fetcher.config.EnvironmentConfig
import org.bibletranslationtools.fetcher.io.LocalFileTransferClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wycliffeassociates.rcmediadownloader.io.IDownloadClient

class LocalFileTransferClientTest {
    @Test
    fun testLocalFileDownload() {
        val mockCDN = "https://test_domain.org"
        val relativePath = "en/ulb/tit/1/CONTENTS/mp3/hi/chapter"
        val fileName = "en_ulb_tit_c1.txt"
        val url = "$mockCDN/$relativePath/$fileName"

        val outputDir = createTempDir("fetcher_test")
        val mockContentDir = createTempDir("fetcher_test")
        val srcFile = mockContentDir.resolve(relativePath)
            .apply { mkdirs() }
            .resolve(fileName)
        srcFile.writeText("test content")

        withEnvironmentVariable("CONTENT_ROOT", mockContentDir.path)
            .and("CDN_BASE_URL", "unused")
            .and("CDN_BASE_RC_URL", mockCDN)
            .and("CACHE_REFRESH_MINUTES", "unused")
            .and("ORATURE_REPO_DIR", "unused")
            .and("RC_TEMP_DIR", "unused")
            .and("LANG_NAMES_URL", "unused")
            .execute {
                val downloadClient: IDownloadClient = LocalFileTransferClient(EnvironmentConfig())
                val file = downloadClient.downloadFromUrl(url, outputDir)
                assertNotNull("Transfer unsuccessful from $url", file)
                assertTrue(file!!.exists())
                assertEquals("test content", file.readText())
            }

        mockContentDir.deleteRecursively()
        outputDir.deleteRecursively()
    }
}
