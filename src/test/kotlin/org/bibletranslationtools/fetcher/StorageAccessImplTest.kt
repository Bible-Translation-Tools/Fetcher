package org.bibletranslationtools.fetcher

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.io.FileFilter
import java.net.URL
import org.bibletranslationtools.fetcher.impl.repository.StorageAccessImpl
import org.bibletranslationtools.fetcher.repository.DirectoryProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.slf4j.LoggerFactory

class StorageAccessImplTest {

    data class GetLanguageCodesTestCase(
        val mockFileDirs: List<File>,
        val expectedResult: Set<String>
    )

    data class GetBookCodesTestCase(
        val languageCode: String,
        val mockDirs: List<File>,
        val expectedResult: Set<String>
    )

    data class GetPathPrefixDirTestCase(
        val languageCode: String,
        val resourceId: String,
        val fileExtension: String,
        val expectedResult: String,
        val bookSlug: String = "",
        val chapter: String = ""
    )

    data class GetContentDirTestCase(
        val prefixDir: File,
        val fileExtension: String,
        val mediaExtension: String,
        val mediaQuality: String,
        val grouping: String,
        val expectedResult: String
    )

    private val logger = LoggerFactory.getLogger(javaClass)

    @Test
    fun testGetLanguageCodes() {
        val mockDirectoryProvider = mock(DirectoryProvider::class.java)
        val mockFile = mock(File::class.java)

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

    @Test
    fun testGetBookCodes() {
        val mockDirectoryProvider = mock(DirectoryProvider::class.java)
        val mockFile = mock(File::class.java)
        val testCases = retrieveGetBookCodesTestCases()
        for (testCase in testCases) {
            `when`(mockFile.listFiles(any(FileFilter::class.java)))
                .thenReturn(testCase.mockDirs.toTypedArray())
            `when`(mockDirectoryProvider.getProjectsDir(testCase.languageCode, "ulb"))
                .thenReturn(mockFile)

            val storageAccessImpl =
                StorageAccessImpl(
                    mockDirectoryProvider
                )
            assertEquals(
                testCase.expectedResult,
                storageAccessImpl.getBookSlugs(testCase.languageCode, "ulb").toSet()
            )
        }
    }

    private fun retrieveGetBookCodesTestCases(): List<GetBookCodesTestCase> {
        val testCasesResource: URL? = javaClass.classLoader.getResource(
            "StorageAccessImpl_GetBookCodes_TestCases.json"
        )
        if (testCasesResource == null) {
            logger.error("Storage Access Implementation JSON test file not found.")
            return listOf()
        }

        val testCasesFile = File(testCasesResource.file)
        return jacksonObjectMapper().readValue(testCasesFile.readText())
    }

    @Test
    fun testGetPathPrefixDir() {
        val mockDirectoryProvider = mock(DirectoryProvider::class.java)
        val testCases = retrieveGetPathPrefixDirTestCases()

        `when`(mockDirectoryProvider.getContentRoot())
            .thenReturn(File("/mock"))

        for (testCase in testCases) {
            assertEquals(
                File("/mock/${testCase.expectedResult}").path,
                StorageAccessImpl.getPathPrefixDir(
                    testCase.languageCode,
                    testCase.resourceId,
                    testCase.fileExtension,
                    mockDirectoryProvider,
                    testCase.bookSlug,
                    testCase.chapter
                ).path
            )
        }
    }

    private fun retrieveGetPathPrefixDirTestCases(): List<GetPathPrefixDirTestCase> {
        val testCasesResource: URL? = javaClass.classLoader.getResource(
            "StorageAccessImpl_GetPathPrefixDir_TestCases.json"
        )
        if (testCasesResource == null) {
            logger.error("Storage Access Implementation JSON test file not found.")
            return listOf()
        }

        val testCasesFile = File(testCasesResource.file)
        return jacksonObjectMapper().readValue(testCasesFile.readText())
    }

    @Test
    fun testGetContentDir() {
        val testCases = retrieveGetContentDirTestCases()

        for (testCase in testCases) {
            assertEquals(
                File(testCase.expectedResult).path,
                StorageAccessImpl.getContentDir(
                    testCase.prefixDir,
                    testCase.fileExtension,
                    testCase.mediaExtension,
                    testCase.mediaQuality,
                    testCase.grouping
                ).path
            )
        }
    }

    private fun retrieveGetContentDirTestCases(): List<GetContentDirTestCase> {
        val testCasesResource: URL? = javaClass.classLoader.getResource(
            "StorageAccessImpl_GetContentDir_TestCases.json"
        )
        if (testCasesResource == null) {
            logger.error("Storage Access Implementation JSON test file not found.")
            return listOf()
        }

        val testCasesFile = File(testCasesResource.file)
        return jacksonObjectMapper().readValue(testCasesFile.readText())
    }
}
