package org.bibletranslationtools.fetcher

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.net.URL
import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.impl.repository.LanguageRepositoryImpl
import org.bibletranslationtools.fetcher.repository.LanguageCatalog
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.slf4j.LoggerFactory

class LanguageRepositoryImplTest {

    data class GetLanguagesTestCase(
        val languageCodes: List<String>,
        val catalogLanguages: List<Language>,
        val expectedResult: List<Language>
    )

    private val logger = LoggerFactory.getLogger(javaClass)

    @Test
    fun testGetLanguages() {
        val mockStorageAccess = Mockito.mock(StorageAccess::class.java)
        val mockLanguageCatalog = Mockito.mock(LanguageCatalog::class.java)

        for (testCase in retrieveGetLanguagesTestCases()) {
            `when`(mockStorageAccess.getLanguageCodes())
                .thenReturn(testCase.languageCodes)
            `when`(mockLanguageCatalog.getAll())
                .thenReturn(testCase.catalogLanguages)

            val languageRepository =
                LanguageRepositoryImpl(
                    mockStorageAccess,
                    mockLanguageCatalog
                )
            assertEquals(
                testCase.expectedResult,
                languageRepository.getLanguages()
            )
        }
    }

    private fun retrieveGetLanguagesTestCases(): List<GetLanguagesTestCase> {
        val testCasesResource: URL? = javaClass.classLoader.getResource(
            "LanguageRepositoryImpl_GetLanguages_TestCases.json"
        )
        if (testCasesResource == null) {
            logger.error("Language Repository Implementation JSON test file not found.")
            return listOf()
        }

        val testCasesFile = File(testCasesResource.file)
        return jacksonObjectMapper().readValue(testCasesFile.readText())
    }
}
