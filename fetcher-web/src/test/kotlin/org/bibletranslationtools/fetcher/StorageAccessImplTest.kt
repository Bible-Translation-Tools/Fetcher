package org.bibletranslationtools.fetcher

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.net.URL
import org.bibletranslationtools.fetcher.impl.repository.StorageAccessImpl
import org.bibletranslationtools.fetcher.repository.DirectoryProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.slf4j.LoggerFactory
import java.io.FileFilter

class StorageAccessImplTest {

    data class GetLanguageCodesTestCase(
        val mockFileDirs: List<File>,
        val expectedResult: Set<String>
    )

    data class GetPathPrefixDirTestCase(
        val languageCode: String,
        val resourceId: String,
        val fileExtension: String?,
        val expectedResult: String,
        val bookSlug: String? = null,
        val chapter: String? = null
    )

    data class GetContentDirTestCase(
        val prefixDir: File,
        val fileExtension: String,
        val mediaExtension: String,
        val mediaQuality: String,
        val grouping: String,
        val expectedResult: String
    )

    data class HasBookContentTestCase(
        val languageCode: String,
        val resourceId: String,
        val bookSlug: String,
        val fileExtensionList: List<String>,
        val tempFileDir: File,
        val tempFileName: String,
        val expectedResult: Boolean
    )

    private val logger = LoggerFactory.getLogger(javaClass)

    @Test
    fun testHasLanguageContent() {
        val mockDirectoryProvider = mock(DirectoryProvider::class.java)
        val mockFile = mock(File::class.java)
        val nonExistentLanguage = "ru"

        for (testCase in retrieveGetLanguageCodeTestCases()) {
            `when`(mockFile.listFiles(any(FileFilter::class.java)))
                .thenReturn(testCase.mockFileDirs.toTypedArray())
            `when`(mockDirectoryProvider.getContentRoot())
                .thenReturn(mockFile)

            val storageAccessImpl =
                StorageAccessImpl(
                    mockDirectoryProvider
                )

            for (languageDir in testCase.mockFileDirs) {
                assertEquals(true, storageAccessImpl.hasLanguageContent(languageDir.name))
            }

            assertEquals(false, storageAccessImpl.hasLanguageContent(nonExistentLanguage))
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
    fun testGetPathPrefixDir() {
        val mockDirectoryProvider = mock(DirectoryProvider::class.java)
        val testCases = retrieveGetPathPrefixDirTestCases()

        `when`(mockDirectoryProvider.getContentRoot())
            .thenReturn(File("/mock"))

        for (testCase in testCases) {
            assertEquals(
                File("/mock/${testCase.expectedResult}").path,
                StorageAccessImpl.getPathPrefixDir(
                    mockDirectoryProvider.getContentRoot(),
                    testCase.languageCode,
                    testCase.resourceId,
                    testCase.fileExtension,
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

    @Test
    fun testHasBookContent() {
        val testCases = retrieveHasBookContentTestCases()
        val mockDirectoryProvider = mock(DirectoryProvider::class.java)
        val storageAccess = StorageAccessImpl(mockDirectoryProvider)

        val tempRootDir = createTempDir("contentRootFetcherTmp")
        `when`(mockDirectoryProvider.getContentRoot()).thenReturn(tempRootDir)

        for (testCase in testCases) {
            val tempFileDir = tempRootDir.resolve(testCase.tempFileDir).apply { mkdirs() }
            tempFileDir.resolve(testCase.tempFileName).createNewFile()

            assertEquals(testCase.tempFileName,
                storageAccess.hasBookContent(
                    testCase.languageCode,
                    testCase.resourceId,
                    testCase.bookSlug,
                    testCase.fileExtensionList
                ), testCase.expectedResult
            )
            // remove the previously created dir in each loop iteration
            tempFileDir.deleteRecursively()
        }

        tempRootDir.deleteRecursively()
    }

    private fun retrieveHasBookContentTestCases(): List<HasBookContentTestCase> {
        val testCasesResource: URL? = javaClass.classLoader.getResource(
            "StorageAccessImpl_HasBookFileTestCases.json"
        )
        if (testCasesResource == null) {
            logger.error("Storage Access Implementation JSON test file not found.")
            return listOf()
        }

        val testCasesFile = File(testCasesResource.file)
        return jacksonObjectMapper().readValue(testCasesFile.readText())
    }
}
