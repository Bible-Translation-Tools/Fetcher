package org.bibletranslationtools.fetcher

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.io.FileFilter
import java.net.URL
import org.bibletranslationtools.fetcher.repository.DirectoryProvider
import org.bibletranslationtools.fetcher.impl.repository.StorageAccessImpl
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.slf4j.LoggerFactory

class StorageAccessImplTest {

    data class GetLanguageCodesTestCase(
        val mockFileDirs: List<File>,
        val expectedResult: Set<String>
    )

    private val logger = LoggerFactory.getLogger(javaClass)

    @Test
    fun testGetLanguageCodes() {
        val mockDirectoryProvider = Mockito.mock(DirectoryProvider::class.java)
        val mockFile = Mockito.mock(File::class.java)

        for (testCase in retrieveGetLanguageCodeTestCases()) {
            `when`(mockFile.listFiles(any(FileFilter::class.java)))
                .thenReturn(testCase.mockFileDirs.toTypedArray())
            `when`(mockDirectoryProvider.getContentRoot())
                .thenReturn(mockFile)

            val storageAccessImpl =
                StorageAccessImpl(
                    mockDirectoryProvider
                )
            assertEquals(
                testCase.expectedResult,
                storageAccessImpl.getLanguageCodes().toSet()
            )
        }
    }

    private fun retrieveGetLanguageCodeTestCases(): List<GetLanguageCodesTestCase> {
        val testCasesResource: URL? = javaClass.classLoader.getResource(
            "StorageAccessImpl_GetLanguageCodes_TestCases.json"
        )
        if (testCasesResource == null) {
            logger.error("Storage Access Implementation JSON test file not found.")
            return listOf()
        }

        val testCasesFile = File(testCasesResource.file)
        return jacksonObjectMapper().readValue(testCasesFile.readText())
    }
}
