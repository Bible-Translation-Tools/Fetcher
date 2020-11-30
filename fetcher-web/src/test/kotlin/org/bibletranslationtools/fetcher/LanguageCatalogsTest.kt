package org.bibletranslationtools.fetcher

import org.bibletranslationtools.fetcher.impl.repository.PortGatewayLanguageCatalog
import org.bibletranslationtools.fetcher.impl.repository.UnfoldingWordHeartLanguagesCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

const val GL_COUNT = 38

class LanguageCatalogsTest {

    private val portGatewayLanguageCatalog =
        PortGatewayLanguageCatalog()

    @Test
    fun testGLsCount() {
        val languages = portGatewayLanguageCatalog.getAll()

        assertEquals(GL_COUNT, languages.size)

        for (language in languages) {
            assertTrue(language.code.isNotEmpty())
            assertTrue(language.anglicizedName.isNotEmpty())
            assertTrue(language.localizedName.isNotEmpty())
        }
    }

    @Test
    fun testHLsParse() {
        val hls = UnfoldingWordHeartLanguagesCatalog().getAll()

        assertNotEquals(hls.size, 0)
        hls.forEach {
            assertFalse(it.code.isEmpty())
            assertFalse(it.anglicizedName.isEmpty() && it.localizedName.isEmpty())
        }
    }
}
