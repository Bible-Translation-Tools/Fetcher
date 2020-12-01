package org.bibletranslationtools.fetcher.impl.repository

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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

class UnfoldingWordHeartLanguagesCatalog : LanguageCatalog {

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class UnfoldingWordHeartLanguage(
        @JsonProperty(LANGUAGE_CODE_ID) val code: String,
        @JsonProperty(ANGLICIZED_NAME_ID) val anglicizedName: String,
        @JsonProperty(LOCALIZED_NAME_ID) val localizedName: String,
        @JsonProperty(IS_GATEWAY) val isGateway: Boolean
    )

    private val logger = LoggerFactory.getLogger(javaClass)
    private val languageCatalogUrl = "http://td.unfoldingword.org/exports/langnames.json"
    private val languageList: List<Language> = parseCatalog()

    override fun getAll(): List<Language> = this.languageList

    override fun getLanguage(code: String): Language? = this.languageList.firstOrNull { it.code == code }

    @Throws(FileNotFoundException::class)
    private fun parseCatalog(): List<Language> {
        val jsonCatalog = getLanguageCatalogContent()
        val languages: List<UnfoldingWordHeartLanguage> =
            jacksonObjectMapper().readValue(jsonCatalog)

        return languages
            .filter {
                !it.isGateway
            }
            .map {
                Language(
                    it.code,
                    it.anglicizedName,
                    it.localizedName
                )
            }
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
