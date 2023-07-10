package org.bibletranslationtools.fetcher

import com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable
import org.bibletranslationtools.fetcher.config.EnvironmentConfig
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
        withEnvironmentVariable("CONTENT_ROOT", "unused")
            .and("CDN_BASE_URL", "unused")
            .and("CDN_BASE_RC_URL", "unused")
            .and("CACHE_REFRESH_MINUTES", "unused")
            .and("ORATURE_REPO_DIR", "unused")
            .and("RC_TEMP_DIR", "unused")
            .and("LANG_NAMES_URL", "https://langnames-temp.walink.org/langnames.json")
            .execute {
                val hls = UnfoldingWordHeartLanguagesCatalog(EnvironmentConfig()).getAll()

                assertNotEquals(0, hls.size)
                hls.forEach {
                    assertFalse(it.code.isEmpty())
                    assertFalse(it.anglicizedName.isEmpty() && it.localizedName.isEmpty())
                }
            }
    }
}
