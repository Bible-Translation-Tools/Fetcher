package org.bibletranslationtools.fetcher.impl.repository

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.bibletranslationtools.fetcher.config.EnvironmentConfig
import java.io.FileNotFoundException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.repository.LanguageCatalog
import org.slf4j.LoggerFactory

private const val LANGUAGE_CODE_ID = "lc"
private const val ANGLICIZED_NAME_ID = "ang"
private const val LOCALIZED_NAME_ID = "ln"
private const val IS_GATEWAY = "gw"
private const val DIRECTION = "ld"

enum class LangType {
    GL,
    HL,
    ALL
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class BielLanguage(
    @JsonProperty(LANGUAGE_CODE_ID) val code: String,
    @JsonProperty(ANGLICIZED_NAME_ID) val anglicizedName: String,
    @JsonProperty(LOCALIZED_NAME_ID) val localizedName: String,
    @JsonProperty(IS_GATEWAY) val isGateway: Boolean
)

class BielLanguageCatalogSource(
    envConfig: EnvironmentConfig
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val languageCatalogUrl = envConfig.LANG_NAMES_URL
    @Volatile
    private var languages: List<BielLanguage> = fetchLanguages()

    fun refresh() {
        languages = fetchLanguages()
    }

    fun getLanguages(langType: LangType): List<Language> {
        return languages
            .filter {
                when (langType) {
                    LangType.GL -> it.isGateway
                    LangType.HL -> !it.isGateway
                    LangType.ALL -> true
                }
            }
            .map {
                Language(it.code, it.anglicizedName, it.localizedName, isGateway = false)
            }
    }

    @Throws(FileNotFoundException::class)
    private fun fetchLanguages(): List<BielLanguage> {
        val jsonCatalog = getLanguageCatalogContent()
        return jacksonObjectMapper().readValue(jsonCatalog)
    }

    @Throws(FileNotFoundException::class)
    private fun getLanguageCatalogContent(): String {
        var response = ""

        try {
            val conn = (URL(languageCatalogUrl).openConnection() as HttpURLConnection)
            conn.requestMethod = "GET"
            conn.inputStream.reader().use {
                response = it.readText()
            }
            conn.disconnect()
        } catch (ex: IOException) {
            logger.error("An error occurred when requesting language catalog from $languageCatalogUrl", ex)
            throw ex
        }

        return response
    }
}

class BielLanguagesCatalog(
    private val source: BielLanguageCatalogSource,
    private val langType: LangType
) : LanguageCatalog {

    override fun getAll(): List<Language> = source.getLanguages(langType)

    override fun getLanguage(code: String): Language? = getAll().firstOrNull { it.code == code }

    override fun refresh() {
        source.refresh()
    }
}
