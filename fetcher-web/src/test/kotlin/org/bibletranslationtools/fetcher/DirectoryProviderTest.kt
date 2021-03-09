package org.bibletranslationtools.fetcher

import org.bibletranslationtools.fetcher.impl.repository.DirectoryProviderImpl
import org.bibletranslationtools.fetcher.repository.DirectoryProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.slf4j.LoggerFactory

class DirectoryProviderTest {
    companion object {
        private val directoryProvider: DirectoryProvider = DirectoryProviderImpl()
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    @Test
    fun testGetContentRoot() {
        try {
            val contentRoot = directoryProvider.getContentRoot()
            assertTrue(contentRoot.exists())
        } catch (ex: Exception) {
            logger.error("Cannot get content root directory. Is the path variable configured properly?")
            throw(ex)
        }
    }

    @Test
    fun testGetRCExportDir() {
        try {
            val rcDir = directoryProvider.getRCExportDir()
            assertTrue(rcDir.exists())
        } catch (ex: Exception) {
            logger.error("Cannot get RC export directory. Is the path variable configured properly?")
            throw(ex)
        }
    }

    @Test
    fun testGetRCRepoDir() {
        try {
            val rcDir = directoryProvider.getRCRepositoriesDir()
            assertTrue(rcDir.exists())
        } catch (ex: Exception) {
            logger.error("Cannot get RC repos directory. Is the path variable configured properly?")
            throw(ex)
        }
    }
}
