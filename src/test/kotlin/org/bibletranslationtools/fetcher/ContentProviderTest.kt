package org.bibletranslationtools.fetcher

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import junit.framework.Assert.assertEquals
import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.domain.DirectoryProvider
import org.bibletranslationtools.fetcher.domain.ContentProvider
import org.bibletranslationtools.fetcher.repository.LanguageCatalog
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileFilter
import java.net.URL

class ContentProviderTest {

    private data class LanguageTestCase(
        val mockDirs: List<String>,
        val mockCatalogLanguages: List<Language>,
        val expectedResult: List<Language>
    )

    private val logger = LoggerFactory.getLogger(javaClass)

    @Test
    fun testGetLanguages() {
        val mockDirectoryProvider = Mockito.mock(DirectoryProvider::class.java)
        val mockFile = Mockito.mock(File::class.java)
        val mockCatalog = Mockito.mock(LanguageCatalog::class.java)

        for(testCase in getLanguageTestCases()) {
            `when`(mockFile.listFiles(any(FileFilter::class.java)))
                .thenReturn(testCase.mockDirs.map { File(it) }.toTypedArray())
            `when`(mockDirectoryProvider.getContentRoot())
                .thenReturn(mockFile)
            `when`(mockCatalog.getLanguages())
                .thenReturn(testCase.mockCatalogLanguages)

            val contentProvider = ContentProvider(mockDirectoryProvider, mockCatalog)

            assertEquals(
                testCase.expectedResult,
                contentProvider.getLanguages()
            )
        }
    }

    private fun getLanguageTestCases(): List<LanguageTestCase> {
        val testCasesResource: URL? = javaClass.classLoader.getResource("ContentProviderLanguageTestCases.json")
        if(testCasesResource == null) {
            logger.error("Source Content JSON test file not found.")
        }

        val testCasesFile = File(testCasesResource!!.file)

        return jacksonObjectMapper().readValue(testCasesFile.readText())
    }
}