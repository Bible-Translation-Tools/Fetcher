package org.bibletranslationtools.fetcher

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.bibletranslationtools.fetcher.data.Book
import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.impl.repository.ChapterCatalogImpl
import org.junit.Assert.assertEquals
import org.junit.Test
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URL

class ChapterCatalogImplTest {

    data class GetChapterCountTestCase(
        val language: Language,
        val book: Book,
        val expectedResult: Int
    )

    private val logger = LoggerFactory.getLogger(javaClass)

    @Test
    fun testGetChapterCount() {
        val chapterCatalog = ChapterCatalogImpl()

        for (testCase in retrieveGetChapterCountTestCases()) {
            assertEquals(
                testCase.expectedResult,
                chapterCatalog.getChapterCount(testCase.language, testCase.book)
            )
        }

    }

    private fun retrieveGetChapterCountTestCases(): List<GetChapterCountTestCase> {
        val testCasesResource: URL? = javaClass.classLoader.getResource(
            "ChapterCatalogImpl_GetChapterCount_TestCases.json"
        )
        if(testCasesResource == null) {
            logger.error("Chapter Catalog Implementation JSON test file not found.")
            return listOf()
        }

        val testCasesFile = File(testCasesResource.file)
        return jacksonObjectMapper().readValue(testCasesFile.readText())
    }
}
