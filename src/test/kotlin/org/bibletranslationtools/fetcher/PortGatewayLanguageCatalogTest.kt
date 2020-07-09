package org.bibletranslationtools.fetcher

import org.bibletranslationtools.fetcher.repository.implementations.PortGatewayLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

const val TOTAL_LANGUAGES = 44

class PortGatewayLanguageCatalogTest {

    private val portGatewayLanguageCatalog = PortGatewayLanguageCatalog()

    @Test
    fun testGetAll() {
        val languages = portGatewayLanguageCatalog.getAll()

        assertEquals(TOTAL_LANGUAGES, languages.size)

        for (language in languages) {
            assertTrue(language.code.isNotEmpty())
            assertTrue(language.anglicizedName.isNotEmpty())
            assertTrue(language.localizedName.isNotEmpty())
        }
    }
}
