package org.bibletranslationtools.fetcher

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.io.FileNotFoundException
import java.net.URL
import org.bibletranslationtools.fetcher.impl.repository.verifyChapterExists
import org.junit.Assert.assertEquals
import org.junit.Test
import org.slf4j.LoggerFactory
import org.wycliffeassociates.rcmediadownloader.data.MediaType

class RCUtilTest {
    private val rcFileName = "titus_test.zip"
    private val testCaseFileName = "VerifyChapterExists_TestCases.json"
    private val logger = LoggerFactory.getLogger(javaClass)

    data class VerifyChapterExistTestCase(
        val bookSlug: String,
        val mediaTypes: List<String>,
        val chapterNumber: Int?,
        val expectedResult: Boolean
    )

    @Test
    fun testVerifyChapterExists() {
        val rcFile = getTestRCFile()
        val testCases = getVerifyChapterExistsTestCase()
        for (case in testCases) {
            val mediaTypes = case.mediaTypes.mapNotNull { MediaType.get(it) }
            val result = verifyChapterExists(
                rcFile,
                case.bookSlug,
                mediaTypes,
                case.chapterNumber
            )
            assertEquals(case.expectedResult, result)
        }
    }

    @Throws(FileNotFoundException::class)
    private fun getTestRCFile(): File {
        val rcFilePath = javaClass.classLoader.getResource(rcFileName)
            ?: throw(FileNotFoundException("Test resource not found: $rcFileName"))
        return File(rcFilePath.file)
    }

    private fun getVerifyChapterExistsTestCase(): List<VerifyChapterExistTestCase> {
        val testCasesResource: URL? =
            javaClass.classLoader.getResource(testCaseFileName)
        if (testCasesResource == null) {
            logger.error("Test file $testCaseFileName not found.")
            return listOf()
        }

        val testCasesFile = File(testCasesResource.file)
        return jacksonObjectMapper().readValue(testCasesFile.readText())
    }
}
