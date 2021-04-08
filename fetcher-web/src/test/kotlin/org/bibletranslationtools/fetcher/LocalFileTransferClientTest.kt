package org.bibletranslationtools.fetcher

import com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable
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

        val mockContentDir = createTempDir("fetcher_test")
        val srcFile = mockContentDir.resolve(relativePath)
            .apply { mkdirs() }
            .resolve(fileName)
        srcFile.writeText("test content")

        val outputDir = createTempDir("fetcher_test")

        withEnvironmentVariable("CONTENT_ROOT", mockContentDir.path)
            .and("CDN_BASE_URL", mockCDN)
            .execute {
                val downloadClient: IDownloadClient = LocalFileTransferClient()
                val file = downloadClient.downloadFromUrl(url, outputDir)
                assertNotNull("Transfer unsuccessful from $url", file)
                assertTrue(file!!.exists())
                assertEquals("test content", file.readText())
            }

        mockContentDir.deleteRecursively()
        outputDir.deleteRecursively()
    }
}
