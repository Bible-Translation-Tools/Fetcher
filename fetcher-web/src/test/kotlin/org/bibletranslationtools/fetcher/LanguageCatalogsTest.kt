package org.bibletranslationtools.fetcher

import com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable
import org.bibletranslationtools.fetcher.config.EnvironmentConfig
import org.bibletranslationtools.fetcher.impl.repository.LangType
import org.bibletranslationtools.fetcher.impl.repository.PortGatewayLanguageCatalog
import org.bibletranslationtools.fetcher.impl.repository.UnfoldingWordLanguagesCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

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
        val mockConfig = mock(EnvironmentConfig::class.java)
        `when`(mockConfig.CONTENT_ROOT_DIR).thenReturn("unused")
        `when`(mockConfig.CDN_BASE_URL).thenReturn("unused")
        `when`(mockConfig.CDN_BASE_RC_URL).thenReturn("unused")
        `when`(mockConfig.CACHE_REFRESH_MINUTES).thenReturn("60")
        `when`(mockConfig.ORATURE_REPO_DIR).thenReturn("unused")
        `when`(mockConfig.RC_OUTPUT_DIR).thenReturn("unused")
        `when`(mockConfig.LANG_NAMES_URL).thenReturn("https://langnames.bibleineverylanguage.org/langnames.json")

        val hls = UnfoldingWordLanguagesCatalog(mockConfig, LangType.ALL).getAll()

        assertNotEquals(0, hls.size)
        hls.forEach {
            assertFalse(it.code.isEmpty())
            assertFalse(it.anglicizedName.isEmpty() && it.localizedName.isEmpty())
        }
    }
}
