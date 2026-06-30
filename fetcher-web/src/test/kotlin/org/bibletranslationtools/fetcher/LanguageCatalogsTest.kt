package org.bibletranslationtools.fetcher

import org.bibletranslationtools.fetcher.config.EnvironmentConfig
import org.bibletranslationtools.fetcher.impl.repository.BielLanguageCatalogSource
import org.bibletranslationtools.fetcher.impl.repository.BielLanguagesCatalog
import org.bibletranslationtools.fetcher.impl.repository.LangType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class LanguageCatalogsTest {

    private val mockConfig = mock(EnvironmentConfig::class.java).apply {
        `when`(CONTENT_ROOT_DIR).thenReturn("unused")
        `when`(CDN_BASE_URL).thenReturn("unused")
        `when`(CDN_BASE_RC_URL).thenReturn("unused")
        `when`(CACHE_REFRESH_MINUTES).thenReturn("60")
        `when`(ORATURE_REPO_DIR).thenReturn("unused")
        `when`(RC_OUTPUT_DIR).thenReturn("unused")
        `when`(LANG_NAMES_URL).thenReturn("https://langnames.bibleineverylanguage.org/langnames.json")
    }

    private val catalogSource = BielLanguageCatalogSource(mockConfig)

    @Test
    fun testGLsParse() {
        val gls = BielLanguagesCatalog(catalogSource, LangType.GL).getAll()

        assertNotEquals(0, gls.size)
        gls.forEach {
            assertFalse(it.code.isEmpty())
            assertFalse(it.anglicizedName.isEmpty() && it.localizedName.isEmpty())
        }
    }

    @Test
    fun testHLsParse() {
        val hls = BielLanguagesCatalog(catalogSource, LangType.HL).getAll()

        assertNotEquals(0, hls.size)
        hls.forEach {
            assertFalse(it.code.isEmpty())
            assertFalse(it.anglicizedName.isEmpty() && it.localizedName.isEmpty())
        }
    }
}
