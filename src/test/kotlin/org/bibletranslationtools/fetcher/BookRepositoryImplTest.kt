package org.bibletranslationtools.fetcher

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import junit.framework.Assert.assertEquals
import org.bibletranslationtools.fetcher.data.Book
import org.bibletranslationtools.fetcher.impl.repository.BookRepositoryImpl
import org.bibletranslationtools.fetcher.repository.BookCatalog
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.slf4j.LoggerFactory

class BookRepositoryImplTest {
    private data class BookTestCases(
        val languageCode: String,
        val bookCodes: List<String>,
        val bookCatalog: List<Book>,
        val expectedResult: List<Book>
    )

    private val logger = LoggerFactory.getLogger(javaClass)

    @Test
    fun testGetBooks() {
        val testCases = getTestCasesForBooksRetrieval()
        val mockStorageAccess = mock(StorageAccess::class.java)
        val mockBookCatalog = mock(BookCatalog::class.java)

        for (testCase in testCases) {
            `when`(mockStorageAccess.getBookSlugs(testCase.languageCode, "ulb"))
                .thenReturn(testCase.bookCodes)
            `when`(mockBookCatalog.getAll()).thenReturn(testCase.bookCatalog)

            val bookRepo = BookRepositoryImpl(
                storageAccess = mockStorageAccess,
                bookCatalog = mockBookCatalog
            )

            assertEquals(testCase.expectedResult, bookRepo.getBooks(testCase.languageCode, "ulb"))
        }
    }

    private fun getTestCasesForBooksRetrieval(): List<BookTestCases> {
        val testCasesResource = javaClass.classLoader.getResource(
            "BookRepositoryImpl_GetBooks_TestCases.json"
        )
        if (testCasesResource == null) {
            logger.error("Book Repository Implementation JSON test file not found.")
            return listOf()
        }

        val testCasesFile = File(testCasesResource.file)
        return jacksonObjectMapper().readValue(testCasesFile.readText())
    }
}
