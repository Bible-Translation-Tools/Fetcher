package org.bibletranslationtools.fetcher

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import junit.framework.Assert.assertEquals
import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.domain.DirectoryProvider
import org.bibletranslationtools.fetcher.domain.SourceAudioContent
import org.bibletranslationtools.fetcher.repository.LanguageCatalog
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileFilter
import java.net.URL

class SourceAudioContentTest {

    private data class SourceAudioContentTestCase(
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

        for(testCase in getTestCases()) {
            `when`(mockFile.listFiles(any(FileFilter::class.java)))
                .thenReturn(testCase.mockDirs.map { File(it) }.toTypedArray())
            `when`(mockDirectoryProvider.getSourceAudioRoot())
                .thenReturn(mockFile)
            `when`(mockCatalog.getLanguages())
                .thenReturn(testCase.mockCatalogLanguages)

            val sourceAudioContent = SourceAudioContent(mockCatalog, mockDirectoryProvider)

            assertEquals(
                testCase.expectedResult,
                sourceAudioContent.getLanguages()
            )
        }
    }

    private fun getTestCases(): List<SourceAudioContentTestCase> {
        val testCasesResource: URL? = javaClass.classLoader.getResource("SourceAudioContentTestCases.json")
        if(testCasesResource == null) {
            logger.error("Source Audio Content JSON test file not found.")
        }

        val testCasesFile = File(testCasesResource!!.file)

        return jacksonObjectMapper().readValue(testCasesFile.readText())
    }
}